@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import de.moritzhank.cmftbl.smt.solver.misc.getAbsolutePathFromProjectDir
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

/** Captures all supported SMT-Solver. */
enum class SmtSolver(val solverName: String) {
  CVC5("cvc5"),
  Z3("z3"),
  YICES("yices")
}

/**
 * Save the SMT-program [program] in a file.
 * @return Path of the saved program.
 */
fun saveSmtFile(program: String, solver: SmtSolver = SmtSolver.CVC5, path: String? = null): String {
  val directory = path?.let { File(path).parent } ?: getAbsolutePathFromProjectDir("_smtTmp")
  val smt2FilePath = path ?: "$directory${File.separator}${UUID.randomUUID()}.smt2"
  File(directory).mkdirs()
  File(smt2FilePath).apply { writeText(program) }
  return smt2FilePath
}

/**
 * Run a local SMT-Solver instance. This requires a correct setup of "smtSolverSettings.json".
 * @return The first line contains the exit code. The second line contains information about a possible manual
 * interruption related to the timeout, followed by the output and possible errors.
 */
@OptIn(DelicateCoroutinesApi::class)
fun runSmtSolver(
  program: String,
  solver: SmtSolver = SmtSolver.CVC5,
  filePath: String?,
  removeSmt2File: Boolean,
  manualTimeoutInSeconds: Int?,
  vararg solverArgs: String,
  memoryProfilerCallback: ((Long) -> Unit)?,
): String {
  val solverBinPath = requireSolverBinPath(solver)
  val smt2FilePath = saveSmtFile(program, solver, filePath)
  val smt2File = File(smt2FilePath)
  val proc = ProcessBuilder(solverBinPath, smt2FilePath, *solverArgs).start()
  // MemoryProfiler should run async
  GlobalScope.launch {
    memoryProfilerCallback?.invoke(proc.pid())
  }
  var timeoutOccurred = false
  // Handle timeout for Yices2
  if (solver == SmtSolver.YICES && manualTimeoutInSeconds != null) {
    timeoutOccurred = !proc.waitFor(manualTimeoutInSeconds.toLong(), TimeUnit.SECONDS)
    if (proc.isAlive) {
      proc.destroyForcibly().waitFor()
    }
  } else {
    proc.waitFor()
  }
  val exitCode = proc.exitValue()
  // Has run into timeout
  if (timeoutOccurred) {
    if (removeSmt2File) {
      smt2File.delete()
    }
    return "Exited with $exitCode.\nManual interruption of process due to timeout."
  }
  val result = "Exited with $exitCode.\n\n" + proc.inputReader().readText() + proc.errorReader().readText()
  if (removeSmt2File) {
    smt2File.delete()
  }
  return result
}

/**
 * Run a local SMT-Solver instance with the option to show the version. This requires a correct
 * setup of "smtSolverSettings.json".
 */
fun smtSolverVersion(solver: SmtSolver): String {
  val solverBinPath = requireSolverBinPath(solver)
  val versionOption =
      when (solver) {
        SmtSolver.CVC5 -> "--version"
        SmtSolver.Z3 -> "--version"
        SmtSolver.YICES -> "--version"
      }
  val proc = ProcessBuilder(solverBinPath, versionOption).start().apply { waitFor() }
  val result = proc.inputReader().readText() + proc.errorReader().readText()
  return when (solver) {
    SmtSolver.CVC5 -> {
      result.lines().first().removePrefix("This is ").dropLastWhile { it != '[' }.dropLast(2).removePrefix("cvc5 version ")
    }
    SmtSolver.Z3 -> result.dropLastWhile { it != '-' }.dropLast(2).removePrefix("Z3 version ")
    SmtSolver.YICES -> result.lines().first().removePrefix("Yices ")
  }
}
