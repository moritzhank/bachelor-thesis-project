package de.moritzhank.cmftbl.smt.solver.misc

/** Replaces first character with a lower-case one. */
fun String.firstCharLower(): String = this.replaceFirstChar { it.lowercaseChar() }

/** Converts given primitive to SmtLib format. */
fun Any.toSmtLibPrimitiveFormat(smtLibTermForNegativeNumbers: (Number) -> String): String {
  return when (this) {
    is String -> "\"$this\""
    is Number ->
        if (!this.isNegative()) this._toSmtLibPrimitiveFormat()
        else smtLibTermForNegativeNumbers(this)
    else -> this.toString()
  }
}

/** Converts number to SmtLib format. */
fun Number._toSmtLibPrimitiveFormat(): String {
  return when (this) {
    is Int -> this.toString()
    is Long -> this.toString()
    is Float -> this.toBigDecimal().toPlainString()
    is Double -> this.toBigDecimal().toPlainString()
    else -> this.toString()
  }
}

/** Generate ITE-structure for SMT-LIB. */
fun <T> generateEqualsITEStructure(
    elements: Collection<T>,
    comparisonVarName: String,
    ifStr: (T) -> String,
    thenStr: (T) -> String,
    defaultValue: String? = null
): String {
  val iteStructureFront = StringBuilder("")
  var bracketsNeeded = 0
  val firstElem = elements.first()
  elements.forEachIndexed { index, elem ->
    // Skip first element if no default is given
    val skip = defaultValue == null && index == 0
    if (!skip) {
      iteStructureFront.append("(ite (= $comparisonVarName ${ifStr(elem)}) ${thenStr(elem)} ")
      bracketsNeeded++
    }
  }
  if (defaultValue == null) {
    iteStructureFront.append("${thenStr(firstElem)}${")".repeat(bracketsNeeded)}")
  } else {
    iteStructureFront.append("$defaultValue${")".repeat(bracketsNeeded)}")
  }
  return iteStructureFront.toString()
}

/** Negate a number. */
fun Number.negate(): Number {
  require(this != Int.MIN_VALUE) { "Int.MIN_VALUE cannot be negated." }
  return when (this) {
    is Int -> -this
    is Long -> -this
    is Float -> -this
    is Double -> -this
    else -> throw IllegalArgumentException("Unsupported number type: ${this::class.simpleName}")
  }
}

/** Is number smaller than zero? */
fun Number.isNegative(): Boolean {
  return when (this) {
    is Int -> this < 0
    is Long -> this < 0
    is Float -> this < 0
    is Double -> this < 0
    else -> throw IllegalArgumentException("Unsupported number type: ${this::class.simpleName}")
  }
}

/** Converts integer interval to double interval. */
fun Pair<Int, Int>?.convert() : Pair<Double, Double> {
  if (this == null) {
    return Pair(0.0, Double.POSITIVE_INFINITY)
  }
  return Pair(first.toDouble(), second.toDouble())
}

/** Mirror interval. Note: Double.NEGATIVE_INFINITY or Double.POSITIVE_INFINITY = -∞ or ∞. */
fun Pair<Double, Double>.mirror() : Pair<Double, Double> {
  val minus = { n: Double -> if (n == 0.0) 0.0 else -n }
  val newFirst = if (second == Double.POSITIVE_INFINITY) Double.NEGATIVE_INFINITY else minus(second)
  val newSecond = if (first == Double.NEGATIVE_INFINITY) Double.POSITIVE_INFINITY else minus(first)
  return Pair(newFirst, newSecond)
}

/** Check if interval is mirrored. */
fun Pair<Double, Double>.isMirrored() : Boolean {
  return this.first < 0.0
}

/** Double interval to String. */
fun Pair<Double, Double>.str(): String {
  val left = if (first == Double.NEGATIVE_INFINITY) "(-∞" else "[$first"
  val right = if (second == Double.POSITIVE_INFINITY) "∞)" else "$second]"
  return "$left,$right"
}
