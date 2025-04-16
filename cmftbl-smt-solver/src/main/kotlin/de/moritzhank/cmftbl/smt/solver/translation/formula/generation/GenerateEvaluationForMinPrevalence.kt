package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.MinPrevalence
import de.moritzhank.cmftbl.smt.solver.misc.convert
import de.moritzhank.cmftbl.smt.solver.translation.formula.*

/** Generate an [IEvalNode] from [MinPrevalence]. */
internal fun generateEvaluationForMinPrevalence(
  formula: MinPrevalence,
  evalCtx: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Double, Double>?
): IEvalNode {
  return if (evalType == EvaluationType.EVALUATE) {
    // Prepare result node
    val resultNode = EvalNode(mutableListOf(), evalCtx, mutableListOf(), formula, evalTickIndex)
    resultNode.childSatNotRequired = true
    val newEmissionID = evalCtx.constraintIDGenerator.generateID()
    resultNode.emissions.add(PrevalenceOfChildrenEmission(newEmissionID, formula.fraction, resultNode))

    val interval = formula.interval.convert()
    val inner = generateEvaluation(formula.inner, evalCtx, EvaluationType.UNIV_INST, evalTickIndex, interval, null)
    (inner as UniversalEvalNode).prevalenceFlag = true
    resultNode.children.add(inner)

    resultNode
  } else {
    error("Nested evaluations with MinPrevalence are not supported yet.")
  }
}
