package de.moritzhank.cmftbl.smt.solver.translation.data

/** Represents Smt-primitives and their name in SmtLib. */
enum class SmtPrimitive(val smtPrimitiveSortName: String, val defaultValue: Any) {

  BOOL("Bool", false),
  INT("Int", Int.MIN_VALUE + 1),
  REAL("Real", Double.MIN_VALUE + 1.0),
  STRING("String", "")
}

/** Get [SmtPrimitive] for a [Class] (based on [Class.getSimpleName]). */
fun Class<*>.smtPrimitive(): SmtPrimitive? =
    when (this.simpleName) {
      "boolean" -> SmtPrimitive.BOOL
      "int" -> SmtPrimitive.INT
      "float" -> SmtPrimitive.REAL
      "double" -> SmtPrimitive.REAL
      "String" -> SmtPrimitive.STRING
      else -> null
    }

/** Get [SmtPrimitive] for an object. */
fun Any.smtPrimitive(): SmtPrimitive? =
    when (this) {
      is Boolean -> SmtPrimitive.BOOL
      is Int -> SmtPrimitive.INT
      is Float -> SmtPrimitive.REAL
      is Double -> SmtPrimitive.REAL
      is String -> SmtPrimitive.STRING
      else -> null
    }
