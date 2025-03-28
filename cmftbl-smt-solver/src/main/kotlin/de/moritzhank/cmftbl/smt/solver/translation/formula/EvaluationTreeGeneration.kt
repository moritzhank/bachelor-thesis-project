@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.translation.formula

import tools.aqua.stars.core.types.EntityType
import de.moritzhank.cmftbl.smt.solver.dsl.*
import de.moritzhank.cmftbl.smt.solver.misc.ITreeVisualizationNode


// TODO: Remove
fun <T: EntityType<*, *, *, *, *>> ((CallContextBase<T>) -> FormulaBuilder).genEval(
  holdsFor: T,
  name: String,
  ticks: Array<Double>
) : ITreeVisualizationNode {
  return this.generateEvaluation(holdsFor, name, ticks)
}

/** Generates an [IEvalNode] for a formula. */
internal fun <T: EntityType<*, *, *, *, *>> ((CallContextBase<T>) -> FormulaBuilder).generateEvaluation(
  holdsFor: T,
  name: String,
  ticks: Array<Double>
) : IEvalNode {
  val evalCtx = EvaluationContext(EvaluationIDGenerator(), mapOf(), mapOf(), mapOf())
  val ccb = CCB<T>().apply { debugInfo = name }
  val ccbSMTName = "vinst_${evalCtx.evaluationIDGenerator.generateID()}"
  val formula = this(ccb).getPhi().first()

  // Generate VarIntroNode
  val varIntroNode = VarIntroNode(mutableListOf(), evalCtx, ccbSMTName, ccb, holdsFor.id, 0, null)
  val newEvalCtx = evalCtx.copy(newIntroducedVariable = ccb to varIntroNode, newAssignedID = ccb to holdsFor.id)
  varIntroNode.children.add(formula.generateEvaluation(newEvalCtx, EvaluationType.EVALUATE, 0, null, null))

  eliminateUniversalQuantification(varIntroNode, ticks)
  return varIntroNode
}

/** Generates an [IEvalNode] from a [Formula]. */
internal fun Formula.generateEvaluation(
  evalCtx: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int?,
  evalInterval: Pair<Int, Int>?,
  evalTickPrecond: EvaluationTickPrecondition?
): IEvalNode {
  return when (this) {
    is EvaluableRelation<*> -> {
      generateEvaluationForEvaluableRelation(evalCtx, evalType, evalTickIndex, evalInterval, evalTickPrecond)
    }
    is Until -> {
      generateEvaluationForUntil(this, evalCtx, evalType, evalTickIndex, evalInterval, evalTickPrecond)
    }
    is Binding<*> -> {
      generateEvaluationForBinding(this, evalCtx, evalType, evalTickIndex, evalInterval, evalTickPrecond)
    }
    else -> error("The generation is not yet available for the formula type \"${this::class.simpleName}\".")
  }
}

