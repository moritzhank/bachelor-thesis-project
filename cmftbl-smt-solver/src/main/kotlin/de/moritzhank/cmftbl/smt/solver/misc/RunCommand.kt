@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.misc

import java.io.File
import java.util.concurrent.TimeUnit

// Copied and modified from https://stackoverflow.com/a/41495542
fun String.runCommand(workingDir: File, timeOutInMS: Long = 60 * 60 * 1000): String? {
  try {
    val parts = this.split("\\s".toRegex())
    val proc =
        ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
    proc.waitFor(timeOutInMS, TimeUnit.MILLISECONDS)
    if (proc.exitValue() != 0)
        error(
            "Error executing a command:${System.lineSeparator()}${proc.errorStream.bufferedReader().readText()}")
    return proc.inputReader().readText()
  } catch (e: Exception) {
    e.printStackTrace()
    return null
  }
}
