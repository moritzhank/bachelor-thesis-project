package de.moritzhank.cmftbl.smt.solver.misc

import java.io.File

class Logger private constructor(
  private val path: String
) {

  companion object {
    fun new(path: String) = Logger(path)
  }

  private fun prefix() = "[${getDateTimeString('.', ':', " ", false)}] "

  fun log(message: String) {
    val message = prefix() + message + System.lineSeparator()
    try {
      File(path).appendText(message)
    } catch (exception: Exception ) {
      println("An error occurred during logging into file $path:")
      exception.printStackTrace()
    }
    print(message)
  }

}