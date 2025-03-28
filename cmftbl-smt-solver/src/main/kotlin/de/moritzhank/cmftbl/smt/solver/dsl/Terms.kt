package de.moritzhank.cmftbl.smt.solver.dsl

sealed interface Term<Type> : Evaluable

data class Constant<Type>(val value: Type) : Term<Type>

data class Variable<Type>(val callContext: CallContext<*, Type>) : Term<Type>

/** Create a deep copy of [term]. */
fun <T> copyTerm(term: Term<T>): Term<T> {
  return when (term) {
    is Constant -> Constant(term.value)
    is Variable -> Variable(term.callContext)
  }
}
