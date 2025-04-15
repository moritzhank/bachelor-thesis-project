package de.moritzhank.cmftbl.smt.solver.translation.formula

/** Generate distinct IDs. */
internal class EvaluationIDGenerator {

  private var nextID = 0

  /** Generate an unique ID. */
  fun generateID(): Int = nextID++

  /** Create an exact copy. */
  fun copy(): EvaluationIDGenerator {
    val copy = EvaluationIDGenerator()
    copy.nextID = nextID
    return copy
  }

}
