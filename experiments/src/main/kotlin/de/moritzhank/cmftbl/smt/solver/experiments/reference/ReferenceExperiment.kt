@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.experiments.reference

import de.moritzhank.cmftbl.smt.solver.ExperimentLoader
import tools.aqua.stars.data.av.dataclasses.Segment
import kotlin.time.Duration

data class UnaryEvent(val city: String, val seed: Int, val segment: Int, val id: Int?, val measuredTime: Duration)

fun unaryPredHoldsForAny(pred: (Segment) -> Pair<Int?, Duration>, cities: Array<String>, seeds: Array<Int>) : List<UnaryEvent> {
  val results = mutableListOf<UnaryEvent>()
  cities.forEach { city ->
    seeds.forEach { seed ->
      try {
        val segments = ExperimentLoader.loadTestSegments(city, "$seed")
        segments.forEachIndexed { i, seg ->
          val holdsForId = pred(seg)
          results.add(UnaryEvent(city, seed, i, holdsForId.first, holdsForId.second))
        }
      } catch (ex: Exception) {
        println("ERROR occurred with seed $seed.")
        ex.printStackTrace()
      }
    }
  }
  return results
}