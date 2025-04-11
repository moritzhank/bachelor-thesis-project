package de.moritzhank.cmftbl.smt.solver.translation.formula.generation

import de.moritzhank.cmftbl.smt.solver.dsl.Term
import de.moritzhank.cmftbl.smt.solver.dsl.Variable
import de.moritzhank.cmftbl.smt.solver.dsl.str
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationContext
import de.moritzhank.cmftbl.smt.solver.translation.formula.EvaluationType
import de.moritzhank.cmftbl.smt.solver.translation.formula.IEvalNode
import de.moritzhank.cmftbl.smt.solver.translation.formula.WitnessEvalNode

/** Generate an [IEvalNode] from a [Term]. */
internal fun <T> generateEvaluation(
  term: Term<T>,
  evalContext: EvaluationContext,
  evalType: EvaluationType,
  evalTickIndex: Int,
  evalInterval: Pair<Double, Double>?,
): IEvalNode {
  val termStr = term.str((term as? Variable<*>)?.let { evalContext.getSmtID(it.callContext.base()) })
  return if (evalType == EvaluationType.EVALUATE) {
    EvalNode(mutableListOf(), evalContext, mutableListOf(), term, evalTickIndex, termStr)
  } else {
    require(evalType == EvaluationType.WITNESS)
    WitnessEvalNode(mutableListOf(), evalContext, mutableListOf(), term, evalInterval!!, termStr)
  }
}
