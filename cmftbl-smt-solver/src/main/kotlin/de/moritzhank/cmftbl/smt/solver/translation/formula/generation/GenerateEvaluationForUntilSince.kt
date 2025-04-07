package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.Relation
import de.moritzhank.cmftbl.smt.solver.dsl.Until
import de.moritzhank.cmftbl.smt.solver.dsl.UntilSinceFormula
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationContext
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationTickPrecondition
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationType
import de.moritzhank.cmftbl.smt.solver.translation.formula.IEvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.NewInstanceEmission
import de.moritzhank.cmftbl.smt.solver.translation.formula.TickWitnessTimeEmission
import de.moritzhank.cmftbl.smt.solver.translation.formula.VarIntroNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.generateEvaluation
import de.moritzhank.cmftbl.smt.solver.translation.formula.getUsedUnboundVariables

/** Generate an [IEvalNode] from a [Until]. */
internal fun generateEvaluationForUntilSince(
  formula: UntilSinceFormula,
  evalCtx: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Int, Int>?,
  evalTickPrecond: EvaluationTickPrecondition?
): IEvalNode {
  val newEmissionID = { evalCtx.constraintIDGenerator.generateID() }
  return if (evalType == EvaluationType.EVALUATE) {
    require(evalTickPrecond == null) {
      "The generation of until with present tick precondition is not available yet."
    }

    // Prepare result node
    val resultNode = EvalNode(mutableListOf(), evalCtx, mutableListOf(), formula, evalTickIndex, evalTickPrecond)
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
    val tickPreconditionLhs = if(tickWitness == null) null else EvaluationTickPrecondition(tickWitness, Relation.Lt)
    val lhs = generateEvaluation(formula.lhs, evalCtx, EvaluationType.UNIV_INST, evalTickIndex, formula.interval,
      tickPreconditionLhs)
    resultNode.children.add(lhs)

    // Generate VarIntroNodes for rhs
    val varIntroNodes = mutableListOf<VarIntroNode>()
    var lastEvalCtx = evalCtx
    usedUnboundVarsRhs.forEach {
      val newVarName = "inst${lastEvalCtx.evaluationIDGenerator.generateID()}"
      val varID = lastEvalCtx.previouslyAssignedIDs[it]!!
      val newVarIntroNode = VarIntroNode(mutableListOf(), lastEvalCtx, newVarName, it, varID, evalTickIndex,
        formula.interval)
      varIntroNodes.add(newVarIntroNode)
      lastEvalCtx = lastEvalCtx.copy(newIntroducedVariable = it to newVarIntroNode)
    }
    varIntroNodes.forEachIndexed { i, node ->
      if (i + 1 < varIntroNodes.size) {
        node.children.add(varIntroNodes[i + 1])
      }
    }
    var lastNode: IEvalNode = resultNode
    if (varIntroNodes.isNotEmpty()) {
      resultNode.children.add(varIntroNodes.first())
      lastNode = varIntroNodes.last()
    }

    // Evaluate rhs
    val rhs = generateEvaluation(formula.rhs, lastEvalCtx, EvaluationType.WITNESS, evalTickIndex, formula.interval, null)
    lastNode.children.add(rhs)
    if (tickWitness != null) {
      // Add [TickWitnessTimeEmission] to right child node of until
      resultNode.children[1].emissions.add(TickWitnessTimeEmission(newEmissionID(), varIntroNodes.first().emittedID,
        tickWitness))
    }

    resultNode
  } else {
    error("Nested evaluations with Until are not supported yet.")
  }
}
