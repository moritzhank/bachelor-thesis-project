@file:Suppress("UndocumentedPublicProperty", "ClassOrdering", "ExpressionBodySyntax")

package de.moritzhank.cmftbl.smt.solver

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import de.moritzhank.cmftbl.smt.solver.misc.getAbsolutePathFromProjectDir

/** Contains all relevant settings for local SMT-solver instances. */
@Serializable
data class SmtSolverSettings(
  var pathToCVC5Bin: String,
  var pathToz3Bin: String,
  var pathToYicesBin: String,
  var pathToMathSATBin: String
) {

  companion object {

    private val settingsFilePath = getAbsolutePathFromProjectDir("smtSolverSettings.json")

    /** Loads the [SmtSolverSettings] from "smtSolverSettings.json". */
    fun load(): SmtSolverSettings? {
      val settingsFile = File(settingsFilePath)
      if (!settingsFile.exists()) {
        return null
      }
      return Json.decodeFromString(settingsFile.readText())
    }

    /** Generate empty "smtSolverSettings.json". */
    fun generateTemplate() {
      File(settingsFilePath).writeText(Json.encodeToString(SmtSolverSettings("", "", "", "")))
    }
  }

  /** Get the path to the local binary based on [SmtSolver]. */
  fun getPathToSolverBin(smtSolver: SmtSolver): String {
    return when (smtSolver) {
      SmtSolver.CVC5 -> pathToCVC5Bin
      SmtSolver.Z3 -> pathToz3Bin
      SmtSolver.YICES -> pathToYicesBin
      SmtSolver.MATHSAT -> pathToMathSATBin
    }
  }
}

fun requireSolverBinPath(solver: SmtSolver): String {
  val settings = SmtSolverSettings.load()
  requireNotNull(settings) {
    SmtSolverSettings.generateTemplate()
    "The file smtSolverSettings.json must be specified."
  }
  val solverBinPath = settings.getPathToSolverBin(solver)
  require(File(solverBinPath).exists()) {
    "The specified binary (at \"$solverBinPath\") for ${solver.solverName} does not exist."
  }
  return solverBinPath
}
