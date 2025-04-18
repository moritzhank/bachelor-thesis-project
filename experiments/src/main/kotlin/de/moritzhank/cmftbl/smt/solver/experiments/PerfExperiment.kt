@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.experiments

import de.moritzhank.cmftbl.smt.solver.SmtSolver
import de.moritzhank.cmftbl.smt.solver.misc.MemoryProfiler
import de.moritzhank.cmftbl.smt.solver.misc.getAbsolutePathFromProjectDir
import de.moritzhank.cmftbl.smt.solver.runSmtSolver
import de.moritzhank.cmftbl.smt.solver.scripts.getDateTimeString
import de.moritzhank.cmftbl.smt.solver.smtSolverVersion
import oshi.SystemInfo
import java.io.File
import java.util.*
import kotlin.math.pow
import kotlin.time.Duration

interface PerfExperimentSetup {

  val identifier: Int

  fun specialSolverArgs(solver: SmtSolver): Array<String> {
    return when(solver) {
      SmtSolver.CVC5 -> arrayOf()
      SmtSolver.Z3 -> arrayOf()
      SmtSolver.YICES -> arrayOf()
    }
  }

}

abstract class PerfExperiment<T: PerfExperimentSetup>(val name: String) {

  private val expFolderName = "_experiment${File.separator}${name.replaceFirstChar { it.lowercase() }}"
  /** Returns the experiment path, guaranteed without '\' or '/' at the end. */
  val expFolderPath = getAbsolutePathFromProjectDir(expFolderName)
  abstract val memoryProfilerWorkingCond: (MemoryProfiler) -> Boolean
  protected var useMemoryProfiler = true
  protected var memoryProfilerSampleRateMs = 100
  protected var timeOutInSeconds : Int? = null
  protected var fileName: (T) -> String? = { null }

  abstract fun generateSmtLib(experiment: T, solver: SmtSolver, logic: String): String

  private fun generateDetailsComment(solver: SmtSolver, logic: String, title: String, color: String, label: String): String {
    val sysInfo = SystemInfo()
    val cpu = sysInfo.hardware.processor.processorIdentifier.name.trim()
    val ram = (sysInfo.hardware.memory.physicalMemory.foldRight(0L) { elem, acc -> acc + elem.capacity } * 10.0.pow(-9) / 1.074)
    val ramStr = String.format(Locale.ENGLISH, "%.2f", ram)
    val os = "${sysInfo.operatingSystem.family} ${sysInfo.operatingSystem.versionInfo}"
    val result = StringBuilder()
    val memProfilerState = if (useMemoryProfiler) "on" else "off"
    result.appendLine("# Details for $title")
    result.appendLine("# Date, time: \"${getDateTimeString('.', ':', ", ", false)}\"")
    result.appendLine("# Solver: \"${smtSolverVersion(solver)}\" with logic: \"$logic\"")
    val timeOutStr = timeOutInSeconds?.let { "${it}s" } ?: "none"
    result.appendLine("# Timeout: $timeOutStr")
    result.appendLine("# Memory profiler: $memProfilerState with sample rate: ${memoryProfilerSampleRateMs}ms")
    result.appendLine("# CPU: \"$cpu\"")
    result.appendLine("# RAM: \"$ramStr\"")
    result.appendLine("# OS: \"$os\"")
    result.appendLine("#")
    result.appendLine("# Plotting settings")
    result.appendLine("# Color: \"$color\"")
    result.appendLine("# Label: \"$label\"")
    result.append("#")
    return result.toString()
  }

