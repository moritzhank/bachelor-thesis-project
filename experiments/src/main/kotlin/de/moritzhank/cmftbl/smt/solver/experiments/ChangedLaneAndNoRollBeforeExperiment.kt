@file:Suppress("unused", "unused_variable")

package de.moritzhank.cmftbl.smt.solver.experiments

import de.moritzhank.cmftbl.smt.solver.ExperimentLoader
import de.moritzhank.cmftbl.smt.solver.SmtSolver
import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.dsl.FormulaBuilder.Companion.formula
import de.moritzhank.cmftbl.smt.solver.dsl.formulaToLatex
import de.moritzhank.cmftbl.smt.solver.dsl.renderLatexFormula
import de.moritzhank.cmftbl.smt.solver.dsl.times
import de.moritzhank.cmftbl.smt.solver.generateSmtLibForSegment
import de.moritzhank.cmftbl.smt.solver.misc.*
import de.moritzhank.cmftbl.smt.solver.runSmtSolver
import de.moritzhank.cmftbl.smt.solver.translation.formula.genEval
import de.moritzhank.cmftbl.smt.solver.translation.formula.generateSmtLib
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
  val ccb = CCB<Vehicle>(Vehicle::class).apply { debugInfo = "v" }

  //Viewing Town 10HD, Seed 3, Segment 1, Vehicle 49
  val seg: Segment = ExperimentLoader.loadTestSegments("10HD", "3")[1]
  val vehicleID = 49
  val ticks = seg.tickData.map { it.currentTick.tickSeconds }.toTypedArray()

  renderLatexFormula(formulaToLatex(changedLaneAndHadSpeedBefore(CCB<Vehicle>(Vehicle::class).apply { debugInfo = "v" })))
  val graphViz = changedLaneAndHadSpeedBefore.genEval(emptyVehicle(id = vehicleID), "v", ticks).generateGraphvizCode()
  renderTree(graphViz)

  val dataSmtLib = generateSmtLibForSegment(seg, SmtSolver.YICES, "QF_LIRA")
  val formulaSmtLib = generateSmtLib(changedLaneAndHadSpeedBefore, emptyVehicle(id = vehicleID), "v", ticks)

  println(runSmtSolver("$dataSmtLib$formulaSmtLib(check-sat)", SmtSolver.YICES, false))
}
