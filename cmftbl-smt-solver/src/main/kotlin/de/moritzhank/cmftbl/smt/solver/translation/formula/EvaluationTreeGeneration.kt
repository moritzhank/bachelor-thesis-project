@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.*
import de.moritzhank.cmftbl.smt.solver.misc.ITreeVisualizationNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.generation.generateEvaluationForBinding
import de.moritzhank.cmftbl.smt.solver.translation.formula.generation.generateEvaluationForEvaluableRelation
import de.moritzhank.cmftbl.smt.solver.translation.formula.generation.generateEvaluationForLogicConnective
import tools.aqua.stars.core.types.EntityType
import kotlin.reflect.KClass

// region Visualization
inline fun <reified T: EntityType<*, *, *, *, *>> ((CallContextBase<T>) -> FormulaBuilder).generateVisualization(
  holdsFor: T,
  name: String,
  ticks: Array<Double>
) : ITreeVisualizationNode {
  return this.generateVisualization(holdsFor, T::class, name, ticks)
}

fun <T: EntityType<*, *, *, *, *>> ((CallContextBase<T>) -> FormulaBuilder).generateVisualization(
  holdsFor: T,
  kClass: KClass<T>,
  name: String,
  ticks: Array<Double>
) : ITreeVisualizationNode {
  return this.generateEvaluation(holdsFor, kClass, name, ticks)
}
// endregion

/** Generates an [IEvalNode] for a formula. */
internal inline fun <reified T: EntityType<*, *, *, *, *>> ((CallContextBase<T>) -> FormulaBuilder).generateEvaluation(
  holdsFor: T,
  name: String,
  ticks: Array<Double>
) : IEvalNode {
  return this.generateEvaluation(holdsFor, T::class, name, ticks)
}

/** Generates an [IEvalNode] for a formula. */
internal fun <T: EntityType<*, *, *, *, *>> ((CallContextBase<T>) -> FormulaBuilder).generateEvaluation(
  holdsFor: T,
  kClass: KClass<T>,
  name: String,
  ticks: Array<Double>
) : IEvalNode {
  val evalCtx = EvaluationContext(EvaluationIDGenerator(), mapOf(), mapOf(), mapOf())
  val ccb = CCB<T>(kClass).apply { debugInfo = name }
  val ccbSMTName = "variable_${evalCtx.evaluationIDGenerator.generateID()}"
  val formula = this(ccb).getPhi().first()

  // Generate VarIntroNode
  val varIntroNode = VarIntroNode(mutableListOf(), evalCtx, ccbSMTName, ccb, holdsFor.id, 0, null, "")
  val newEvalCtx = evalCtx.copy(newIntroducedVariable = ccb to varIntroNode, newAssignedID = ccb to holdsFor.id)
  val subFormulaHolds = "subFormulaHolds_${evalCtx.evaluationIDGenerator.generateID()}"
  varIntroNode.children.add(generateEvaluation(formula, newEvalCtx, EvaluationType.EVALUATE, 0, null, null, subFormulaHolds))
  varIntroNode.emissions.add(NewInstanceEmission(subFormulaHolds, true))

  eliminateUniversalQuantification(varIntroNode, ticks)
  return varIntroNode
}

/** Generates an [IEvalNode] from a [Formula]. */
internal fun generateEvaluation(
  formula: Formula,
  evalCtx: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Int, Int>?,
  evalTickPrecondition: EvaluationTickPrecondition?,
  subFormulaHoldsVariable: String
): IEvalNode {
  return when (formula) {
    is EvaluableRelation<*> -> {
      generateEvaluationForEvaluableRelation(formula, evalCtx, evalType, evalTickIndex, evalInterval,
        evalTickPrecondition, subFormulaHoldsVariable)
    }
    is Binding<*> -> {
      generateEvaluationForBinding(formula, evalCtx, evalType, evalTickIndex, evalInterval, evalTickPrecondition,
        subFormulaHoldsVariable)
    }
    is LogicalConnectiveFormula  -> {
      generateEvaluationForLogicConnective(formula, evalCtx, evalType, evalTickIndex, evalInterval,
        evalTickPrecondition, subFormulaHoldsVariable)
    }
    /*
    is Until -> {
      generateEvaluationForUntil(formula, evalCtx, evalType, evalTickIndex, evalInterval, evalTickPrecondition,
        subFormulaHoldsVariable)
    }
     */
    /*
    is Next -> {
      // TODO
      this.inner.generateEvaluation(evalCtx, evalType, evalTickIndex, evalInterval, evalTickPrecondition)
    }
     */
    else -> error("The generation is not yet available for the formula type \"${formula::class.simpleName}\".")
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