  /** @return Path of the resulting CSV file */
  fun runExperiment(
    experiments: List<T>,
    solver: SmtSolver,
    logic: String,
    repetitions: Int,
    color: String,
    label: String,
    runID: String,
    resTime: (Array<Long>) -> String,
    resMaxSolverMemUsageGB: (List<Long>) -> String
  ): String {
    // Run experiment
    val results = Array(experiments.size) { Array(repetitions) { -1L } }
    val memoryStats = Array(experiments.size) { Array(repetitions) { Pair(-1.0, -1L) } }
    val runDirectoryPath = getRunDirectoryPath(runID)
    File(runDirectoryPath).mkdirs()
    run experiments@{
      experiments.forEachIndexed { i, setup ->
        val smtLib = generateSmtLib(setup, solver, logic)
        val filePath = "$runDirectoryPath${File.separator}${fileName(setup)}"
        (0 ..< repetitions).forEach { j ->
          val args = mutableListOf(getTimeOutArg(solver, timeOutInSeconds), getStatsArg(solver),
            *setup.specialSolverArgs(solver)).apply { removeIf { it.isEmpty() } }.toTypedArray()
          val result = runSmtSolver(smtLib, solver, filePath, false, timeOutInSeconds, *args) { pid ->
            if (useMemoryProfiler) {
              val memProfiler = MemoryProfiler.start(pid.toInt(), memoryProfilerSampleRateMs)
              if (memoryProfilerWorkingCond(memProfiler)) {
                memoryStats[i][j] = Pair(memProfiler.maxSysMemUsagePercent, memProfiler.maxProcMemUsageBytes)
              }
            }
          }
          // Write log file
          val logFile = File("${filePath}_${solver.solverName}.log")
          logFile.writeText(result)
          // Timeout occurred
          if (didTimeoutOccurInOutput(solver, result)) {
            memoryStats[i][j] = Pair(-1.0, -1L)
            println("${solver.solverName} timed out for $setup.")
            return@experiments
          }
          // Error occurred
          if (extractExitCodeFromOutput(result) != 0) {
            memoryStats[i][j] = Pair(-1.0, -1L)
            println("${solver.solverName} had an error for $setup.")
          } else {
            val resultingTime = extractDurationFromOutput(solver, result).inWholeMilliseconds
            results[i][j] = resultingTime
            println("${solver.solverName} took ${resultingTime}ms for $setup.")
          }
        }
      }
    }

    // Persist results into csv
    val timeCols = (1..repetitions).fold("") { acc, i -> "$acc\"time$i\", " }.dropLast(2)
    val maxSysMemUsagePCols = (1..repetitions).fold("") { acc, i -> "$acc\"maxSysMemUsage%$i\", " }.dropLast(2)
    val maxSolverMemUsageBCols = (1..repetitions).fold("") { acc, i -> "$acc\"maxSolverMemUsageB$i\", " }.dropLast(2)
    val csv = StringBuilder()
    csv.appendLine(generateDetailsComment(solver, logic, "\"$name\"-Benchmark", color, label))
    csv.appendLine("\"identifier\", $timeCols, $maxSysMemUsagePCols, $maxSolverMemUsageBCols, \"resTime\", \"resMaxSolverMemUsageGB\"")
    run timeRows@{
      experiments.forEachIndexed { i, setup ->
        if (results[i].any { it == -1L }) {
          return@timeRows
        }
        val resultTimeCols = (0 ..< repetitions).fold("") { acc, j -> acc + "${results[i][j]}, " }.dropLast(2)
        val resultMaxSysMemUsagePCols = (0 ..< repetitions).fold("") { acc, j ->
          acc + "%.2f, ".format(Locale.ENGLISH, memoryStats[i][j].first)
        }.dropLast(2)
        val resultMaxSolverMemUsageBCols = (0 ..< repetitions).fold("") { acc, j ->
          acc + "${memoryStats[i][j].second}, "
        }.dropLast(2)
        val r1 = resTime(results[i])
        val r2 = resMaxSolverMemUsageGB(memoryStats[i].map { it.second })
        csv.appendLine("${setup.identifier}, $resultTimeCols, $resultMaxSysMemUsagePCols, $resultMaxSolverMemUsageBCols, $r1, $r2")
      }
    }
    val resultCsvFile = File("$runDirectoryPath${File.separator}${solver.solverName}_${getDateTimeString()}.csv")
    resultCsvFile.writeText(csv.toString())
    return resultCsvFile.absolutePath
  }

  fun getRunDirectoryPath(runID: String) = "$expFolderPath${File.separator}$runID"

  private fun getStatsArg(solver: SmtSolver): String {
    return when(solver) {
      SmtSolver.CVC5, SmtSolver.YICES -> "--stats"
      SmtSolver.Z3 -> "-st"
    }
  }

  private fun getTimeOutArg(solver: SmtSolver, timeOutInSeconds: Int?): String {
    if (timeOutInSeconds == null) {
      return ""
    }
    return when(solver) {
      SmtSolver.CVC5 -> "--tlimit=${timeOutInSeconds * 1000}"
      SmtSolver.YICES -> "--timeout=$timeOutInSeconds"
      SmtSolver.Z3 -> "-T:$timeOutInSeconds"
    }
  }

  private fun extractDurationFromOutput(solver: SmtSolver, output: String): Duration {
    return when(solver) {
      SmtSolver.CVC5 -> {
        val prefix = "global::totalTime = "
        Duration.parse(output.lines().first { it.startsWith(prefix) }.drop(prefix.length))
      }
      SmtSolver.Z3 -> {
        val prefix = " :total-time"
        Duration.parse(output.lines().first { it.startsWith(prefix) }.drop(prefix.length).dropLast(1).replace(" ", "") + "s")
      }
      SmtSolver.YICES -> {
        val prefix = " :total-run-time "
        Duration.parse(output.lines().first { it.startsWith(prefix) }.drop(prefix.length) + "s")
      }
    }
  }

  private fun didTimeoutOccurInOutput(solver: SmtSolver, output: String): Boolean {
    return when(solver) {
      SmtSolver.CVC5 -> {
        output.lines().getOrNull(2)?.endsWith("timeout.", true) == true
      }
      SmtSolver.Z3 -> {
        output.lines().getOrNull(2)?.startsWith("timeout", true) == true
      }
      SmtSolver.YICES -> {
        output.lines().getOrNull(1)?.isNotEmpty() == true
      }
    }
  }

  private fun extractExitCodeFromOutput(output: String): Int? {
    return output.lines().getOrNull(0)?.removePrefix("Exited with ")?.removeSuffix(".")?.toIntOrNull()
  }
}
