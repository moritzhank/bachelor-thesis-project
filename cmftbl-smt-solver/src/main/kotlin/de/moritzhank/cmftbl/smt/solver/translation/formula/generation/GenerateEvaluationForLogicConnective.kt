package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.EvaluableRelation
import de.moritzhank.cmftbl.smt.solver.dsl.LogicalConnectiveFormula
import de.moritzhank.cmftbl.smt.solver.dsl.Neg
import de.moritzhank.cmftbl.smt.solver.misc.lhs
import de.moritzhank.cmftbl.smt.solver.misc.rhs
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationContext
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationTickPrecondition
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationType
import de.moritzhank.cmftbl.smt.solver.translation.formula.FormulaeFromChildrenEmission
import de.moritzhank.cmftbl.smt.solver.translation.formula.IEmission
import de.moritzhank.cmftbl.smt.solver.translation.formula.IEvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.IEvalNodeWithEvaluable
import de.moritzhank.cmftbl.smt.solver.translation.formula.UniversalEvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.WitnessEvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.generateEvaluation


/** Generate an [IEvalNode] from a [EvaluableRelation]. */
internal fun generateEvaluationForLogicConnective(
  formula: LogicalConnectiveFormula,
  evalContext: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Int, Int>?,
  evalTickPrecond: EvaluationTickPrecondition?
): IEvalNode {
  if (formula is Neg) {
    error("Evaluating Neg is not available yet.")
  }
  val resultNode = if (evalType == EvaluationType.EVALUATE) {
    EvalNode(mutableListOf(), evalContext, mutableListOf(), formula, evalTickIndex, evalTickPrecond)
  } else {
    require(evalType == EvaluationType.WITNESS)
    WitnessEvalNode(mutableListOf(), evalContext, mutableListOf(), formula, evalInterval, evalTickPrecond)
  }
  val newEmissionID = evalContext.constraintIDGenerator.generateID()
  val lhs = generateEvaluation(formula.lhs(), evalContext, evalType, evalTickIndex, evalInterval, evalTickPrecond)
  val rhs = generateEvaluation(formula.rhs(), evalContext, evalType, evalTickIndex, evalInterval, evalTickPrecond)
  resultNode.children.addAll(listOf(lhs, rhs))
  resultNode.emissions.add(FormulaeFromChildrenEmission(newEmissionID, formula, lhs as IEvalNodeWithEvaluable,
    rhs as IEvalNodeWithEvaluable))
  return resultNode
}
