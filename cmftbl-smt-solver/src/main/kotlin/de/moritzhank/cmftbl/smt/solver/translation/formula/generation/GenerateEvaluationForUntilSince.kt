package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.Relation
import de.moritzhank.cmftbl.smt.solver.dsl.Until
import de.moritzhank.cmftbl.smt.solver.dsl.UntilSinceFormula
import de.moritzhank.cmftbl.smt.solver.misc.convert
import de.moritzhank.cmftbl.smt.solver.misc.mirror
import de.moritzhank.cmftbl.smt.solver.translation.formula.*

/** Generate an [IEvalNode] from a [Until]. */
internal fun generateEvaluationForUntilSince(
  formula: UntilSinceFormula,
  evalCtx: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Double, Double>?
): IEvalNode {
  val newEmissionID = evalCtx.constraintIDGenerator.generateID()
  return if (evalType == EvaluationType.EVALUATE) {
    // Prepare result node
    val resultNode = EvalNode(mutableListOf(), evalCtx, mutableListOf(), formula, evalTickIndex)
    val usedUnboundVarsRhs = getUsedUnboundVariables(formula.rhs, evalCtx)
    val tickWitness = if (usedUnboundVarsRhs.isEmpty()) {
      null
    } else {
      "twtns${evalCtx.evaluationIDGenerator.generateID()}"
    }
    if(tickWitness != null) {
      resultNode.emissions.add(NewInstanceEmission(tickWitness))
    }

    // Evaluate lhs
    val relation = if (formula is Until) Relation.Lt else Relation.Gt
    val tickPreconditionLhs = if (tickWitness == null) null else EvaluationTickPrecondition(tickWitness, relation)
    val interval = if (formula is Until) formula.interval.convert() else formula.interval.convert().mirror()
    val lhs = generateEvaluation(formula.lhs, evalCtx, EvaluationType.UNIV_INST, evalTickIndex, interval,
      tickPreconditionLhs)
    resultNode.children.add(lhs)

    // Generate VarIntroNodes for rhs
    val genVarIntroNodes = generateVarIntroNodes(resultNode, usedUnboundVarsRhs, evalCtx, evalTickIndex,
      interval, null)
    val varIntroNodes = genVarIntroNodes.first
    val lastNode = if (varIntroNodes.isEmpty()) resultNode else varIntroNodes.last()
    val lastEvalCtx = genVarIntroNodes.second

    // Evaluate rhs
    val rhs = generateEvaluation(formula.rhs, lastEvalCtx, EvaluationType.WITNESS, evalTickIndex, interval, null)
    lastNode.children.add(rhs)
    if (tickWitness != null) {
      // Add [TickWitnessTimeEmission] to right child node of until or since
      resultNode.children[1].emissions.add(TickWitnessTimeEmission(newEmissionID, varIntroNodes.first().emittedID,
        tickWitness))
    }

    resultNode
  } else {
    error("Nested evaluations with Until are not supported yet.")
  }
}
