@file:Suppress("unused", "unused_variable")

package de.moritzhank.cmftbl.smt.solver.experiments

import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.dsl.FormulaBuilder.Companion.formula
import de.moritzhank.cmftbl.smt.solver.dsl.formulaToLatex
import de.moritzhank.cmftbl.smt.solver.dsl.renderLatexFormula
import de.moritzhank.cmftbl.smt.solver.dsl.times
import de.moritzhank.cmftbl.smt.solver.misc.*
import de.moritzhank.cmftbl.smt.solver.translation.formula.genEval
import tools.aqua.stars.data.av.dataclasses.*

private val changedLaneAndHadSpeedBefore = formula { v: CCB<Vehicle> ->
  binding(term(v * Vehicle::lane)) { l ->
    until(Pair(1, 3)) {
      term(v * Vehicle::effVelocityInKmPH) gt const(0.0)
      term(v * Vehicle::lane * Lane::laneId) ne term(l * Lane::laneId)
    }
  }.apply { ccb.debugInfo = "l" }
}

private val changesLaneAndNoRollBefore = formula { v: CCB<Vehicle> ->
  until(Pair(1, 2)) {
    term(v * Vehicle::rotation * Rotation::roll) lt const(1.5)
    binding(term(v * Vehicle::lane)) { l ->
      next {
        (term(v * Vehicle::lane * Lane::laneId) ne term(l * Lane::laneId)) and
                (term(v * Vehicle::lane * Lane::road * Road::id) eq term(l * Lane::road * Road::id))
      }
    }.apply { ccb.debugInfo = "l" }
  }
}

fun main() {
  val ccb = CCB<Vehicle>().apply { debugInfo = "v" }

  renderLatexFormula(formulaToLatex(changedLaneAndHadSpeedBefore(CCB<Vehicle>().apply { debugInfo = "v" })))
  val graphViz = changedLaneAndHadSpeedBefore.genEval(emptyVehicle(id = 1), "v", arrayOf(1.0, 2.0, 3.0, 4.0, 5.5)).generateGraphvizCode()
  renderTree(graphViz)
}
