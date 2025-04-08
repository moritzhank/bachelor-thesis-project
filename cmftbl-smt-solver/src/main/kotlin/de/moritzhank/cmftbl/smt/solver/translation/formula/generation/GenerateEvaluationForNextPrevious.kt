package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.EvaluableRelation
import de.moritzhank.cmftbl.smt.solver.dsl.Next
import de.moritzhank.cmftbl.smt.solver.dsl.NextPreviousFormula
import de.moritzhank.cmftbl.smt.solver.misc.negate
import de.moritzhank.cmftbl.smt.solver.translation.formula.*

/** Generate an [IEvalNode] from a [EvaluableRelation]. */
internal fun generateEvaluationForNextPrevious(
  formula: NextPreviousFormula,
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
      val newEvalTickIndex = if (formula is Next) {
        resultNode.emissions.add(TickIndexExistsInIntervalEmission(newEmissionIDs[0], evalTickIndex + 1,
          formula.interval, false))
        evalTickIndex + 1
      } else {
        resultNode.emissions.add(TickIndexExistsInIntervalEmission(newEmissionIDs[0], evalTickIndex - 1,
          formula.interval.negate(), true))
        evalTickIndex - 1
      }

      // Generate VarIntroNodes
      val usedUnboundVars = getUsedUnboundVariables(formula.inner, evalCtx)
      val genVarIntroNodes = generateVarIntroNodes(resultNode, usedUnboundVars, evalCtx, newEvalTickIndex, null, null)
      val varIntroNodes = genVarIntroNodes.first
      val lastNode = if (varIntroNodes.isEmpty()) resultNode else varIntroNodes.last()
      val lastEvalCtx = genVarIntroNodes.second

      // Evaluate inner
      val inner = generateEvaluation(formula.inner, lastEvalCtx, EvaluationType.EVALUATE, newEvalTickIndex, null, null)
      lastNode.children.add(inner)

      resultNode.emissions.add(FormulaFromChildrenEmission(newEmissionIDs[1], formula.inner,
        inner as IEvalNodeWithEvaluable))
      resultNode
    }
    else -> error("Nested evaluations with Next are not supported yet.")
  }
}