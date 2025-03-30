@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.experiments.reference

import de.moritzhank.cmftbl.smt.solver.ExperimentLoader
import de.moritzhank.cmftbl.smt.solver.misc.carlaReplaySegment
import tools.aqua.stars.core.evaluation.PredicateContext
import tools.aqua.stars.core.evaluation.UnaryPredicate.Companion.predicate
import tools.aqua.stars.data.av.dataclasses.TickDataDifferenceSeconds
import tools.aqua.stars.data.av.dataclasses.Vehicle
import tools.aqua.stars.logic.kcmftbl.next
import tools.aqua.stars.logic.kcmftbl.until

// CHANGED LANE UND NICHT CHANGE__S__ LANE!!!!!
private val changedLaneAlt =
  predicate(Vehicle::class) { ctx, v ->
    val lane = v.lane
    until(
      v,
      phi1 = { v0: Vehicle -> true },
      phi2 = { v1: Vehicle ->
        v1.lane.laneId != lane.laneId && v1.lane.road.id == lane.road.id
      }
    )
  }

private val changesLaneAndNoRollBefore =
  predicate(Vehicle::class) { ctx, v ->
    until(
      v,
      TickDataDifferenceSeconds(1.0) to TickDataDifferenceSeconds(2.0),
      phi1 = { v0: Vehicle -> true }, //v0.rotation.roll < 1.5
      phi2 = { v1: Vehicle ->
        next(v1) { v2: Vehicle ->
          v1.lane.laneId != v2.lane.laneId && v1.lane.road.id == v2.lane.road.id
        }
      }
    )
  }

private val changedLaneAndNoSpeedBefore =
  predicate(Vehicle::class) { ctx, v ->
    val lane = v.lane
    until(
      v,
      TickDataDifferenceSeconds(1.0) to TickDataDifferenceSeconds(3.0),
      phi1 = { v0: Vehicle -> v0.effVelocityInKmPH > 0 }, //v0.rotation.roll < 1.5
      phi2 = { v1: Vehicle ->
        v1.lane.laneId != lane.laneId
      }
    )
  }

private fun batchWatchSatSegs(town: String = "10HD", seed: String = "3") {
  val segs = ExperimentLoader.loadTestSegments(town, seed)
  val resultList = mutableListOf<Pair<Int, Vehicle>>()
  segs.forEachIndexed { segI, seg ->
    for (v in seg.tickData.first().vehicles) {
      val holds = changedLaneAndNoSpeedBefore.holds(PredicateContext(seg), v)
      if (holds) {
        resultList.add(Pair(segI, v))
      }
    }
  }
  println()
  println("=============================================================================")
  println("  Overview over sat segments")
  println("=============================================================================")
  for (r in resultList) {
    val seg = segs[r.first]
    val segLength = (seg.tickData.last().currentTick - seg.tickData.first().currentTick).differenceSeconds
    println(" Town $town, Seed $seed, Segment ${r.first} (len: ${segLength.toInt()}), Vehicle ${r.second.id}")
  }
  println("=============================================================================")
  println("Sum: ${resultList.size}")
  for (r in resultList) {
    var again = true
    do {
      println("\n")
      println("=============================================================================")
      println("  Viewing Town $town, Seed $seed, Segment ${r.first}, Vehicle ${r.second.id}")
      println("=============================================================================")
      carlaReplaySegment(town, seed, segs[r.first], r.second.id)
      val currentTicks = segs[r.first].tickData
      val firstTickTime = currentTicks.first().currentTick.tickSeconds
      val cumRelTickTime = currentTicks.map { t -> String.format("%.1f", t.currentTick.tickSeconds - firstTickTime)
        .replace(',', '.')}
      println("CumRelTickTime: $cumRelTickTime")
      println("LaneIDs: " + segs[r.first].tickData.map { it.vehicles.find { it.id == r.second.id }!!.lane.laneId })
      print("To review again enter \"r\": ")
      val input = readln()
      again = input == "r"
    } while (again)
  }
}

fun main() {
  batchWatchSatSegs()

  /*
  val segments = ExperimentLoader.loadTestSegments("10HD", "3")
  val vehicleID = 49
  val segID = 1

  var stopReplay = false
  do {
    carlaReplaySegment("10HD", "3", segments[segID], vehicleID)
    val currentTicks = segments[segID].tickData
    val firstTickTime = currentTicks.first().currentTick.tickSeconds
    val cumRelTickTime = currentTicks.map { t -> String.format("%.1f", t.currentTick.tickSeconds - firstTickTime)
      .replace(',', '.')}
    println("CumRelTickTime: " + cumRelTickTime)
    println("LaneIDs: " + currentTicks.map { it.vehicles.find { it.id == vehicleID }!!.lane.laneId })
    print("To review again enter \"r\": ")
    stopReplay = readln() != "r"
  } while (!stopReplay)
   */
}