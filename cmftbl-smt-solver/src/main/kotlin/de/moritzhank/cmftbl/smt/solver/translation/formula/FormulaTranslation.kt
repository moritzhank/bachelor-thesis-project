package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.CallContextBase
import de.moritzhank.cmftbl.smt.solver.dsl.Constant
import de.moritzhank.cmftbl.smt.solver.dsl.FormulaBuilder
import de.moritzhank.cmftbl.smt.solver.dsl.Term
import de.moritzhank.cmftbl.smt.solver.dsl.Variable
import de.moritzhank.cmftbl.smt.solver.misc.idMemberName
import de.moritzhank.cmftbl.smt.solver.misc.tickMemberName
import de.moritzhank.cmftbl.smt.solver.misc.toSmtRepresentation
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
  val tickDataSeconds = { tickID: String ->
    "(tickDataUnitSeconds_tickSeconds (tickData_currentTick $tickID))"
  }
  for (node in evalTree.iterator()) {
    for (emission in node.emissions) {
      when (emission) {
        is DecConstEmission -> {
          result.appendLine("(declare-const ${emission.decConstID} Int)")
        }
        is BindingTermFromChildEmission -> {
          val term = emission.term
          val smtTerm = termToSmtRepresentation(term) { v ->
            node.evaluationContext.getSmtID(v.callContext.base())!!
          }
          result.appendLine("(assert (= ${emission.variableID} $smtTerm))")
        }
        is EvalAtTickConstraintEmission -> {
          require(node is VarIntroNode)
          val ccb = node.referenceCCB
          val tickIndex = emission.tickIndex
          val varID = emission.variableID
          result.appendLine("(assert (= (${ccb.tickMemberName()} $varID) (indexToTick $tickIndex)))")
        }
        is EvalInIntervalConstraintEmission -> {
          require(node is VarIntroNode)
          val varID = emission.variableID
          val tickSeconds = tickDataSeconds("(${node.referenceCCB.tickMemberName()} $varID)")
          val evaluatedTickIndexSeconds = tickDataSeconds("(indexToTick ${node.evaluatedTickIndex})")
          val intervalLeft = "(+ ${emission.interval?.first ?: 0} $evaluatedTickIndexSeconds)"
          result.appendLine("(assert (>= $tickSeconds $intervalLeft))")
          if (emission.interval != null) {
            val intervalRight = "(+ ${emission.interval.second} $evaluatedTickIndexSeconds)"
            result.appendLine("(assert (<= $tickSeconds $intervalRight))")
          }
        }
        is IDConstraintEmission -> {
          require(node is VarIntroNode)
          val idOfVar = "(${node.referenceCCB.idMemberName()} ${emission.variableID})"
          result.appendLine("(assert (= $idOfVar ${emission.ID}))")
        }
        is TickWitnessTimeEmission -> {
          require(node is VarIntroNode)
          result.appendLine("(assert (= (${node.referenceCCB.tickMemberName()} ${emission.wtnsID}) ${emission.twtnsID}))")
        }
        is TermFromChildrenConstraintEmission -> {
          val smtTerm1 = termToSmtRepresentation(emission.term1) { v ->
            node.evaluationContext.getSmtID(v.callContext.base())!!
          }
          val smtTerm2 = termToSmtRepresentation(emission.term2) { v ->
            node.evaluationContext.getSmtID(v.callContext.base())!!
          }
          result.appendLine("(assert (${emission.operator.toSMTString()} $smtTerm1 $smtTerm2))")
        }
      }
    }
  }
  return result.toString()
}

private fun termToSmtRepresentation(term: Term<*>, baseElem: (Variable<*>) -> String): String {
  return when (term) {
    is Constant<*> -> term.value.toString()
    is Variable<*> -> {
      term.callContext.toSmtRepresentation(baseElem(term))
    }
  }
}