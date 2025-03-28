@file:Suppress("unused", "unused_variable")

package de.moritzhank.cmftbl.smt.solver.experiments

import kotlinx.serialization.modules.EmptySerializersModule
import tools.aqua.stars.data.av.dataclasses.Block
import tools.aqua.stars.data.av.dataclasses.Lane
import tools.aqua.stars.data.av.dataclasses.Road
import tools.aqua.stars.data.av.dataclasses.Segment
import tools.aqua.stars.data.av.dataclasses.TickData
import tools.aqua.stars.data.av.dataclasses.Vehicle
import de.moritzhank.cmftbl.smt.solver.dsl.And
import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.dsl.FormulaBuilder.Companion.formula
import de.moritzhank.cmftbl.smt.solver.dsl.Leq
import de.moritzhank.cmftbl.smt.solver.dsl.MinPrevalence
import de.moritzhank.cmftbl.smt.solver.dsl.TFunctionBuilder.Companion.function
import de.moritzhank.cmftbl.smt.solver.dsl.Variable
import de.moritzhank.cmftbl.smt.solver.dsl.times
import de.moritzhank.cmftbl.smt.solver.ExperimentLoader
import de.moritzhank.cmftbl.smt.solver.translation.data.SmtDataTranslationWrapper
import de.moritzhank.cmftbl.smt.solver.translation.data.getSmtIntermediateRepresentation

val vehiclesInBlock = function { t: CCB<TickData>, b: CCB<Block> ->
  filter(t * TickData::vehicles) { v: CCB<Vehicle> ->
    eq {
      wrap(v * Vehicle::lane * Lane::road * Road::block)
      wrap(b)
    }
  }
}
val hasMidTrafficDensity = formula { v: CCB<Vehicle> ->
  registerFunction(TickData::vehiclesInBlock, vehiclesInBlock)
  minPrevalence(0.6) {
    val block = v * Vehicle::lane * Lane::road * Road::block
    val numVehicles =
        term(
            (v * Vehicle::tickData * TickData::vehiclesInBlock).withParam(block) *
                List<Vehicle>::size)
    const(6) leq numVehicles and (numVehicles leq const(15))
  }
}
val ast = hasMidTrafficDensity(CCB<Vehicle>().apply { debugInfo = "v" })

fun main() {
  val seg: Segment = ExperimentLoader.loadTestSegment()
  // val primaryEntity = seg.tickData.first().vehicles.find { it.id == seg.primaryEntityId }
  val serializersModule = EmptySerializersModule()
  var intermediateRepresentation = getSmtIntermediateRepresentation(serializersModule, seg)
  val translationWrapper =
      SmtDataTranslationWrapper(intermediateRepresentation, seg.tickData.toTypedArray())

  val y =
      intermediateRepresentation
          .find { it.ref is Vehicle && (it.ref as Vehicle).id == seg.primaryEntityId }!!
          .ref
          .getSmtID()
  val z = translationWrapper.smtIDToExternalID[y]

  val x =
      (((ast.getPhi().first() as MinPrevalence).inner as And).lhs as Leq<*>).rhs as Variable<*>
}
