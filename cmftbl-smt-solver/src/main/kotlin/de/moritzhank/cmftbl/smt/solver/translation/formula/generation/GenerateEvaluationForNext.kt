package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.EvaluableRelation
import de.moritzhank.cmftbl.smt.solver.dsl.Next
import de.moritzhank.cmftbl.smt.solver.translation.formula.*

/** Generate an [IEvalNode] from a [EvaluableRelation]. */
internal fun generateEvaluationForNext(
  formula: Next,
  evalCtx: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Int, Int>?,
  evalTickPrecond: EvaluationTickPrecondition?
): IEvalNode {
  return when (evalType) {
    EvaluationType.EVALUATE -> {
      require(evalTickPrecond == null) {
        "The generation of until with present tick precondition is not available yet."
      }
      val newEmissionID = { evalCtx.constraintIDGenerator.generateID() }
      // Prepare result node
      val resultNode = EvalNode(mutableListOf(), evalCtx, mutableListOf(), formula, evalTickIndex, evalTickPrecond)
      val newEmissionIDs = arrayOf(newEmissionID(), newEmissionID())
      resultNode.emissions.add(NextTickExistsInIntervalEmission(newEmissionIDs[0], evalTickIndex, formula.interval))

      // Generate VarIntroNodes
      val usedUnboundVars = getUsedUnboundVariables(formula.inner, evalCtx)
      val varIntroNodes = mutableListOf<VarIntroNode>()
      var lastEvalCtx = evalCtx
      usedUnboundVars.forEach {
        val newVarName = "inst${lastEvalCtx.evaluationIDGenerator.generateID()}"
        val assertedID = lastEvalCtx.previouslyAssignedIDs[it]!!
        val newVarIntroNode = VarIntroNode(mutableListOf(), lastEvalCtx, newVarName, it, assertedID, evalTickIndex + 1,
          null)
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

      // Evaluate inner
      val inner = generateEvaluation(formula.inner, lastEvalCtx, EvaluationType.EVALUATE, evalTickIndex + 1,
        null, evalTickPrecond)
      lastNode.children.add(inner)

      resultNode.emissions.add(FormulaFromChildrenEmission(newEmissionIDs[1], formula.inner,
        inner as IEvalNodeWithEvaluable))

      resultNode
    }
    else -> error("Nested evaluations with Next are not supported yet.")
  }
}