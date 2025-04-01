package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.EvaluableRelation
import de.moritzhank.cmftbl.smt.solver.translation.formula.*

/** Generate an [IEvalNode] from a [EvaluableRelation]. */
internal fun generateEvaluationForEvaluableRelation(
  evaluableRelation: EvaluableRelation<*>,
  evalContext: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Int, Int>?,
  evalTickPrecondition: EvaluationTickPrecondition?,
  subFormulaHoldsVariable: String
): IEvalNode {
  if (evalType == EvaluationType.UNIV_INST) {
    return UniversalEvalNode(evalContext, evaluableRelation, evalTickIndex, evalTickPrecondition, evalInterval?.second)
  }
  val lhs = generateEvaluation(evaluableRelation.lhs, evalContext, evalType, evalTickIndex,
    evalInterval) as IEvalNodeWithEvaluable
  val rhs = generateEvaluation(evaluableRelation.rhs, evalContext, evalType, evalTickIndex,
    evalInterval) as IEvalNodeWithEvaluable
  val emissions = mutableListOf<IEmission>(TermFromChildrenEmission(evaluableRelation.type, lhs, rhs,
    subFormulaHoldsVariable))
  return if (evalType == EvaluationType.EVALUATE) {
    EvalNode(mutableListOf(lhs, rhs), evalContext, emissions, evaluableRelation, evalTickIndex, evalTickPrecondition)
  } else {
    require(evalType == EvaluationType.WITNESS)
    WitnessEvalNode(mutableListOf(lhs, rhs), evalContext, emissions, evaluableRelation, evalInterval,
      evalTickPrecondition)
  }
}
