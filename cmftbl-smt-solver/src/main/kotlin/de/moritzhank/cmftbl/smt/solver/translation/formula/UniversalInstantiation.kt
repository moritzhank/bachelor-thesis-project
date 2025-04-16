@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.misc.isMirrored
import de.moritzhank.cmftbl.smt.solver.misc.mirror
import de.moritzhank.cmftbl.smt.solver.translation.formula.generation.generateVarIntroNodes

/** Replaces all [UniversalEvalNode] with their respective instantiation. */
internal fun eliminateUniversalQuantification(
  rootNode: IEvalNode,
  ticks: Array<Double>
) {
  while(true) {
    val listOfNodes = rootNode.iterator().asSequence().toList()
    val universalEvalNode = listOfNodes.find { it is UniversalEvalNode } as? UniversalEvalNode ?: break
    val parent = listOfNodes.find { it.children.contains(universalEvalNode) } as EvalNode
    instantiateUniversalQuantification(universalEvalNode, parent, ticks)
  }
}

private fun instantiateUniversalQuantification(
  node: UniversalEvalNode,
  parent: EvalNode,
  ticks: Array<Double>
) {
  val currentTick = node.evaluatedTickIndex
  val ticksInInterval = getTicksInInterval(currentTick, ticks, node.interval)
  val instantiatedNodes = mutableListOf<IEvalNode>()
  val evalCtx = node.evalCtx
  val usedUnboundVars =  getUsedUnboundVariables(node.evaluable , evalCtx)
  ticksInInterval.forEach {
    val evalTickIndex = it.first
    val genVarIntroNodes = generateVarIntroNodes(null, usedUnboundVars, evalCtx, evalTickIndex, null,
      node.tickPrecondition)
    val varIntroNodes = genVarIntroNodes.first
    val lastEvalCtx = genVarIntroNodes.second
    val newTickPrecond = if (varIntroNodes.isEmpty()) node.tickPrecondition else null
    val newNode = generateEvaluation(node.evaluable, lastEvalCtx, EvaluationType.EVALUATE, evalTickIndex, null,
      newTickPrecond)
    varIntroNodes.lastOrNull()?.children?.add(newNode)
    instantiatedNodes.add(varIntroNodes.firstOrNull() ?: newNode)
  }
  val childIndex = parent.children.indexOf(node)
  parent.children.removeAt(childIndex)
  if (node.prevalenceFlag) {
    parent.children.addAll(childIndex, instantiatedNodes)
  } else {
    parent.children.add(childIndex, OrgaEvalNode(instantiatedNodes, evalCtx, "UNIV_INST"))
  }
}


private fun getTicksInInterval(
  currentTick: Int,
  ticks: Array<Double>,
  interval: Pair<Double, Double>
): List<Pair<Int, Double>> {
  val listOfIndexedTicks = ticks.mapIndexed { index, tick -> Pair(index, tick) }.toMutableList()
  listOfIndexedTicks.removeIf {
    if (!interval.isMirrored()) {
      val tooSmall = it.second < ticks[currentTick]
      val tooBig = if (interval.second == Double.POSITIVE_INFINITY) false else it.second > (ticks[currentTick] + interval.second)
      tooSmall || tooBig
    } else {
      val tooBig = it.second > ticks[currentTick]
      val tooSmall = if (interval.first == Double.NEGATIVE_INFINITY) false else it.second < (ticks[currentTick] + interval.first)
      tooSmall || tooBig
    }
  }
  return listOfIndexedTicks
}

private fun testsForGetTicksInInterval() {
  val ticks = arrayOf(1.0, 2.0, 3.0, 4.0, 5.5)
  var result: List<Pair<Int, Double>>
  result = getTicksInInterval(0, ticks, Pair(0.0, Double.POSITIVE_INFINITY))
  require("[(0, 1.0), (1, 2.0), (2, 3.0), (3, 4.0), (4, 5.5)]" == result.toString())
  result = getTicksInInterval(2, ticks, Pair(0.0, Double.POSITIVE_INFINITY))
  require("[(2, 3.0), (3, 4.0), (4, 5.5)]" == result.toString())
  result = getTicksInInterval(2, ticks, Pair(0.0, 1.0))
  require("[(2, 3.0), (3, 4.0)]" == result.toString())
  // Mirrored intervals
  result = getTicksInInterval(4, ticks, Pair(Double.NEGATIVE_INFINITY, 0.0))
  require("[(0, 1.0), (1, 2.0), (2, 3.0), (3, 4.0), (4, 5.5)]" == result.toString())
  result = getTicksInInterval(3, ticks, Pair(0.0, 1.0).mirror())
  require("[(2, 3.0), (3, 4.0)]" == result.toString())
}
