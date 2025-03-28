package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.Relation

internal class EvaluationTickPrecondition(val tickWitnessTimeID: String, val operation: Relation) {

  override fun toString(): String {
    return "$operation $tickWitnessTimeID"
  }

  fun toHTMLString(): String {
    return "${operation.toHTMLString()} $tickWitnessTimeID"
  }

}
