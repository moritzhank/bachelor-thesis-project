package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.CCB

/** Contains information that are needed during the translation process of [IEvalNode]s. */
internal class EvaluationContext(
  /** This ID generator is used for introduced variables closely related to the formula. */
  val evaluationIDGenerator: EvaluationIDGenerator,
  /** This ID generator is used for IDs of emissions and nodes. */
  val constraintIDGenerator: EvaluationIDGenerator,
  val previouslyBoundCallContexts: Map<CCB<*>, String>,
  val previouslyIntroducedVariables: Map<CCB<*>, VarIntroNode>,
  val previouslyAssignedIDs: Map<CCB<*>, Int>
) {

  fun copy(
    newBoundCallContext: Pair<CCB<*>, String>? = null,
    newIntroducedVariable: Pair<CCB<*>, VarIntroNode>? = null,
    newAssignedID: Pair<CCB<*>, Int>? = null
  ): EvaluationContext {
    val copiedPreviouslyBoundCallContexts = if (newBoundCallContext != null) {
      previouslyBoundCallContexts + newBoundCallContext
    } else {
      previouslyBoundCallContexts
    }
    val copiedPreviouslyIntroducedVariables = if (newIntroducedVariable != null) {
      previouslyIntroducedVariables + newIntroducedVariable
    } else {
      previouslyIntroducedVariables
    }
    val copiedPreviouslyAssignedIDs = if (newAssignedID != null) {
      previouslyAssignedIDs + newAssignedID
    } else {
      previouslyAssignedIDs
    }
    return EvaluationContext(
      evaluationIDGenerator,
      constraintIDGenerator,
      copiedPreviouslyBoundCallContexts,
      copiedPreviouslyIntroducedVariables,
      copiedPreviouslyAssignedIDs
    )
  }

  /**
   * If the [CCB] is registered as bound and as a new introduced variable, the new introduced variable has precedence.
   */
  fun getSmtID(ccb: CCB<*>): String? =
    previouslyIntroducedVariables[ccb]?.emittedID ?: previouslyBoundCallContexts[ccb]

}