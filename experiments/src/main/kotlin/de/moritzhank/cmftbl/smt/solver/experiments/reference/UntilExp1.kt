@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.experiments.reference

import tools.aqua.stars.core.evaluation.UnaryPredicate.Companion.predicate
import tools.aqua.stars.data.av.dataclasses.Vehicle
import tools.aqua.stars.logic.kcmftbl.eventually

/*
val changedLaneAndHadSpeedBefore = formula { v: CCB<Vehicle> ->
  binding(term(v * Vehicle::lane)) { l ->
    until(Pair(1, 3)) {
      term(v * Vehicle::effVelocityInKmPH) gt const(0.0)
      term(v * Vehicle::lane * Lane::laneId) ne term(l * Lane::laneId)
    }
  }.apply { ccb.debugInfo = "l" }
}
 */

val changedLaneAndHadSpeedBeforePred = predicate(Vehicle::class) { ctx, v ->
  
  eventually(v) { v0 ->
    eventually(v0) { v1 ->
      v0.lane.road == v1.lane.road && v0.lane != v1.lane && v0.effVelocityInKmPH > 0.0
    }
  }
}


//val changedLane =
//  predicate(Vehicle::class) { _, v ->
//    eventually(v) { v0 ->
//      eventually(v0) { v1 -> v0.lane.road == v1.lane.road && v0.lane != v1.lane }
//    }
//  }

fun main() {

}