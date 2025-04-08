@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.*
import de.moritzhank.cmftbl.smt.solver.misc.ITreeVisualizationNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.generation.generateEvaluationForBinding
import de.moritzhank.cmftbl.smt.solver.translation.formula.generation.generateEvaluationForEvaluableRelation
import de.moritzhank.cmftbl.smt.solver.translation.formula.generation.generateEvaluationForLogicConnective
import de.moritzhank.cmftbl.smt.solver.translation.formula.generation.generateEvaluationForNextPrevious
import de.moritzhank.cmftbl.smt.solver.translation.formula.generation.generateEvaluationForUntilSince
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

inline fun <reified T: EntityType<*, *, *, *, *>, reified U: EntityType<*, *, *, *, *>>
        ((CallContextBase<T>, CallContextBase<U>) -> FormulaBuilder).generateVisualization(
  holdsFor1: T,
  holdsFor2: U,
  name1: String,
  name2: String,
  ticks: Array<Double>
) : ITreeVisualizationNode {
  return this.generateVisualization(holdsFor1, holdsFor2, T::class, U::class, name1, name2, ticks)
}

fun <T: EntityType<*, *, *, *, *>, U: EntityType<*, *, *, *, *>>
        ((CallContextBase<T>, CallContextBase<U>) -> FormulaBuilder).generateVisualization(
  holdsFor1: T,
  holdsFor2: U,
  kClass1: KClass<T>,
  kClass2: KClass<U>,
  name1: String,
  name2: String,
  ticks: Array<Double>
) : ITreeVisualizationNode {
  return this.generateEvaluation(holdsFor1, holdsFor2, kClass1, kClass2, name1, name2, ticks)
}
// endregion

/** Generates an [IEvalNode] for a unary formula. */
internal inline fun <reified T: EntityType<*, *, *, *, *>> ((CallContextBase<T>) -> FormulaBuilder).generateEvaluation(
  holdsFor: T,
  name: String,
  ticks: Array<Double>
) : IEvalNode {
  return this.generateEvaluation(holdsFor, T::class, name, ticks)
}

/** Generates an [IEvalNode] for a unary formula. */
internal fun <T: EntityType<*, *, *, *, *>> ((CallContextBase<T>) -> FormulaBuilder).generateEvaluation(
  holdsFor: T,
  kClass: KClass<T>,
  name: String,
  ticks: Array<Double>
) : IEvalNode {
  val evalCtx = EvaluationContext(EvaluationIDGenerator(), EvaluationIDGenerator(), mapOf(), mapOf(), mapOf())
  val ccb = CCB<T>(kClass).apply { debugInfo = name }
  val ccbSMTName = "inst${evalCtx.evaluationIDGenerator.generateID()}"
  val formula = this(ccb).getPhi().first()

  // Generate VarIntroNode
  val varIntroNode = VarIntroNode(mutableListOf(), evalCtx, ccbSMTName, ccb, holdsFor.id, 0, null, null, null)
  val newEvalCtx = evalCtx.copy(newIntroducedVariable = ccb to varIntroNode, newAssignedID = ccb to holdsFor.id)
  varIntroNode.children.add(generateEvaluation(formula, newEvalCtx, EvaluationType.EVALUATE, 0, null, null))
  eliminateUniversalQuantification(varIntroNode, ticks)
  return varIntroNode
}

/** Generates an [IEvalNode] for a binary formula. */
internal inline fun <reified T: EntityType<*, *, *, *, *>, reified U: EntityType<*, *, *, *, *>>
        ((CallContextBase<T>, CallContextBase<U>) -> FormulaBuilder).generateEvaluation(
  holdsFor1: T,
  holdsFor2: U,
  name1: String,
  name2: String,
  ticks: Array<Double>
) : IEvalNode {
  return this.generateEvaluation(holdsFor1, holdsFor2, T::class, U::class, name1, name2, ticks)
}

/** Generates an [IEvalNode] for a binary formula. */
internal fun <T: EntityType<*, *, *, *, *>, U: EntityType<*, *, *, *, *>>
        ((CallContextBase<T>, CallContextBase<U>) -> FormulaBuilder).generateEvaluation(
  holdsFor1: T,
  holdsFor2: U,
  kClass1: KClass<T>,
  kClass2: KClass<U>,
  name1: String,
  name2: String,
  ticks: Array<Double>
) : IEvalNode {
  val evalCtx = EvaluationContext(EvaluationIDGenerator(), EvaluationIDGenerator(), mapOf(), mapOf(), mapOf())
  val ccb1 = CCB<T>(kClass1).apply { debugInfo = name1 }
  val ccb2 = CCB<U>(kClass2).apply { debugInfo = name2 }
  val ccbSMTName1 = "inst${evalCtx.evaluationIDGenerator.generateID()}"
  val ccbSMTName2 = "inst${evalCtx.evaluationIDGenerator.generateID()}"
  val formula = this(ccb1, ccb2).getPhi().first()

  // Generate VarIntroNodes
  val varIntroNode1 = VarIntroNode(mutableListOf(), evalCtx, ccbSMTName1, ccb1, holdsFor1.id, 0, null, null, null)
  var newEvalCtx = evalCtx.copy(newIntroducedVariable = ccb1 to varIntroNode1, newAssignedID = ccb1 to holdsFor1.id)
  val varIntroNode2 = VarIntroNode(mutableListOf(), newEvalCtx, ccbSMTName2, ccb2, holdsFor2.id, 0, null, null,
    ccbSMTName1)
  newEvalCtx = newEvalCtx.copy(newIntroducedVariable = ccb2 to varIntroNode2, newAssignedID = ccb2 to holdsFor2.id)
  varIntroNode1.children.add(varIntroNode2)
  varIntroNode2.children.add(generateEvaluation(formula, newEvalCtx, EvaluationType.EVALUATE, 0, null, null))
  eliminateUniversalQuantification(varIntroNode1, ticks)
  return varIntroNode1
}



/** Generates an [IEvalNode] from a [Formula]. */
internal fun generateEvaluation(
  formula: Formula,
  evalCtx: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Double, Double>?,
  evalTickPrecondition: EvaluationTickPrecondition?
): IEvalNode {
  if (evalType == EvaluationType.UNIV_INST) {
    return UniversalEvalNode(evalCtx, formula, evalTickIndex, evalTickPrecondition, evalInterval!!)
  }
  return when (formula) {
    is EvaluableRelation<*> -> {
      generateEvaluationForEvaluableRelation(formula, evalCtx, evalType, evalTickIndex, evalInterval,
        evalTickPrecondition)
    }
    is Binding<*> -> {
      generateEvaluationForBinding(formula, evalCtx, evalType, evalTickIndex, evalInterval, evalTickPrecondition)
    }
    is LogicalConnectiveFormula  -> {
      generateEvaluationForLogicConnective(formula, evalCtx, evalType, evalTickIndex, evalInterval,
        evalTickPrecondition)
    }
    is NextPreviousFormula -> {
      generateEvaluationForNextPrevious(formula, evalCtx, evalType, evalTickIndex, evalInterval, evalTickPrecondition)
    }
    is UntilSinceFormula -> {
      generateEvaluationForUntilSince(formula, evalCtx, evalType, evalTickIndex, evalInterval, evalTickPrecondition)
    }
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
