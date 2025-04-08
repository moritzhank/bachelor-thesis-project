package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.Binding
import de.moritzhank.cmftbl.smt.solver.translation.formula.*

/** Generate an [IEvalNode] from a [Binding]. */
internal fun generateEvaluationForBinding(
  binding: Binding<*>,
  evalCtx: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Double, Double>?,
  evalTickPrecondition: EvaluationTickPrecondition?
): IEvalNode {
  val resultingNode = if (evalType == EvaluationType.EVALUATE) {
    EvalNode(mutableListOf(), evalCtx, mutableListOf(), binding, evalTickIndex, evalTickPrecondition)
  } else {
    require(evalType == EvaluationType.WITNESS)
    WitnessEvalNode(mutableListOf(), evalCtx, mutableListOf(), binding, evalInterval!!, evalTickPrecondition)
  }
  val newEmissionID = evalCtx.constraintIDGenerator.generateID()
  val boundVarID = "bndInst${evalCtx.evaluationIDGenerator.generateID()}"
  val evalTerm = generateEvaluation(binding.bindTerm, evalCtx, evalType, evalTickIndex, evalInterval) as IEvalNodeWithEvaluable
  val newEvalCtx = evalCtx.copy(newBoundCallContext = binding.ccb to boundVarID)
  val evalNode = generateEvaluation(binding.inner, newEvalCtx, evalType, evalTickIndex, evalInterval, null)
  resultingNode.children.addAll(listOf(evalTerm, evalNode))
  resultingNode.emissions.add(NewInstanceEmission(boundVarID))
  resultingNode.emissions.add(BindingTermFromChildEmission(newEmissionID, boundVarID, evalTerm))
  return resultingNode
}
