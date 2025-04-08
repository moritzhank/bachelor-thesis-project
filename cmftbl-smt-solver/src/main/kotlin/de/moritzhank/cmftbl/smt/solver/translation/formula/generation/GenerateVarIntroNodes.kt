package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationContext
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationTickPrecondition
import de.moritzhank.cmftbl.smt.solver.translation.formula.IEvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.VarIntroNode

/** Generate [VarIntroNode] for all following unbound variables. */
internal fun generateVarIntroNodes(
  prevNode: IEvalNode?,
  usedUnboundVars: Set<CCB<*>>,
  evalCtx: EvaluationContext,
  evalTickIndex: Int,
  evalInterval: Pair<Int, Int>?,
  evalTickPrecondition: EvaluationTickPrecondition?
): Pair<List<VarIntroNode>, EvaluationContext> {
  val varIntroNodes = mutableListOf<VarIntroNode>()
  var lastEvalCtx = evalCtx
  var tickPrecondition = evalTickPrecondition
  var lastInstanceName: String? = null
  usedUnboundVars.forEach {
    val newVarName = "inst${lastEvalCtx.evaluationIDGenerator.generateID()}"
    val assertedID = lastEvalCtx.previouslyAssignedIDs[it]!!
    val newVarIntroNode = VarIntroNode(mutableListOf(), lastEvalCtx, newVarName, it, assertedID, evalTickIndex,
      evalInterval, tickPrecondition, lastInstanceName)
    tickPrecondition = null
    varIntroNodes.add(newVarIntroNode)
    lastEvalCtx = lastEvalCtx.copy(newIntroducedVariable = it to newVarIntroNode)
    lastInstanceName = newVarName
  }
  varIntroNodes.forEachIndexed { i, node ->
    if (i + 1 < varIntroNodes.size) {
      node.children.add(varIntroNodes[i + 1])
    }
  }
  if (varIntroNodes.isNotEmpty() && prevNode != null) {
    prevNode.children.add(varIntroNodes.first())
  }
  return Pair(varIntroNodes, lastEvalCtx)
}
