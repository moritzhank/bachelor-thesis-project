@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.*
import de.moritzhank.cmftbl.smt.solver.misc.str

/** Represents an emission of an [IEvalNode]. */
internal sealed interface IEmission {
  /** Is used to generate logic structure of formula. */
  val emissionID: Int?
  /** Provides additional information that can be viewed in the graphical representation. */
  val annotation: String?
}

/** Represents the emission of a new instance (that is a constant function in SMT-Lib) with the id [newInstanceID]. */
internal class NewInstanceEmission(
  val newInstanceID: String,
  val isBool: Boolean = false,
  override val annotation: String? = null
): IEmission {

  override val emissionID: Int? = null

}

/** Constrains [tools.aqua.stars.core.types.EntityType.id] of [VarIntroNode]'s introduced variable to [id]. */
internal class ConstrainIDEmission(
  override val emissionID: Int?,
  val constraintVariableID: String,
  val id: Int,
  override val annotation: String? = null
) : IEmission

/** Constraints the evaluation of the [VarIntroNode]'s introduced variable to the tick with the index [tickIndex]. */
internal class EvalAtTickConstraintEmission(
  override val emissionID: Int?,
  val variableID: String,
  val tickIndex: Int,
  override val annotation: String? = null
): IEmission

/**
 * Constraints the evaluation of the [VarIntroNode]'s introduced variable to the interval
 * [interval]. The [interval] is relative in relation to [VarIntroNode.evaluatedTickIndex].
 */
internal class EvalInIntervalConstraintEmission(
  override val emissionID: Int?,
  val variableID: String,
  val interval: Pair<Double, Double>,
  override val annotation: String? = null
): IEmission

/** Represents an emission that is specific to the evaluated node and depends on the terms of the child nodes. */
internal class TermFromChildrenEmission(
  override val emissionID: Int?,
  val operator: Relation,
  val evalNode1: IEvalNodeWithEvaluable,
  val evalNode2: IEvalNodeWithEvaluable,
  override val annotation: String? = null,
): IEmission {

  val term1 = evalNode1.evaluable as Term<*>
  val term2 = evalNode2.evaluable as Term<*>

}

/** Represents an emission that is specific to the evaluated node and depends on the formulas of the child nodes. */
internal class FormulaeFromChildrenEmission(
  override val emissionID: Int?,
  val formula: LogicalConnectiveFormula,
  val evalNode1: IEvalNodeWithEvaluable,
  val evalNode2: IEvalNodeWithEvaluable,
  override val annotation: String? = null,
): IEmission {

  val childFormula1 = evalNode1.evaluable as Formula
  val childFormula2 = evalNode2.evaluable as Formula

}

/** Represents the emission that binds a term to a variable. */
internal class BindingTermFromChildEmission(
  override val emissionID: Int?,
  val variableID: String,
  val evalNode: IEvalNodeWithEvaluable,
  override val annotation: String? = null,
): IEmission {

  val term = evalNode.evaluable as Term<*>

}

/** Represents the emission that ties the tick of [witnessID] to [tickWitnessID]. */
internal class TickWitnessTimeEmission(
  override val emissionID: Int?,
  val witnessID: String,
  val tickWitnessID: String,
  override val annotation: String? = null
): IEmission

/** Represents the emission that checks if the tick with [tickIndex] exists in [interval]. */
internal class TickIndexExistsInIntervalEmission(
  override val emissionID: Int?,
  val tickIndex: Int,
  val referenceTickIndex: Int,
  val interval: Pair<Double, Double>,
  override val annotation: String? = null
): IEmission

/** Represents the emission that ensures that the time of this element is the same as [referenceID]. */
internal class SameTimeEmission(
  override val emissionID: Int?,
  val referenceID: String,
  val referenceCCB: CCB<*>,
  override val annotation: String? = null
) : IEmission

/** Generate a String representation of [IEmission] suitable for visualization. */
internal fun IEmission.tableStr(): String {
  return when(this) {
    is NewInstanceEmission -> "<TR><TD COLSPAN=\"2\">DEC</TD><TD>${str()}</TD></TR>"
    else -> "<TR><TD>$emissionID</TD><TD>ASS</TD><TD>${str()}</TD></TR>"
  }
}

/** Represents an emission that ensures a [fraction] of children of [evalNode] hold. */
internal class PrevalenceOfChildrenEmission(
  override val emissionID: Int?,
  val fraction: Double,
  val evalNode: IEvalNode,
  override val annotation: String? = null,
): IEmission

/** Generate a String representation of [IEmission] suitable for visualization. */
internal fun IEmission.str(): String {
  val annotationInParentheses = if (this.annotation == null) "" else " (${this.annotation})"
  return when(this) {
    is BindingTermFromChildEmission -> "$variableID = ${termToString(evalNode)}"
    is ConstrainIDEmission -> "id($constraintVariableID) = $id"
    is EvalAtTickConstraintEmission -> "tickIndex($variableID) = $tickIndex"
    is EvalInIntervalConstraintEmission -> "time($variableID) in ${interval.str()}"
    is TickWitnessTimeEmission -> "$tickWitnessID = time($witnessID)"
    is NewInstanceEmission -> "$newInstanceID (${if (isBool) "Bool" else "Int"})"
    is FormulaeFromChildrenEmission -> {
      val connectiveString = binaryLogicalConnectiveToString(formula)
      "eval(lhs) $connectiveString eval(rhs)"
    }
    is TermFromChildrenEmission -> "${termToString(evalNode1)} ${operator.toHTMLString()} ${termToString(evalNode2)}"
    is TickIndexExistsInIntervalEmission -> {
      "indexToTick($tickIndex) ${Relation.Ne.toHTMLString()} -1 &and; time(indexToTick($tickIndex)) in " +
              interval.str()
    }
    is SameTimeEmission -> "tickTime(this) = tickTime($referenceID)"
    is PrevalenceOfChildrenEmission -> "Subformulae hold for ${fraction * 100}%"
  } + annotationInParentheses
}

private fun termToString(node: IEvalNodeWithEvaluable): String {
  val evaluable = node.evaluable
  require(evaluable is Term<*>)
  val replaceBaseWith: String? = if (evaluable is Variable<*>) {
    node.evalCtx.getSmtID(evaluable.callContext.base())
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
