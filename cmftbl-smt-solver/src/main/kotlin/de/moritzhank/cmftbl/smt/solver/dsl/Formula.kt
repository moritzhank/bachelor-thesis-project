package de.moritzhank.cmftbl.smt.solver.dsl

import tools.aqua.stars.core.types.EntityType

sealed interface Formula : Evaluable

sealed interface LogicalConnectiveFormula : Formula

data object TT : Formula

data object FF : Formula

data class Neg(val inner: Formula) : LogicalConnectiveFormula

data class And(val lhs: Formula, val rhs: Formula) : LogicalConnectiveFormula

data class Or(val lhs: Formula, val rhs: Formula) : LogicalConnectiveFormula

data class Implication(val lhs: Formula, val rhs: Formula) : LogicalConnectiveFormula

data class Iff(val lhs: Formula, val rhs: Formula) : LogicalConnectiveFormula

data class Prev(val interval: Pair<Int, Int>? = null, val inner: Formula) : Formula

data class Next(val interval: Pair<Int, Int>? = null, val inner: Formula) : Formula

data class Once(val interval: Pair<Int, Int>? = null, val inner: Formula) : Formula

data class Historically(val interval: Pair<Int, Int>? = null, val inner: Formula) : Formula

data class Eventually(val interval: Pair<Int, Int>? = null, val inner: Formula) : Formula

data class Always(val interval: Pair<Int, Int>? = null, val inner: Formula) : Formula

data class Since(val interval: Pair<Int, Int>? = null, val lhs: Formula, val rhs: Formula) :
    Formula

data class Until(val interval: Pair<Int, Int>? = null, val lhs: Formula, val rhs: Formula) :
    Formula

data class Forall<E : EntityType<*, *, *, *, *>>(val ccb: CallContextBase<E>, val inner: Formula) :
    Formula

data class Exists<E : EntityType<*, *, *, *, *>>(val ccb: CallContextBase<E>, val inner: Formula) :
    Formula

data class Binding<Type: Any>(
    val ccb: CallContextBase<Type>,
    val bindTerm: Term<Type>,
    val inner: Formula
) : Formula {

  fun copy(callbackForInner: (Formula) -> Formula = { f -> f }): Binding<Type> =
      Binding(ccb, copyTerm(bindTerm), callbackForInner(inner))
}

data class MinPrevalence(
    val interval: Pair<Int, Int>? = null,
    val fraction: Double,
    val inner: Formula
) : Formula

data class PastMinPrevalence(
    val interval: Pair<Int, Int>? = null,
    val fraction: Double,
    val inner: Formula
) : Formula

data class MaxPrevalence(
    val interval: Pair<Int, Int>? = null,
    val fraction: Double,
    val inner: Formula
) : Formula

data class PastMaxPrevalence(
    val interval: Pair<Int, Int>? = null,
    val fraction: Double,
    val inner: Formula
) : Formula

/** Create a deep copy of [formula]. */
fun copyFormula(formula: Formula): Formula {
  return when (formula) {
    is TT -> formula
    is FF -> formula
    is Neg -> Neg(copyFormula(formula.inner))
    is And -> And(copyFormula(formula.lhs), copyFormula(formula.rhs))
    is Or -> Or(copyFormula(formula.lhs), copyFormula(formula.rhs))
    is Implication -> Implication(copyFormula(formula.lhs), copyFormula(formula.rhs))
    is Iff -> Iff(copyFormula(formula.lhs), copyFormula(formula.rhs))
    is Prev -> Prev(formula.interval?.copy(), copyFormula(formula.inner))
    is Next -> Next(formula.interval?.copy(), copyFormula(formula.inner))
    is Once -> Once(formula.interval?.copy(), copyFormula(formula.inner))
    is Historically -> Historically(formula.interval?.copy(), copyFormula(formula.inner))
    is Eventually -> Eventually(formula.interval?.copy(), copyFormula(formula.inner))
    is Always -> Always(formula.interval?.copy(), copyFormula(formula.inner))
    is Since -> Since(formula.interval?.copy(), copyFormula(formula.lhs), copyFormula(formula.rhs))
    is Until -> Until(formula.interval?.copy(), copyFormula(formula.lhs), copyFormula(formula.rhs))
    is Forall<*> -> Forall(formula.ccb, copyFormula(formula.inner))
    is Exists<*> -> Exists(formula.ccb, copyFormula(formula.inner))
    is Binding<*> -> formula.copy()
    is MinPrevalence ->
        MinPrevalence(formula.interval?.copy(), formula.fraction, copyFormula(formula.inner))
    is PastMinPrevalence ->
        PastMinPrevalence(formula.interval?.copy(), formula.fraction, copyFormula(formula.inner))
    is MaxPrevalence ->
        MaxPrevalence(formula.interval?.copy(), formula.fraction, copyFormula(formula.inner))
    is PastMaxPrevalence ->
        PastMaxPrevalence(formula.interval?.copy(), formula.fraction, copyFormula(formula.inner))
    is Leq<*> -> formula.copy()
    is Geq<*> -> formula.copy()
    is Lt<*> -> formula.copy()
    is Gt<*> -> formula.copy()
    is Eq<*> -> formula.copy()
    is Ne<*> -> formula.copy()
  }
}

/** Get a list of all direct child nodes of a formula. */
fun Formula.childNodes(): List<Evaluable> {
  return when (this) {
    is Always -> listOf(inner)
    is And -> listOf(lhs, rhs)
    is Binding<*> -> listOf(bindTerm, inner)
    is Eq<*> -> listOf(lhs, rhs)
    is Geq<*> -> listOf(lhs, rhs)
    is Gt<*> -> listOf(lhs, rhs)
    is Leq<*> -> listOf(lhs, rhs)
    is Lt<*> -> listOf(lhs, rhs)
    is Ne<*> -> listOf(lhs, rhs)
    is Eventually -> listOf(inner)
    is Exists<*> -> listOf(inner)
    FF -> listOf()
    is Forall<*> -> listOf(inner)
    is Historically -> listOf(inner)
    is Iff -> listOf(lhs, rhs)
    is Implication -> listOf(lhs, rhs)
    is MaxPrevalence -> listOf(inner)
    is MinPrevalence -> listOf(inner)
    is Neg -> listOf(inner)
    is Next -> listOf(inner)
    is Once -> listOf(inner)
    is Or -> listOf(lhs, rhs)
    is PastMaxPrevalence -> listOf(inner)
    is PastMinPrevalence -> listOf(inner)
    is Prev -> listOf(inner)
    is Since -> listOf(lhs, rhs)
    TT -> listOf()
    is Until -> listOf(lhs, rhs)
  }
}