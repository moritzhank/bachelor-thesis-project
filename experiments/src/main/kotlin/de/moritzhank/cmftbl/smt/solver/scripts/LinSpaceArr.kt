@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.scripts

fun linSpaceArr(start: Int, stop: Int, num: Int): List<Int> {
  val args = arrayOf(start.toString(), stop.toString(), num.toString())
  val proc = PythonCommandLineWrapper.runScript("linSpaceArr.py", *args)
  if (proc.exitValue() != 0) {
    throw RuntimeException(proc.inputReader().readText() + proc.errorReader().readText())
  }
  val rawResult = proc.inputReader().readText()
  return rawResult.split(",").fold(listOf()) { list, str -> list.plus(str.toInt()) }
}
