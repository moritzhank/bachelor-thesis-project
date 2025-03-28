@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.scripts

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun getDateTimeString(
    dSep: Char = '_',
    tSep: Char = '_',
    sep: String = "_",
    millisOfDays: Boolean = true
): String {
  val timeString = if (millisOfDays) "AAAA" else "HH${tSep}mm${tSep}ss"
  return LocalDateTime.now()
      .format(DateTimeFormatter.ofPattern("yyyy${dSep}MM${dSep}dd${sep}$timeString"))
}
