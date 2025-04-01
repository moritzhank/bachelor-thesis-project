@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.And
import de.moritzhank.cmftbl.smt.solver.dsl.Formula
import de.moritzhank.cmftbl.smt.solver.dsl.Iff
import de.moritzhank.cmftbl.smt.solver.dsl.Implication
import de.moritzhank.cmftbl.smt.solver.dsl.LogicalConnectiveFormula
import de.moritzhank.cmftbl.smt.solver.dsl.Neg
import de.moritzhank.cmftbl.smt.solver.dsl.Or
import de.moritzhank.cmftbl.smt.solver.dsl.Relation
import de.moritzhank.cmftbl.smt.solver.dsl.Term
import de.moritzhank.cmftbl.smt.solver.dsl.Variable
import de.moritzhank.cmftbl.smt.solver.dsl.str
import de.moritzhank.cmftbl.smt.solver.misc.check

/** Represents an emission of an [IEvalNode]. */
internal sealed interface IEmission {
  /** Provides additional information that can be viewed in the graphical representation. */
  val annotation: String?
}

/** Represents the emission of a new instance (that is a constant function in SMT-Lib) with the id [newInstanceID]. */
internal class NewInstanceEmission(
  val newInstanceID: String,
  val isBool: Boolean = false,
  override val annotation: String? = null
): IEmission

/** Constrains [tools.aqua.stars.core.types.EntityType.id] of [VarIntroNode]'s introduced variable to [id]. */
internal class ConstrainIDEmission(
  val constraintVariableID: String,
  val id: Int,
  val formulaHoldsVariable: String,
  override val annotation: String? = null
) : IEmission

/** Constraints the evaluation of the [VarIntroNode]'s introduced variable to the tick with the index [tickIndex]. */
internal class EvalAtTickConstraintEmission(
  val variableID: String,
  val tickIndex: Int,
  val formulaHoldsVariable: String,
  override val annotation: String? = null
): IEmission

/**
 * Constraints the evaluation of the [VarIntroNode]'s introduced variable to the interval
 * [interval]. The [interval] is relative in relation to [VarIntroNode.evaluatedTickIndex].
 */
internal class EvalInIntervalConstraintEmission(
  val variableID: String,
  val interval: Pair<Int, Int>?,
  val formulaHoldsVariable: String,
  override val annotation: String? = null
): IEmission {

  init {
    interval.check()
  }

}

/** Represents an emission that is specific to the evaluated node and depends on the terms of the child nodes. */
internal class TermFromChildrenEmission(
  val operator: Relation,
  val evalNode1: IEvalNodeWithEvaluable,
  val evalNode2: IEvalNodeWithEvaluable,
  val formulaHoldsVariable: String,
  override val annotation: String? = null,
): IEmission {

  val term1 = evalNode1.evaluable as Term<*>
  val term2 = evalNode2.evaluable as Term<*>

}

/** Represents an emission that is specific to the evaluated node and depends on the formulas of the child nodes. */
internal class FormulaFromChildrenEmission(
  val formula: LogicalConnectiveFormula,
  val evalNode1: IEvalNodeWithEvaluable,
  val evalNode2: IEvalNodeWithEvaluable,
  val formulaHoldsVariable: String,
  val subFormulaHoldsVariable1: String,
  val subFormulaHoldsVariable2: String,
  override val annotation: String? = null,
): IEmission {

  val childFormula1 = evalNode1.evaluable as Formula
  val childFormula2 = evalNode2.evaluable as Formula

}

/** Represents the emission that binds a term to a variable. */
internal class BindingTermFromChildEmission(
  val variableID: String,
  val evalNode: IEvalNodeWithEvaluable,
  val formulaHoldsVariable: String,
  override val annotation: String? = null,
): IEmission {

  val term = evalNode.evaluable as Term<*>

}

/** Represents the emission that ties the tick of [witnessID] to [tickWitnessID]. */
internal class TickWitnessTimeEmission(
  val witnessID: String,
  val tickWitnessID: String,
  val formulaHoldsVariable: String,
  override val annotation: String? = null
): IEmission

