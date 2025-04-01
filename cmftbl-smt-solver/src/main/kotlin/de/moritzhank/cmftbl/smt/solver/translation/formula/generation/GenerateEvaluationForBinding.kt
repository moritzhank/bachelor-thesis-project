package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.Binding
import de.moritzhank.cmftbl.smt.solver.translation.formula.BindingTermFromChildEmission
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationContext
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationTickPrecondition
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationType
import de.moritzhank.cmftbl.smt.solver.translation.formula.IEmission
import de.moritzhank.cmftbl.smt.solver.translation.formula.IEvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.IEvalNodeWithEvaluable
import de.moritzhank.cmftbl.smt.solver.translation.formula.NewInstanceEmission
import de.moritzhank.cmftbl.smt.solver.translation.formula.SubFormulaeHoldEmission
import de.moritzhank.cmftbl.smt.solver.translation.formula.WitnessEvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.generateEvaluation

/** Generate an [IEvalNode] from a [Binding]. */
internal fun generateEvaluationForBinding(
  binding: Binding<*>,
  evalCtx: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Int, Int>?,
  evalTickPrecondition: EvaluationTickPrecondition?,
  formulaHoldsVariable: String
): IEvalNode {
  val boundVarID = "boundVariable_${evalCtx.evaluationIDGenerator.generateID()}"
  val evalTerm = generateEvaluation(binding.bindTerm, evalCtx, evalType, evalTickIndex, null) as IEvalNodeWithEvaluable
  val newEvalCtx = evalCtx.copy(newBoundCallContext = binding.ccb to boundVarID)
  val subFormulaHolds1 = "subFormulaHolds_${evalCtx.evaluationIDGenerator.generateID()}"
  val subFormulaHolds2 = "subFormulaHolds_${evalCtx.evaluationIDGenerator.generateID()}"
  val evalNode = generateEvaluation(binding.inner, newEvalCtx, evalType, evalTickIndex, null, evalTickPrecondition,
    subFormulaHolds2)
  val emissions = mutableListOf<IEmission>()
  emissions.add(NewInstanceEmission(subFormulaHolds1, true))
  emissions.add(NewInstanceEmission(subFormulaHolds2, true))
  emissions.add(NewInstanceEmission(boundVarID))
  emissions.add(BindingTermFromChildEmission(boundVarID, evalTerm, subFormulaHolds1))
  emissions.add(SubFormulaeHoldEmission(listOf(subFormulaHolds1, subFormulaHolds2), formulaHoldsVariable))
  return when (evalType) {
    EvaluationType.EVALUATE -> {
      EvalNode(mutableListOf(evalTerm, evalNode), evalCtx, emissions, binding, evalTickIndex, evalTickPrecondition)
    }
    EvaluationType.WITNESS -> {
      WitnessEvalNode(mutableListOf(evalTerm, evalNode), evalCtx, emissions, binding, evalInterval, evalTickPrecondition)
    }
    else -> error("Evaluating a binding in anything other than EVALUATE and WITNESS mode is not yet supported.")
  }
}
