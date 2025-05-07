@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.experiments

import de.moritzhank.cmftbl.smt.solver.SmtSolver
import de.moritzhank.cmftbl.smt.solver.misc.Logger
import de.moritzhank.cmftbl.smt.solver.misc.MemoryProfiler
import de.moritzhank.cmftbl.smt.solver.misc.getAbsolutePathFromProjectDir
import de.moritzhank.cmftbl.smt.solver.misc.getDateTimeString
import de.moritzhank.cmftbl.smt.solver.runSmtSolver
import de.moritzhank.cmftbl.smt.solver.smtSolverVersion
import oshi.SystemInfo
import java.io.File
import java.util.*
import kotlin.collections.any
import kotlin.collections.map
import kotlin.math.pow
import kotlin.time.Duration

interface PerfExperimentSetup {

  val identifier: Int

  fun specialSolverArgs(solver: SmtSolver): Array<String> {
    return when(solver) {
      SmtSolver.CVC5 -> arrayOf()
      SmtSolver.Z3 -> arrayOf()
      SmtSolver.YICES -> arrayOf()
      SmtSolver.MATHSAT -> arrayOf()
    }
  }

}

abstract class PerfExperiment<T: PerfExperimentSetup>(val name: String) {

  private val expFolderName = "_experiment${File.separator}${name.replaceFirstChar { it.lowercase() }}"
  /** Returns the experiment path, guaranteed without '\' or '/' at the end. */
  val expFolderPath = getAbsolutePathFromProjectDir(expFolderName)
  var logger: Logger? = null
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
    val runDirectoryPath = getRunDirectoryPath(runID, solver)
    File(runDirectoryPath).mkdirs()
    val resultCSVPath = "$runDirectoryPath${File.separator}results.csv"
    generateResultCSV(repetitions, solver, logic, color, label, resultCSVPath)
    run experiments@{
      experiments.forEachIndexed { i, setup ->
        val smtLib = generateSmtLib(setup, solver, logic)
        val filePath = "$runDirectoryPath${File.separator}${fileName(setup)}"
        (0 ..< repetitions).forEach { j ->
          val args = mutableListOf(getTimeOutArg(solver, timeOutInSeconds), getStatsArg(solver),
            *setup.specialSolverArgs(solver)).apply { removeIf { it.isEmpty() } }.toTypedArray()
          val result = runSmtSolver(smtLib, solver, filePath, false, timeOutInSeconds, *args, logger = logger) { pid ->
            if (useMemoryProfiler) {
              val memProfiler = MemoryProfiler.start(pid.toInt(), memoryProfilerSampleRateMs)
              if (memoryProfilerWorkingCond(memProfiler)) {
                memoryStats[i][j] = Pair(memProfiler.maxSysMemUsagePercent, memProfiler.maxProcMemUsageBytes)
              }
            }
          }
          // Timeout occurred
          val currentMemoryStats = memoryStats[i][j]
          if (didTimeoutOccurInOutput(solver, result)) {
            logger?.log("${solver.solverName} timed out for $setup $currentMemoryStats.")
            memoryStats[i][j] = Pair(-1.0, -1L)
            return@experiments
          }
          // Error occurred
          if (extractExitCodeFromOutput(result) != 0) {
            logger?.log("${solver.solverName} had an error for $setup $currentMemoryStats.")
            memoryStats[i][j] = Pair(-1.0, -1L)
          } else {
            val resultingTime = extractDurationFromOutput(solver, result).inWholeMilliseconds
            results[i][j] = resultingTime
            logger?.log("${solver.solverName} took ${resultingTime}ms for $setup.")
          }
        }
        if (results[i].all { it != -1L }) {
          addRowToCSV(repetitions, results[i], memoryStats[i], resTime, resMaxSolverMemUsageGB, setup, resultCSVPath)
        }
      }
    }
    return resultCSVPath
  }

  fun generateResultCSV(
    repetitions: Int,
    solver: SmtSolver,
    logic: String,
    color: String,
    label: String,
    path: String
  ) {
    try {
      val timeCols = (1..repetitions).fold("") { acc, i -> "$acc\"time$i\", " }.dropLast(2)
      val maxSysMemUsagePCols = (1..repetitions).fold("") { acc, i -> "$acc\"maxSysMemUsage%$i\", " }.dropLast(2)
      val maxSolverMemUsageBCols = (1..repetitions).fold("") { acc, i -> "$acc\"maxSolverMemUsageB$i\", " }.dropLast(2)
      val csv = StringBuilder()
      csv.appendLine(generateDetailsComment(solver, logic, "\"$name\"-Benchmark", color, label))
      csv.appendLine("\"identifier\", $timeCols, $maxSysMemUsagePCols, $maxSolverMemUsageBCols, \"resTime\", \"resMaxSolverMemUsageGB\"")
      val resultCSVFile = File(path)
      resultCSVFile.writeText(csv.toString())
    } catch (e: Exception) {
      logger?.log("An error occurred during the initialization of $path.")
      e.printStackTrace()
    }
  }

  fun addRowToCSV(
    repetitions: Int,
    times: Array<Long>,
    memoryStats: Array<Pair<Double, Long>>,
    resTime: (Array<Long>) -> String,
    resMaxSolverMemUsageGB: (List<Long>) -> String,
    setup: T,
    path: String
  ) {
    try {
      val resultTimeCols = (0 ..< repetitions).fold("") { acc, j -> acc + "${times[j]}, " }.dropLast(2)
      val resultMaxSysMemUsagePCols = (0 ..< repetitions).fold("") { acc, j ->
        acc + "%.2f, ".format(Locale.ENGLISH, memoryStats[j].first)
      }.dropLast(2)
      val resultMaxSolverMemUsageBCols = (0 ..< repetitions).fold("") { acc, j ->
        acc + "${memoryStats[j].second}, "
      }.dropLast(2)
      val r1 = resTime(times)
      val r2 = resMaxSolverMemUsageGB(memoryStats.map { it.second })
      val row = "${setup.identifier}, $resultTimeCols, $resultMaxSysMemUsagePCols, $resultMaxSolverMemUsageBCols, $r1, $r2"
      val resultCSVFile = File(path)
      resultCSVFile.appendText("row${System.lineSeparator()}")
    } catch (e: Exception) {
      logger?.log("An error occurred during addition of a row to $path.")
      e.printStackTrace()
    }
  }

  fun getRunDirectoryPath(runID: String, solver: SmtSolver? = null) =
    "$expFolderPath${File.separator}$runID" + (solver?.let { "${File.separator}${it.solverName}" } ?: "")

  private fun getStatsArg(solver: SmtSolver): String {
    return when(solver) {
      SmtSolver.CVC5, SmtSolver.YICES -> "--stats"
      SmtSolver.Z3 -> "-st"
      SmtSolver.MATHSAT -> "-stats"
    }
  }

  private fun getTimeOutArg(solver: SmtSolver, timeOutInSeconds: Int?): String {
    if (timeOutInSeconds == null) {
      return ""
    }
    return when(solver) {
      SmtSolver.CVC5 -> "--tlimit=${timeOutInSeconds * 1000}"
      SmtSolver.YICES -> ""
      SmtSolver.Z3 -> "-T:$timeOutInSeconds"
      SmtSolver.MATHSAT -> ""
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
      SmtSolver.MATHSAT -> {
        val prefix = " :time-seconds "
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
      SmtSolver.YICES, SmtSolver.MATHSAT -> {
        output.lines().getOrNull(1)?.isNotEmpty() == true
      }
    }
  }

  private fun extractExitCodeFromOutput(output: String): Int? {
    return output.lines().getOrNull(0)?.removePrefix("Exited with ")?.removeSuffix(".")?.toIntOrNull()
  }
}
