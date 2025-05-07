@file:Suppress("unused", "unused_variable")

package de.moritzhank.cmftbl.smt.solver.experiments

import de.moritzhank.cmftbl.smt.solver.ExperimentLoader
import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.dsl.FormulaBuilder.Companion.formula
import de.moritzhank.cmftbl.smt.solver.dsl.formulaToLatex
import de.moritzhank.cmftbl.smt.solver.dsl.renderLatexFormula
import de.moritzhank.cmftbl.smt.solver.dsl.times
import de.moritzhank.cmftbl.smt.solver.misc.emptyVehicle
import de.moritzhank.cmftbl.smt.solver.misc.generateGraphvizCode
import de.moritzhank.cmftbl.smt.solver.misc.renderTree
import de.moritzhank.cmftbl.smt.solver.translation.formula.generateVisualization
import tools.aqua.stars.data.av.dataclasses.*

private val changedLaneAndNoRollBefore = formula { v: CCB<Vehicle> ->
  binding(term(v * Vehicle::lane)) { l ->
    until(Pair(1, 2)) {
      term(v * Vehicle::rotation * Rotation::roll) lt const(1.5)
      (term(v * Vehicle::lane * Lane::laneId) ne term(l * Lane::laneId)) and
              (term(v * Vehicle::lane * Lane::road * Road::id) eq term(l * Lane::road * Road::id))
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

private val tmp = formula { v: CCB<Vehicle> ->
  binding(term(v * Vehicle::lane)) { l ->
    eventually {
      (term(v * Vehicle::lane * Lane::laneId) ne term(l * Lane::laneId)) and
              (term(v * Vehicle::lane * Lane::road * Road::id) eq term(l * Lane::road * Road::id))
    }
  }.apply { ccb.debugInfo = "l" }
}

fun main() {
  val ccb = CCB<Vehicle>(Vehicle::class).apply { debugInfo = "v" }

  println(formulaToLatex(tmp(ccb)))
  return

  //Viewing Town 10HD, Seed 3, Segment 10, Vehicle 126
  val seg: Segment = ExperimentLoader.loadTestSegments("10HD", "3")[10]
  val vehicleID1 = 126
  val ticks = seg.tickData.map { it.currentTick.tickSeconds }.toTypedArray()

  val formula = changedLaneAndNoRollBefore(CCB<Vehicle>(Vehicle::class).apply { debugInfo = "v1" })
  renderLatexFormula(formulaToLatex(formula))
  println("Rendered latex formula.")

  val visualization = changedLaneAndNoRollBefore.generateVisualization(emptyVehicle(id = vehicleID1), "v", ticks)
  val graphViz = visualization.generateGraphvizCode()
  renderTree(graphViz)
  println("Rendered graph.")

  //val dataSmtLib = generateSmtLibForSegment(seg, SmtSolver.Z3, "QF_LIRA")
  //val formulaSmtLib = generateSmtLib(visualization)

  //println(runSmtSolver("$dataSmtLib$formulaSmtLib", SmtSolver.Z3, false, yicesTimeoutInSeconds = 999999999))

}
