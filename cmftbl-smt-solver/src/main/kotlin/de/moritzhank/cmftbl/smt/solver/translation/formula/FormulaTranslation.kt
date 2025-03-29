package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.CallContextBase
import de.moritzhank.cmftbl.smt.solver.dsl.FormulaBuilder
import tools.aqua.stars.core.types.EntityType
import kotlin.reflect.KClass

inline fun <reified T: EntityType<*, *, *, *, *>> generateSmtLib(
  noinline formula: (CallContextBase<T>) -> FormulaBuilder,
  holdsFor: T,
  name: String,
  ticks: Array<Double>
): String {
  return generateSmtLib(formula, holdsFor, T::class, name, ticks)
}

fun <T: EntityType<*, *, *, *, *>> generateSmtLib(
  formula: (CallContextBase<T>) -> FormulaBuilder,
  holdsFor: T,
  kClass: KClass<T>,
  name: String,
  ticks: Array<Double>
): String {
  val result = StringBuilder()
  val evalTree = formula.generateEvaluation(holdsFor, kClass, name, ticks)
  for (node in evalTree.iterator()) {
    for (emission in node.emissions) {
      when (emission) {
        is DecConstEmission -> {
          result.appendLine("(declare-const ${emission.decConstID} Int)")
        }
        is BindingTermFromChildEmission -> Unit
        is EvalAtTickConstraintEmission -> {
          result.appendLine("(assert ())")
        }
        is EvalInIntervalConstraintEmission -> Unit
        is IDConstraintEmission -> Unit
        is TermFromChildrenConstraintEmission -> Unit
        is TickWitnessTimeEmission -> Unit
      }
    }
  }
  return result.toString()
}