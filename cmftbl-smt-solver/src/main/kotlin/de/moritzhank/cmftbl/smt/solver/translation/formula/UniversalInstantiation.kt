@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.translation.formula

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
  val ticksInInterval = getTicksInInterval(currentTick, ticks, node.rightBorderOfInterval)
  val instantiatedNodes = mutableListOf<IEvalNode>()
  var evalCtx = node.evaluationContext
  val usedUnboundVars =  getUsedUnboundVariables(node.evaluable , evalCtx)
  ticksInInterval.forEach {
    val evalTickIndex = it.first
    var firstVarIntroNode: VarIntroNode? = null
    var prevIntroNode: VarIntroNode? = null
    usedUnboundVars.forEach {
      val emittedID = "uinst_${evalCtx.evaluationIDGenerator.generateID()}"
      val assignedID = evalCtx.previouslyAssignedIDs[it]!!
      val newVarIntroNode = VarIntroNode(mutableListOf() ,evalCtx, emittedID, it, assignedID, evalTickIndex, null, "")
      evalCtx = evalCtx.copy(newIntroducedVariable = it to newVarIntroNode)
      prevIntroNode?.children?.add(newVarIntroNode)
      prevIntroNode = newVarIntroNode
      if (firstVarIntroNode == null) {
        firstVarIntroNode = newVarIntroNode
      }
    }
    var newNode = generateEvaluation(node.evaluable, evalCtx, EvaluationType.EVALUATE, evalTickIndex, null,
      node.tickPrecondition, "")
    firstVarIntroNode?.children?.add(newNode)
    instantiatedNodes.add(firstVarIntroNode ?: newNode)
  }
  val childIndex = parent.children.indexOf(node)
  parent.children.removeAt(childIndex)
  parent.children.add(childIndex, OrgaEvalNode(instantiatedNodes, evalCtx, "UNIV_INST"))
}


private fun getTicksInInterval(currentTick: Int, ticks: Array<Double>, rightIntervalBorder: Int?): List<Pair<Int, Double>> {
  val listOfIndexedTicks = ticks.mapIndexed { index, tick -> Pair(index, tick) }.toMutableList()
  listOfIndexedTicks.removeIf {
    val tooSmall = it.second < ticks[currentTick]
    val tooBig = if (rightIntervalBorder == null) false else it.second > ticks[currentTick] + rightIntervalBorder
    tooSmall || tooBig
  }
  return listOfIndexedTicks
}

private fun testsForGetTicksInInterval() {
  val ticks = arrayOf(1.0, 2.0, 3.0, 4.0, 5.5)
  var result: List<Pair<Int, Double>>
  result = getTicksInInterval(0, ticks, null)
  require("[(0, 1.0), (1, 2.0), (2, 3.0), (3, 4.0), (4, 5.5)]" == result.toString())
  result = getTicksInInterval(2, ticks, null)
  require("[(2, 3.0), (3, 4.0), (4, 5.5)]" == result.toString())
  result = getTicksInInterval(2, ticks, 1)
  require("[(2, 3.0), (3, 4.0)]" == result.toString())
}