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
  evalTickPrecondition: EvaluationTickPrecondition?
): IEvalNode {
  val resultNode = if (evalType == EvaluationType.EVALUATE) {
    EvalNode(mutableListOf(), evalContext, mutableListOf(), evaluableRelation, evalTickIndex, evalTickPrecondition)
  } else {
    require(evalType == EvaluationType.WITNESS)
    WitnessEvalNode(mutableListOf(), evalContext, mutableListOf(), evaluableRelation, evalInterval,
      evalTickPrecondition)
  }
  val newEmissionID = evalContext.constraintIDGenerator.generateID()
  val lhs = generateEvaluation(evaluableRelation.lhs, evalContext, evalType, evalTickIndex,
    evalInterval) as IEvalNodeWithEvaluable
  val rhs = generateEvaluation(evaluableRelation.rhs, evalContext, evalType, evalTickIndex,
    evalInterval) as IEvalNodeWithEvaluable
  resultNode.children.addAll(listOf(lhs, rhs))
  resultNode.emissions.add(TermFromChildrenEmission(newEmissionID, evaluableRelation.type, lhs, rhs))
  return resultNode
}
