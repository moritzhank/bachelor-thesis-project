package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.EvaluableRelation
import de.moritzhank.cmftbl.smt.solver.dsl.LogicalConnectiveFormula
import de.moritzhank.cmftbl.smt.solver.dsl.Neg
import de.moritzhank.cmftbl.smt.solver.misc.lhs
import de.moritzhank.cmftbl.smt.solver.misc.rhs
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationContext
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationTickPrecondition
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationType
import de.moritzhank.cmftbl.smt.solver.translation.formula.FormulaFromChildrenEmission
import de.moritzhank.cmftbl.smt.solver.translation.formula.IEmission
import de.moritzhank.cmftbl.smt.solver.translation.formula.IEvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.IEvalNodeWithEvaluable
import de.moritzhank.cmftbl.smt.solver.translation.formula.NewInstanceEmission
import de.moritzhank.cmftbl.smt.solver.translation.formula.UniversalEvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.WitnessEvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.generateEvaluation


/** Generate an [IEvalNode] from a [EvaluableRelation]. */
internal fun generateEvaluationForLogicConnective(
  formula: LogicalConnectiveFormula,
  evalContext: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Int, Int>?,
  evalTickPrecond: EvaluationTickPrecondition?,
  subFormulaHoldsVariable: String
): IEvalNode {
  if (evalType == EvaluationType.UNIV_INST) {
    return UniversalEvalNode(evalContext, formula, evalTickIndex, evalTickPrecond, evalInterval?.second)
  }
  if (formula is Neg) {
    error("Evaluating Neg is not available yet.")
  }
  val emissions = mutableListOf<IEmission>()
  val subFormulaHoldsLhs = "subFormulaHolds_${evalContext.evaluationIDGenerator.generateID()}"
  val subFormulaHoldsRhs = "subFormulaHolds_${evalContext.evaluationIDGenerator.generateID()}"
  emissions.add(NewInstanceEmission(subFormulaHoldsLhs, true))
  emissions.add(NewInstanceEmission(subFormulaHoldsRhs, true))
  val lhs = generateEvaluation(formula.lhs(), evalContext, evalType, evalTickIndex, evalInterval, evalTickPrecond,
    subFormulaHoldsLhs)
  val rhs = generateEvaluation(formula.rhs(), evalContext, evalType, evalTickIndex, evalInterval, evalTickPrecond,
    subFormulaHoldsRhs)
  val emission = FormulaFromChildrenEmission(formula, lhs as IEvalNodeWithEvaluable, rhs as IEvalNodeWithEvaluable,
    subFormulaHoldsVariable, subFormulaHoldsLhs, subFormulaHoldsRhs)
  emissions.add(emission)
  return if (evalType == EvaluationType.EVALUATE) {
    EvalNode(mutableListOf(lhs, rhs), evalContext, emissions, formula, evalTickIndex, evalTickPrecond)
  } else {
    require(evalType == EvaluationType.WITNESS)
    WitnessEvalNode(mutableListOf(lhs, rhs), evalContext, emissions, formula, evalInterval, evalTickPrecond)
  }
}
