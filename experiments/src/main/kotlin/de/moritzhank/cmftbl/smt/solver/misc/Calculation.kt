@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.misc

/** Returns [invalid] or the average if there is no [invalid] present. */
fun Collection<Long>.avgWithoutInvalids(invalid: Long = -1L): Long {
  var numberInvalid = this.size
  var acc = 0L
  this.forEach {
    if (it != invalid) {
      acc += it
      numberInvalid--
    }
  }
  return if (numberInvalid > 0) {
    invalid
  } else {
    ((1.0 * acc) / this.size).toLong()
  }
}

/** Returns [invalid] or the average if there is no [invalid] present. */
fun Collection<Double>.avgWithoutInvalids(invalid: Double = -1.0): Double {
  var numberInvalid = this.size
  var acc = 0.0
  this.forEach {
    if (it != invalid) {
      acc += it
      numberInvalid--
    }
  }
  return if (numberInvalid > 0) {
    invalid
  } else {
    (1.0 * acc) / this.size
  }
}