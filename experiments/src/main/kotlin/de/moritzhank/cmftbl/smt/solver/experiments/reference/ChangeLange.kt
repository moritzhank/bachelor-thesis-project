package de.moritzhank.cmftbl.smt.solver.experiments.reference

import de.moritzhank.cmftbl.smt.solver.ExperimentLoader
import tools.aqua.stars.core.evaluation.PredicateContext

fun main() {
  val segs = ExperimentLoader.loadTestSegments("10HD", "1")
  val holds = segs.indexOfFirst { changedLane.holds(PredicateContext(it)) }
  println(segs[holds].ticks.keys.first())
}
