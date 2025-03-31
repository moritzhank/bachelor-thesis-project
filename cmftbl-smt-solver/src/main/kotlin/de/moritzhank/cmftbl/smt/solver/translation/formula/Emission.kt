@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.Formula
import de.moritzhank.cmftbl.smt.solver.dsl.LogicalConnectiveFormula
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

/**
 * Represents an emission of an [IEvalNode] that leds to the declaration of a constant in SMT with the id
 * [decConstID].
 */
internal class DecConstEmission(
  val decConstID: String,
  override val annotation: String? = null
): IEmission


/** Represents an emission of an [IEvalNode] that leds to an assertion in SMT. */
internal sealed interface IAssertionEmission : IEmission

/** Represents the emission that constraints the [VarIntroNode]'s introduced variable to the id [ID]. */
internal class IDConstraintEmission(
  val variableID: String,
  val ID: Int,
  override val annotation: String? = null
): IAssertionEmission

/**
 * Represents the emission that constraint the evaluation of a [VarIntroNode]'s introduced variable to the tick with
 * the index [tickIndex].
 */
internal class EvalAtTickConstraintEmission(
  val variableID: String,
  val tickIndex: Int,
  override val annotation: String? = null
): IAssertionEmission

/**
 * Represents the emission that constraint the evaluation of a [VarIntroNode]'s introduced variable to the interval
 * [interval]. The [interval] is relative in relation to [VarIntroNode.evaluatedTickIndex].
 */
internal class EvalInIntervalConstraintEmission(
  val variableID: String,
  val interval: Pair<Int, Int>?,
  override val annotation: String? = null
): IAssertionEmission {

  init {
    interval.check()
  }

}

/**
 * Represents the emission of an assertion in SMT that is specific to the evaluated node and depends on the child nodes.
 */
internal class TermFromChildrenConstraintEmission(
  val operator: Relation,
  val evalNode1: IEvalNodeWithEvaluable,
  val evalNode2: IEvalNodeWithEvaluable,
  override val annotation: String? = null,
): IAssertionEmission {

  val term1 = evalNode1.evaluable as Term<*>
  val term2 = evalNode2.evaluable as Term<*>

}

/**
 * Represents the emission of an assertion in SMT that is specific to the evaluated node and depends on the child nodes.
 */
internal class FormulaFromChildrenConstraintEmission(
  val formula: LogicalConnectiveFormula,
  val evalNode1: IEvalNodeWithEvaluable,
  val evalNode2: IEvalNodeWithEvaluable,
  override val annotation: String? = null,
): IAssertionEmission {

  val childFormula1 = evalNode1.evaluable as Formula
  val childFormula2 = evalNode2.evaluable as Formula

}

/** Represents the emission of an assertion in SMT that binds a term to a variable. */
internal class BindingTermFromChildEmission(
  val variableID: String,
  val evalNode1: IEvalNodeWithEvaluable,
  override val annotation: String? = null,
): IAssertionEmission {

  val term = evalNode1.evaluable as Term<*>

}

/**
 * Represents the emission of the equality constraint of the top right child of Until or Since that ensures
 * that the tick precondition for the left side is constructed correctly.
 */
internal class TickWitnessTimeEmission(
  val wtnsID: String,
  val twtnsID: String,
  override val annotation: String? = null
): IAssertionEmission

internal fun IEmission.str(): String {
  val annotationInParantheses = if (this.annotation == null) "" else " (${this.annotation})"
  val eq = Relation.Eq.toHTMLString()
  return when(this) {
    is DecConstEmission -> "Emits DEC_CONST $decConstID"
    is IDConstraintEmission -> "Emits ASSERT id($variableID) $eq $ID"
    is EvalAtTickConstraintEmission -> "Emits ASSERT tickIndex($variableID) $eq $tickIndex"
    is EvalInIntervalConstraintEmission -> "Emits ASSERT time($variableID) in ${interval.str()}"
    is TermFromChildrenConstraintEmission -> {
      "Emits ASSERT ${termToString(evalNode1)} ${operator.toHTMLString()} ${termToString(evalNode2)}"
    }
    is BindingTermFromChildEmission -> "Emits ASSERT $variableID $eq ${termToString(evalNode1)}"
    is TickWitnessTimeEmission -> "Emits ASSERT $twtnsID $eq time($wtnsID)"
    is FormulaFromChildrenConstraintEmission -> "Emits ASSERT "
  } + annotationInParantheses
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