/** Represents the emission that ensures all emissions of a node hold. */
internal class SubFormulaeHoldEmission(
  val formulaHoldsVariable: String,
  val subFormulaHoldsVariables: List<String>,
  override val annotation: String? = null
): IEmission

/** Generate a String representation of [IEmission] suitable for visualization. */
internal fun IEmission.str(debugMode: Boolean = true): String {
  val annotationInParentheses = if (this.annotation == null) "" else " (${this.annotation})"
  val eq = Relation.Eq.toHTMLString()
  if (debugMode) {
    return when(this) {
      is BindingTermFromChildEmission -> "Emits ASSERT $formulaHoldsVariable := ($variableID $eq " +
              "${termToString(evalNode)})"
      is ConstrainIDEmission -> "Emits ASSERT $formulaHoldsVariable := (id($constraintVariableID) $eq $id)"
      is EvalAtTickConstraintEmission -> "Emits ASSERT $formulaHoldsVariable := (tickIndex($variableID) $eq $tickIndex)"
      is EvalInIntervalConstraintEmission -> "Emits ASSERT $formulaHoldsVariable := (time($variableID) in " +
              "${interval.str()})"
      is TickWitnessTimeEmission -> "Emits ASSERT $formulaHoldsVariable := ($tickWitnessID $eq time($witnessID))"
      is NewInstanceEmission -> "Emits DEC_CONST $newInstanceID (${if (isBool) "Bool" else "Int"})"
      is FormulaFromChildrenEmission -> {
        val connectiveString = binaryLogicalConnectiveToString(formula)
        "Emits ASSERT $formulaHoldsVariable := ($subFormulaHoldsVariable1 $connectiveString $subFormulaHoldsVariable2)"
      }
      is TermFromChildrenEmission -> {
        "Emits ASSERT $formulaHoldsVariable := (${termToString(evalNode1)} ${operator.toHTMLString()} " +
                "${termToString(evalNode2)})"
      }
      is SubFormulaeHoldEmission -> "Emits ASSERT $formulaHoldsVariable := allHold: $subFormulaHoldsVariables"
    } + annotationInParentheses
  } else {
    return when(this) {
      is BindingTermFromChildEmission -> "Emits ASSERT $variableID $eq ${termToString(evalNode)}"
      is ConstrainIDEmission -> "Emits ASSERT id($constraintVariableID) $eq $id"
      is EvalAtTickConstraintEmission -> "Emits ASSERT tickIndex($variableID) $eq $tickIndex"
      is EvalInIntervalConstraintEmission -> "Emits ASSERT time($variableID) in ${interval.str()}"
      is TickWitnessTimeEmission -> "Emits ASSERT $tickWitnessID $eq time($witnessID)"
      is NewInstanceEmission -> {
        if (!isBool) {
          "Emits DEC_CONST $newInstanceID"
        } else {
          ""
        }
      }
      is FormulaFromChildrenEmission -> {
        val connectiveString = binaryLogicalConnectiveToString(formula)
        "Emits ASSERT eval(lhs) $connectiveString eval(rhs)"
      }
      is TermFromChildrenEmission -> {
        "Emits ASSERT ${termToString(evalNode1)} ${operator.toHTMLString()} ${termToString(evalNode2)}"
      }
      is SubFormulaeHoldEmission -> ""
    } + annotationInParentheses
  }
}

private fun termToString(node: IEvalNodeWithEvaluable): String {
  val evaluable = node.evaluable
  require(evaluable is Term<*>)
  var replaceBaseWith: String? = if (evaluable is Variable<*>) {
    node.evaluationContext.getSmtID(evaluable.callContext.base())
  } else {
    null
  }
  return evaluable.str(replaceBaseWith)
}

private fun binaryLogicalConnectiveToString(formula: LogicalConnectiveFormula): String {
  return when(formula) {
    is Neg -> error("Not supported.")
    is And -> "&and;"
    is Iff -> "&hArr;"
    is Implication -> "&rArr;"
    is Or -> "&or;"
  }
}