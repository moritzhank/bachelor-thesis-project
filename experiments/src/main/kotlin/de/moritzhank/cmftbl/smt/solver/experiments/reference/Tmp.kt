package de.moritzhank.cmftbl.smt.solver.experiments.reference

import de.moritzhank.cmftbl.smt.solver.ExperimentLoader
import kotlin.time.measureTime
import tools.aqua.stars.core.evaluation.PredicateContext
import tools.aqua.stars.core.evaluation.UnaryPredicate.Companion.predicate
import tools.aqua.stars.data.av.dataclasses.Vehicle

val hasV0EverBeenOvertakenFromRight =
    predicate(Vehicle::class) { ctx, v0 ->
      v0.tickData.vehicles.any { v1 -> v0 != v1 && rightOvertaking.holds(ctx, v1, v0) }
    }

fun main() {
  ExperimentLoader.getSeeds("10HD").forEach { seed ->
    val segs = ExperimentLoader.loadTestSegments("10HD", "$seed")
    segs.first().vehicleIds.forEach { vehicle ->
      segs.forEachIndexed { i, seg ->
        val segStart = seg.tickData.first().currentTick
        val segDuration = seg.tickData.last().currentTick.minus(segStart)
        val segStartF = String.format("%.1f", segStart.tickSeconds).replace(",", ".")
        val segDurationF = String.format("%.1f", segDuration.differenceSeconds).replace(",", ".")
        println("$seed|Seg_$i: -c $vehicle -s $segStartF -d $segDurationF")
        val vehicle = seg.tickData.first().vehicles.find { it.id == vehicle }!!
        // Does vehicle exist in all other ticks?
        assert(!seg.tickData.any { !it.vehicles.contains(vehicle) })
        val predHolds: Boolean
        val time = measureTime {
          predHolds = hasV0EverBeenOvertakenFromRight.holds(PredicateContext(seg), vehicle)
        }
        println("PredHolds: $predHolds; $time")
        if (predHolds) {
          return
        }
        println()
      }
    }
  }
}
