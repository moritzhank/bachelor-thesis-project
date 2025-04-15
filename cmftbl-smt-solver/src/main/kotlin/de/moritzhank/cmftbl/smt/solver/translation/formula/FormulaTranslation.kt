package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.*
import de.moritzhank.cmftbl.smt.solver.misc.*

fun generateSmtLib(evalNode: ITreeVisualizationNode): String {
  require(evalNode is IEvalNode)
  val evalNodeID = evalNode.nodeID
  require(evalNodeID != null)
  val result = StringBuilder()
  val tickDataSeconds = { tickID: String -> "(tickDataUnitSeconds_tickSeconds (tickData_currentTick $tickID))" }
  val holdVar = { id: Int -> "holds$id" }
  val nodeIDDeclared = mutableSetOf<Int>()
  for (node in evalNode.iterator()) {
    if (node.nodeID != null) {
      result.appendLine()
      result.appendLine("; Translate node ${node.nodeID}")
    }
    // Declare holdVars
    val nodeID = node.nodeID
    val emissionHoldIDs = node.emissions.mapNotNull { it.emissionID }
    if (nodeID != null) {
      val declareConsts = emissionHoldIDs.toMutableList()
      if (!nodeIDDeclared.contains(nodeID)) {
        declareConsts.add(0, nodeID)
        nodeIDDeclared.add(nodeID)
      }
      val childNodeIDsNotDeclared = node.children.mapNotNull { it.nodeID }.filter { !nodeIDDeclared.contains(it) }
      declareConsts.addAll(childNodeIDsNotDeclared)
      nodeIDDeclared.addAll(childNodeIDsNotDeclared)
      for (holdVarID in declareConsts) {
        result.appendLine("(declare-const ${holdVar(holdVarID)} Bool)")
      }
    }

    // Generate SMT commands for emissions
    for (emission in node.emissions) {
      when (emission) {
        is NewInstanceEmission -> {
          val sort = if (emission.isBool) "Bool" else "Int"
          result.appendLine("(declare-const ${emission.newInstanceID} $sort)")
        }
        is BindingTermFromChildEmission -> {
          val term = emission.term
          val smtTerm = termToSmtRepresentation(term) { v ->
            node.evalCtx.getSmtID(v.callContext.base())!!
          }
          val emitSmtID = holdVar(emission.emissionID!!)
          result.appendLine("(assert (= $emitSmtID (= ${emission.variableID} $smtTerm)))")
        }
        is EvalAtTickConstraintEmission -> {
          require(node is VarIntroNode)
          val ccb = node.referenceCCB
          val tickIndex = emission.tickIndex
          val varID = emission.variableID
          val emitSmtID = holdVar(emission.emissionID!!)
          result.appendLine("(assert (= $emitSmtID (= (${ccb.tickMemberName()} $varID) (indexToTick $tickIndex))))")
        }
        is EvalInIntervalConstraintEmission -> {
          require(node is VarIntroNode)
          val varID = emission.variableID
          // tickSeconds(varID)
          val tickSeconds = tickDataSeconds("(${node.referenceCCB.tickMemberName()} $varID)")
          // tickSeconds(currentEvaluatedIndex)
          val evaluatedTickIndexSeconds = tickDataSeconds("(indexToTick ${node.evaluatedTickIndex})")
          val emitSmtID = holdVar(emission.emissionID!!)
          val assertion = generateInIntervalSmtString(evaluatedTickIndexSeconds, emission.interval, tickSeconds)
          result.appendLine("(assert (= $emitSmtID $assertion))")
        }
        is ConstrainIDEmission -> {
          require(node is VarIntroNode)
          val idOfVar = "(${node.referenceCCB.idMemberName()} ${emission.constraintVariableID})"
          val emitSmtID = holdVar(emission.emissionID!!)
          result.appendLine("(assert (= $emitSmtID (= $idOfVar ${emission.id})))")
        }
        is TickWitnessTimeEmission -> {
          require(node is VarIntroNode)
          val emitSmtID = holdVar(emission.emissionID!!)
          result.appendLine("(assert (= $emitSmtID (= (${node.referenceCCB.tickMemberName()} ${emission.witnessID}) ${emission.tickWitnessID})))")
        }
        is TermFromChildrenEmission -> {
          require(node is IEvalNodeWithEvaluable)
          val smtTerm1 = termToSmtRepresentation(emission.term1) { v ->
            node.evalCtx.getSmtID(v.callContext.base())!!
          }
          val smtTerm2 = termToSmtRepresentation(emission.term2) { v ->
            node.evalCtx.getSmtID(v.callContext.base())!!
          }
          val emitSmtID = holdVar(emission.emissionID!!)
          result.appendLine("(assert (= $emitSmtID (${emission.operator.toSMTString()} $smtTerm1 $smtTerm2)))")
        }
        is SameTimeEmission -> {
          require(node is VarIntroNode)
          val nodeElemTime = tickDataSeconds("(${node.referenceCCB.tickMemberName()} ${node.emittedID})")
          val refElemTime = tickDataSeconds("(${emission.referenceCCB.tickMemberName()} ${emission.referenceID})")
          val emitSmtID = holdVar(emission.emissionID!!)
          result.appendLine("(assert (= $emitSmtID (= $nodeElemTime $refElemTime)))")
        }
        is TickIndexExistsInIntervalEmission -> {
          // TODO: CHECK
          val tickIndex = "(indexToTick ${emission.tickIndex})"
          val firstPart = "(distinct -1 $tickIndex)"
          val currentTickTime = tickDataSeconds("(indexToTick ${emission.referenceTickIndex}))")
          val tickTime = tickDataSeconds(tickIndex)
          val secondPart = generateInIntervalSmtString(currentTickTime, emission.interval, tickTime)
          val emitSmtID = holdVar(emission.emissionID!!)
          result.appendLine("(assert (= $emitSmtID (and $firstPart $secondPart)))")
        }
        is FormulaeFromChildrenEmission -> {
          // TODO: CHECK
          if (node.children.size == 2) {
            val emitSmtID = holdVar(emission.emissionID!!)
            val opStr = logicalConnectiveSmtOperator(emission.formula)
            val firstPart = holdVar(emission.evalNode1.nodeID!!)
            val secondPart = holdVar(emission.evalNode2.nodeID!!)
            result.appendLine("(assert (= $emitSmtID ($opStr $firstPart $secondPart)))")
          }
        }
      }
    }

    // Assert value of holdVar(nodeID)
    val holdVars = mutableListOf<String>()
    if (nodeID != null) {
      holdVars.addAll(emissionHoldIDs.map { holdID -> holdVar(holdID) })
      if (!node.childSatNotRequired) {
        holdVars.addAll(node.children.mapNotNull { it.nodeID }.map { holdVar(it) })
      }
      val holdVarStr = holdVar(nodeID)
      if (node is VarIntroNode && node.tickPrecondition != null) {
        val precond = node.tickPrecondition
        val refStr = tickDataSeconds("(${node.referenceCCB.tickMemberName()} ${node.emittedID})")
        val implicantStr = "(${precond.operation.toSMTString()} $refStr ${tickDataSeconds(precond.tickWitnessTimeID)})"
        result.appendLine("(assert (= $holdVarStr (=> $implicantStr ${generateAndStructure(holdVars)})))")
      } else {
        // If needed so that slicing works (node can have a nodeID but emits nothing in this case)
        if (holdVars.isNotEmpty()) {
          result.appendLine("(assert (= $holdVarStr ${generateAndStructure(holdVars)}))")
        }
      }
    }
  }
  result.appendLine("(assert ${holdVar(evalNodeID)})")
  result.appendLine("(check-sat)")
  return result.toString()
}

