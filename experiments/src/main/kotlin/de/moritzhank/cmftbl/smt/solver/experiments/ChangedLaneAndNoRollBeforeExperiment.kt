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

private val changesLaneAndNoRollBefore = formula { v: CCB<Vehicle> ->
  until(Pair(1, 2)) {
    term(v * Vehicle::rotation * Rotation::roll) lt const(1.5)
    binding(term(v * Vehicle::lane)) { l ->
        (term(v * Vehicle::lane * Lane::laneId) ne term(l * Lane::laneId)) and
                (term(v * Vehicle::lane * Lane::road * Road::id) eq term(l * Lane::road * Road::id))
    }.apply { ccb.debugInfo = "l" }
  }
}

private val changesLaneAndNoRollBefore2 = formula { v1: CCB<Vehicle>, v2: CCB<Vehicle> ->
  until(Pair(1, 2)) {
    term(v1 * Vehicle::rotation * Rotation::roll) lt term(v2 * Vehicle::rotation * Rotation::roll)
    binding(term(v1 * Vehicle::lane)) { l ->
      (term(v1 * Vehicle::lane * Lane::laneId) ne term(l * Lane::laneId)) and
              (term(v1 * Vehicle::lane * Lane::road * Road::id) eq term(l * Lane::road * Road::id))
    }.apply { ccb.debugInfo = "l" }
  }
}

fun main() {
  val ccb = CCB<Vehicle>(Vehicle::class).apply { debugInfo = "v" }

  //Viewing Town 10HD, Seed 3, Segment 1, Vehicle 49
  val seg: Segment = ExperimentLoader.loadTestSegments("10HD", "3")[1]
  val vehicleID1 = 49
  val vehicleID2 = 50
  val ticks = seg.tickData.map { it.currentTick.tickSeconds }.toTypedArray()

  val formula = changesLaneAndNoRollBefore2(CCB<Vehicle>(Vehicle::class).apply { debugInfo = "v1" },
    CCB<Vehicle>(Vehicle::class).apply { debugInfo = "v2" })
  renderLatexFormula(formulaToLatex(formula))

  val graphViz = changesLaneAndNoRollBefore2.generateVisualization(
    emptyVehicle(id = vehicleID1), emptyVehicle(id = vehicleID2), "v1", "v2", ticks
  ).generateGraphvizCode()
  renderTree(graphViz)

  /*
  val dataSmtLib = generateSmtLibForSegment(seg, SmtSolver.YICES, "QF_LIRA")
  val formulaSmtLib = generateSmtLib(changesLaneAndNoRollBefore, emptyVehicle(id = vehicleID), "v", ticks)

  println(runSmtSolver("$dataSmtLib$formulaSmtLib(check-sat)", SmtSolver.YICES, false)
   */
}
