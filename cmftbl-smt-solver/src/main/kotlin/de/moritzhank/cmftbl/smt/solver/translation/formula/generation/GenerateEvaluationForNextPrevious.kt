@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.EvaluableRelation
import de.moritzhank.cmftbl.smt.solver.dsl.Next
import de.moritzhank.cmftbl.smt.solver.dsl.NextPreviousFormula
import de.moritzhank.cmftbl.smt.solver.misc.convert
import de.moritzhank.cmftbl.smt.solver.misc.mirror
import de.moritzhank.cmftbl.smt.solver.translation.formula.*

/** Generate an [IEvalNode] from a [EvaluableRelation]. */
internal fun generateEvaluationForNextPrevious(
  formula: NextPreviousFormula,
  evalCtx: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Double, Double>?
): IEvalNode {
  return when (evalType) {
    EvaluationType.EVALUATE -> {
      val newEmissionID = { evalCtx.constraintIDGenerator.generateID() }
      // Prepare result node
      val resultNode = EvalNode(mutableListOf(), evalCtx, mutableListOf(), formula, evalTickIndex)
      val newEmissionIDs = arrayOf(newEmissionID())
      val newEvalTickIndex = if (formula is Next) {
        resultNode.emissions.add(TickIndexExistsInIntervalEmission(newEmissionIDs[0], evalTickIndex + 1, evalTickIndex,
          formula.interval.convert()))
        evalTickIndex + 1
      } else {
        resultNode.emissions.add(TickIndexExistsInIntervalEmission(newEmissionIDs[0], evalTickIndex - 1, evalTickIndex,
          formula.interval.convert().mirror()))
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

      resultNode
    }
    else -> error("Nested evaluations with Next are not supported yet.")
  }
}