private fun termToSmtRepresentation(term: Term<*>, baseElem: (Variable<*>) -> String): String {
  return when (term) {
    is Constant<*> -> term.value.toString()
    is Variable<*> -> {
      term.callContext.toSmtRepresentation(baseElem(term))
    }
  }
}

private fun generateInIntervalSmtString(
  currentTickTime: String,
  interval: Pair<Double, Double>,
  timeInQuestion: String
): String {
  if (interval.isMirrored()) {
    val intervalRight = "(- $currentTickTime ${-interval.second})"
    var assertion = "(<= $timeInQuestion $intervalRight)"
    if (interval.first != Double.NEGATIVE_INFINITY) {
      val intervalLeft = "(- $currentTickTime ${-interval.first})"
      assertion = "(and $assertion (>= $timeInQuestion $intervalLeft))"
    }
    return assertion
  } else {
    val intervalLeft = "(+ ${interval.first} $currentTickTime)"
    var assertion = "(>= $timeInQuestion $intervalLeft)"
    if (interval.second != Double.POSITIVE_INFINITY) {
      val intervalRight = "(+ ${interval.second} $currentTickTime)"
      assertion = "(and $assertion (<= $timeInQuestion $intervalRight))"
    }
    return assertion
  }

}

private fun logicalConnectiveSmtOperator(formula: LogicalConnectiveFormula): String {
  return when (formula) {
    is And -> "and"
    is Iff -> "="
    is Implication -> "=>"
    is Neg -> "not"
    is Or -> "or"
  }
}
