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
  evalTickPrecond: EvaluationTickPrecondition?,
  subFormulaHoldsVariable: String
): IEvalNode {
  println(subFormulaHoldsVariable)
  return when (evalType) {
    EvaluationType.EVALUATE -> {
      require(evalTickPrecond == null) {
        "The generation of until with present tick precondition is not available yet."
      }

      // Prepare result node
      val resultNode = EvalNode(mutableListOf(), evalCtx, mutableListOf(), formula, evalTickIndex, evalTickPrecond)
      val usedUnboundVars = getUsedUnboundVariables(formula.inner, evalCtx)
      val tickWitness = if (usedUnboundVars.isEmpty()) {
        null
      } else {
        "tickWitness_${evalCtx.evaluationIDGenerator.generateID()}"
      }
      if (tickWitness != null) {
        resultNode.emissions.add(NewInstanceEmission(tickWitness))
      }

      // Generate VarIntroNodes for rhs
      val varIntroNodes = mutableListOf<VarIntroNode>()
      var lastEvalCtx = evalCtx
      var lastSubFormulaHolds = subFormulaHoldsVariable
      usedUnboundVars.forEach {
        val newSubFormulaHoldsVariable = "subFormulaHolds_${evalCtx.evaluationIDGenerator.generateID()}"
        val newVarName = "vinst_${lastEvalCtx.evaluationIDGenerator.generateID()}"
        val varID = lastEvalCtx.previouslyAssignedIDs[it]!!
        val newVarIntroNode = VarIntroNode(mutableListOf(), lastEvalCtx, newVarName, it, varID, evalTickIndex,
          formula.interval, lastSubFormulaHolds, newSubFormulaHoldsVariable)
        lastSubFormulaHolds = newSubFormulaHoldsVariable
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
      val rhs = generateEvaluation(formula.inner, lastEvalCtx, EvaluationType.WITNESS, evalTickIndex, formula.interval,
        evalTickPrecond, lastSubFormulaHolds)
      lastNode.children.add(rhs)
      /*
      if (tickWitness != null) {
        // Add [TickWitnessTimeEmission] to right child node of until TODO
        resultNode.children[1].emissions.add(TickWitnessTimeEmission(varIntroNodes.first().emittedID, tickWitness,
          "TEST"))
      }
       */

      resultNode
    }
    else -> error("Nested evaluations with Next are not supported yet.")
  }
}