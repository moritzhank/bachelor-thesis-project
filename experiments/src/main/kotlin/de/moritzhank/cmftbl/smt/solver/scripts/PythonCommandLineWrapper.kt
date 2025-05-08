@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.scripts

import java.io.IOException
import kotlin.io.path.absolutePathString
import de.moritzhank.cmftbl.smt.solver.ExperimentLoader

internal class PythonCommandLineWrapper {

  companion object {

    private var pythonBaseCmdCache: String? = null

    private fun pythonInstalled(name: String): Boolean =
        try {
          ProcessBuilder(name, "--version").start().apply { waitFor() }.exitValue() == 0
        } catch (err: IOException) {
          false
        }

    fun pythonBaseCmd(): String {
      val pythonBaseCmdCache = this.pythonBaseCmdCache
      if (pythonBaseCmdCache != null) {
        return pythonBaseCmdCache
      }
      val pythonBaseCmd =
          if (pythonInstalled("python")) {
            "python"
          } else if (pythonInstalled("python3")) {
            "python3"
          } else {
            error("No python installation could be found.")
          }
      this.pythonBaseCmdCache = pythonBaseCmd
      return pythonBaseCmd
    }

    fun runScript(name: String, vararg args: String): Process {
      val scriptPath = ExperimentLoader.getPathToResource("/scripts/$name").absolutePathString()
      println("Running \"${pythonBaseCmd()} $scriptPath ${args.joinToString(" ")}\"")
      return ProcessBuilder(pythonBaseCmd(), scriptPath, *args).start().apply { waitFor() }
    }
  }
}