/** Generate an [IEvalNode] from a [Until]. */
private fun generateEvaluationForUntil(
  until: Until,
  evalCtx: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int?,
  evalInterval: Pair<Int, Int>?,
  evalTickPrecond: EvaluationTickPrecondition?
): IEvalNode {
  return when (evalType) {
    EvaluationType.EVALUATE -> {
      require(evalTickPrecond == null) { "The generation of until with present tick precondition is not available yet." }

      // Prepare result node
      val resultNode = EvalNode(mutableListOf(), evalCtx, mutableListOf(), until, evalTickIndex!!, evalTickPrecond)
      val usedUnboundVarsRhs = getUsedUnboundVariables(until.rhs, evalCtx)
      val twtns = if (usedUnboundVarsRhs.isEmpty()) null else "twtns_${evalCtx.evaluationIDGenerator.generateID()}"
      if(twtns != null) {
        resultNode.emissions.add(DecConstEmission(twtns))
      }

      // Evaluate lhs
      val tickPreconditionLhs = if(twtns == null) null else EvaluationTickPrecondition(twtns, Relation.Lt)
      val lhs = until.lhs.generateEvaluation(evalCtx, EvaluationType.UNIV_INST, evalTickIndex, until.interval,
        tickPreconditionLhs)
      resultNode.children.add(lhs)

      // Generate VarIntroNodes for rhs
      val varIntroNodes = mutableListOf<VarIntroNode>()
      var lastEvalCtx = evalCtx
      usedUnboundVarsRhs.forEach {
        val newVarName = "vinst_${lastEvalCtx.evaluationIDGenerator.generateID()}"
        val varID = lastEvalCtx.previouslyAssignedIDs[it]!!
        val newVarIntroNode = VarIntroNode(mutableListOf(), lastEvalCtx, newVarName, it, varID, null, until.interval)
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
      val rhs = until.rhs.generateEvaluation(lastEvalCtx, EvaluationType.WITNESS, evalTickIndex, until.interval, null)
      lastNode.children.add(rhs)
      if (twtns != null) {
        // Add [TickWitnessTimeEmission] to right child node of until
        resultNode.children[1].emissions.add(TickWitnessTimeEmission(varIntroNodes.first().emittedID, twtns))
      }

      resultNode
    }
    else -> error("Nested evaluations with Until are not supported yet.")
  }
}

/** Generate an [IEvalNode] from a [Binding]. */
private fun generateEvaluationForBinding(
  binding: Binding<*>,
  evalCtx: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int?,
  evalInterval: Pair<Int, Int>?,
  evalTickPrecond: EvaluationTickPrecondition?
): IEvalNode {
  return when (evalType) {
    EvaluationType.EVALUATE -> {
      val boundVarID = "bnd_${evalCtx.evaluationIDGenerator.generateID()}"
      val evalTerm = binding.bindTerm.generateEvaluation(evalCtx, evalType, evalTickIndex, null) as EvalNode
      val newEvalCtx = evalCtx.copy(newBoundCallContext = binding.ccb to boundVarID)
      val evalNode = binding.inner.generateEvaluation(newEvalCtx, evalType, evalTickIndex, null, evalTickPrecond)
      val emissions = mutableListOf<IEmission>()
      emissions.add(DecConstEmission(boundVarID))
      emissions.add(BindingTermFromChildEmission(boundVarID, evalTerm))
      EvalNode(mutableListOf(evalTerm, evalNode), evalCtx, emissions, binding, evalTickIndex!!, evalTickPrecond)
    }
    else -> error("Evaluating a binding in anything other than EVALUATE mode is not yet supported.")
  }
}

/** Generate an [IEvalNode] from a [EvaluableRelation]. */
private fun EvaluableRelation<*>.generateEvaluationForEvaluableRelation(
  evalContext: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int?,
  evalInterval: Pair<Int, Int>?,
  evalTickPrecond: EvaluationTickPrecondition?
): IEvalNode {
  return when (evalType) {
    EvaluationType.EVALUATE -> {
      val lhs = this.lhs.generateEvaluation(evalContext, evalType, evalTickIndex, evalInterval) as EvalNode
      val rhs = this.rhs.generateEvaluation(evalContext, evalType, evalTickIndex, evalInterval) as EvalNode
      val emissions = mutableListOf<IEmission>(TermFromChildrenConstraintEmission(type, lhs, rhs))
      EvalNode(mutableListOf(lhs, rhs), evalContext, emissions, this, evalTickIndex!!, evalTickPrecond)
    }
    EvaluationType.WITNESS -> {
      val lhs = this.lhs.generateEvaluation(evalContext, evalType, evalTickIndex, evalInterval) as WitnessEvalNode
      val rhs = this.rhs.generateEvaluation(evalContext, evalType, evalTickIndex, evalInterval) as WitnessEvalNode
      val emissions = mutableListOf<IEmission>(TermFromChildrenConstraintEmission(type, lhs, rhs))
      WitnessEvalNode(mutableListOf(lhs, rhs), evalContext, emissions, this, evalInterval, evalTickPrecond)
    }
    EvaluationType.UNIV_INST -> {
      UniversalEvalNode(evalContext, this, evalTickIndex!!, evalTickPrecond, evalInterval?.second)
    }
  }
}

/** Generate an [IEvalNode] from a [Term]. */
private fun <T> Term<T>.generateEvaluation(
  evalContext: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int?,
  evalInterval: Pair<Int, Int>?,
): IEvalNode {
  val termStr = this.str((this as? Variable<*>)?.let { evalContext.getSmtID(it.callContext.base()) })
  return when(evalType) {
    EvaluationType.EVALUATE -> {
      EvalNode(mutableListOf(), evalContext, mutableListOf(), this, evalTickIndex!!, null, termStr)
    }
    EvaluationType.WITNESS -> {
      WitnessEvalNode(mutableListOf(), evalContext, mutableListOf(), this, evalInterval, null, termStr)
    }
    EvaluationType.UNIV_INST -> error("This path should not be reached.")
  }
}

/** Captures all used [CCB]s that are not bound. */
internal fun getUsedUnboundVariables(
  evaluable: Evaluable,
  lastAvailableEvalCtx: EvaluationContext,
  newlyBoundCCBs: MutableSet<CCB<*>> = mutableSetOf()
): Set<CCB<*>> {
  val result: MutableSet<CCB<*>> = mutableSetOf()
  when (evaluable) {
    is Variable<*> -> {
      val ccb = evaluable.callContext.base()
      if (!lastAvailableEvalCtx.previouslyBoundCallContexts.containsKey(ccb) && !newlyBoundCCBs.contains(ccb)) {
        result.add(ccb)
      }
    }
    is Constant<*> -> Unit
    is Binding<*> -> {
      newlyBoundCCBs.add(evaluable.ccb)
      result.addAll(getUsedUnboundVariables(evaluable.bindTerm, lastAvailableEvalCtx, newlyBoundCCBs))
      result.addAll(getUsedUnboundVariables(evaluable.inner, lastAvailableEvalCtx, newlyBoundCCBs))
    }
    // New time contexts are not considered
    is Until, is Since -> Unit
    is Formula -> {
      evaluable.childNodes().forEach {
        result.addAll(getUsedUnboundVariables(it, lastAvailableEvalCtx, newlyBoundCCBs))
      }
    }
  }
  return result
}
