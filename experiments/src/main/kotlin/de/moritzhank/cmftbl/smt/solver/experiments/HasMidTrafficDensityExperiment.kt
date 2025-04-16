package de.moritzhank.cmftbl.smt.solver.experiments

import de.moritzhank.cmftbl.smt.solver.ExperimentLoader
import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.dsl.FormulaBuilder.Companion.formula
import de.moritzhank.cmftbl.smt.solver.dsl.TFunctionBuilder.Companion.function
import de.moritzhank.cmftbl.smt.solver.dsl.times
import de.moritzhank.cmftbl.smt.solver.misc.emptyVehicle
import de.moritzhank.cmftbl.smt.solver.misc.generateGraphvizCode
import de.moritzhank.cmftbl.smt.solver.misc.renderTree
import de.moritzhank.cmftbl.smt.solver.saveSmtFile
import de.moritzhank.cmftbl.smt.solver.translation.formula.generateSmtLib
import de.moritzhank.cmftbl.smt.solver.translation.formula.generateVisualization
import tools.aqua.stars.data.av.dataclasses.*

private val vehiclesInBlock = function { t: CCB<TickData>, b: CCB<Block> ->
  filter(t * TickData::vehicles) { v: CCB<Vehicle> ->
    eq {
      wrap(v * Vehicle::lane * Lane::road * Road::block)
      wrap(b)
    }
  }
}

private val hasMidTrafficDensity = formula { v: CCB<Vehicle> ->
  registerFunction(TickData::vehiclesInBlock, vehiclesInBlock)
  minPrevalence(0.6, Pair(0, 0)) {
    val block = v * Vehicle::lane * Lane::road * Road::block
    val numVehicles =
      term(
        (v * Vehicle::tickData * TickData::vehiclesInBlock).withParam(block) *
                List<Vehicle>::size)
    const(6) leq numVehicles and (numVehicles leq const(15))
  }
}

fun main() {
  //Viewing Town 10HD, Seed 3, Segment 10, Vehicle 126

  val seg: Segment = ExperimentLoader.loadTestSegments("10HD", "3")[10]
  val vehicleID1 = 126
  val ticks = seg.tickData.map { it.currentTick.tickSeconds }.toTypedArray()

  val visualization = hasMidTrafficDensity.generateVisualization(emptyVehicle(id = vehicleID1), "v", ticks)
  val graphViz = visualization.generateGraphvizCode()
  renderTree(graphViz)

  saveSmtFile(generateSmtLib(visualization))
}
