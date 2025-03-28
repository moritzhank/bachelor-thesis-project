@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.experiments.reference

import de.moritzhank.cmftbl.smt.solver.ExperimentLoader
import tools.aqua.stars.core.evaluation.PredicateContext

data class OvertakingEvent(val seed: Int, val segment: Int, val vehicleId: Int)

fun overtakingEventsInCity(city: String, seeds: Array<Int>): List<OvertakingEvent> {
  val results = mutableListOf<OvertakingEvent>()
  seeds.forEach { seed ->
    try {
      val segs = ExperimentLoader.loadTestSegments(city, "$seed")
      segs.forEachIndexed { i, seg ->
        seg.vehicleIds.forEach { vId ->
          val vehicle = seg.tickData.first().vehicles.find { it.id == vId }!!
          if (hasOvertaken.holds(PredicateContext(seg), vehicle)) {
            results.add(OvertakingEvent(seed, i, vId))
          }
        }
      }
    } catch (ex: Exception) {
      println("ERROR occurred with seed $seed.")
    }
  }
  return results
}

fun main() {
  val res01 = overtakingEventsInCity("01", ExperimentLoader.carlaSeeds.town01)
  res01.forEach {
    println("Town 01; Seed ${it.seed}; Segment ${it.segment}; Vehicle ${it.vehicleId}")
  }
  println()
  val res02 = overtakingEventsInCity("02", ExperimentLoader.carlaSeeds.town02)
  res02.forEach {
    println("Town 02; Seed ${it.seed}; Segment ${it.segment}; Vehicle ${it.vehicleId}")
  }
  println()
  val res10HD = overtakingEventsInCity("10HD", ExperimentLoader.carlaSeeds.town10HD)
  res10HD.forEach {
    println("Town 10HD; Seed ${it.seed}; Segment ${it.segment}; Vehicle ${it.vehicleId}")
  }
}

/*
Results:
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed2.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed7.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed10.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed15.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed18.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed19.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed22.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed25.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed26.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed29.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed32.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed33.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed36.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed42.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed47.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed52.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed53.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed55.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed58.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed59.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed61.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed62.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed64.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed67.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed69.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed73.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed78.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed86.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed88.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town01/dynamic_data__Game_Carla_Maps_Town01_seed94.json

Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed0.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed4.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed5.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed8.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed12.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed13.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed16.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed17.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed21.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed23.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed24.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed27.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed30.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed31.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed34.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed35.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed38.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed39.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed41.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed43.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed45.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed46.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed48.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed49.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed50.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed54.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed57.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed60.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed65.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed66.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed68.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed70.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed76.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed77.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed79.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed80.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed81.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed82.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed83.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed84.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed93.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed96.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed97.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed99.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town02/dynamic_data__Game_Carla_Maps_Town02_seed100.json

Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed1.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed3.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed6.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed9.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed11.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed14.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed20.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed28.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed37.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed40.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed44.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed51.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed56.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed72.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed74.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed75.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed85.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed87.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed89.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed90.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed91.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed92.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed95.json
Reading simulation run file: file:///C:/Users/hanek/Desktop/stars/stars-logic-kcmftbl-smtModelChecker-experiment/build/resources/main/data/_Game_Carla_Maps_Town10HD/dynamic_data__Game_Carla_Maps_Town10HD_seed98.json
Town 10HD; Seed 1; Segment 2; Vehicle 110
Town 10HD; Seed 1; Segment 2; Vehicle 138
Town 10HD; Seed 1; Segment 2; Vehicle 164
Town 10HD; Seed 1; Segment 4; Vehicle 194
Town 10HD; Seed 3; Segment 6; Vehicle 140
Town 10HD; Seed 3; Segment 6; Vehicle 159
Town 10HD; Seed 3; Segment 8; Vehicle 78
Town 10HD; Seed 3; Segment 10; Vehicle 100
Town 10HD; Seed 6; Segment 2; Vehicle 80
Town 10HD; Seed 6; Segment 2; Vehicle 110
Town 10HD; Seed 6; Segment 2; Vehicle 111
Town 10HD; Seed 9; Segment 4; Vehicle 195
Town 10HD; Seed 9; Segment 6; Vehicle 53
Town 10HD; Seed 9; Segment 11; Vehicle 114
Town 10HD; Seed 9; Segment 11; Vehicle 134
Town 10HD; Seed 11; Segment 0; Vehicle 76
Town 10HD; Seed 11; Segment 5; Vehicle 60
Town 10HD; Seed 14; Segment 2; Vehicle 110
Town 10HD; Seed 14; Segment 4; Vehicle 156
Town 10HD; Seed 14; Segment 9; Vehicle 93
Town 10HD; Seed 20; Segment 9; Vehicle 189
Town 10HD; Seed 28; Segment 1; Vehicle 161
Town 10HD; Seed 28; Segment 2; Vehicle 116
Town 10HD; Seed 28; Segment 12; Vehicle 127
Town 10HD; Seed 37; Segment 4; Vehicle 194
Town 10HD; Seed 40; Segment 1; Vehicle 60
Town 10HD; Seed 40; Segment 9; Vehicle 200
Town 10HD; Seed 44; Segment 3; Vehicle 120
Town 10HD; Seed 44; Segment 5; Vehicle 187
Town 10HD; Seed 44; Segment 9; Vehicle 89
Town 10HD; Seed 44; Segment 10; Vehicle 54
Town 10HD; Seed 44; Segment 10; Vehicle 56
Town 10HD; Seed 44; Segment 10; Vehicle 173
Town 10HD; Seed 44; Segment 14; Vehicle 89
Town 10HD; Seed 44; Segment 14; Vehicle 140
Town 10HD; Seed 44; Segment 14; Vehicle 190
Town 10HD; Seed 51; Segment 2; Vehicle 53
Town 10HD; Seed 51; Segment 2; Vehicle 138
Town 10HD; Seed 51; Segment 4; Vehicle 126
Town 10HD; Seed 56; Segment 1; Vehicle 109
Town 10HD; Seed 56; Segment 1; Vehicle 121
Town 10HD; Seed 56; Segment 2; Vehicle 106
Town 10HD; Seed 56; Segment 2; Vehicle 110
Town 10HD; Seed 56; Segment 4; Vehicle 55
Town 10HD; Seed 56; Segment 4; Vehicle 153
Town 10HD; Seed 56; Segment 5; Vehicle 143
Town 10HD; Seed 72; Segment 1; Vehicle 128
Town 10HD; Seed 72; Segment 1; Vehicle 144
Town 10HD; Seed 72; Segment 2; Vehicle 86
Town 10HD; Seed 72; Segment 2; Vehicle 112
Town 10HD; Seed 72; Segment 4; Vehicle 105
Town 10HD; Seed 72; Segment 11; Vehicle 105
Town 10HD; Seed 72; Segment 13; Vehicle 100
Town 10HD; Seed 74; Segment 0; Vehicle 169
Town 10HD; Seed 74; Segment 4; Vehicle 134
Town 10HD; Seed 74; Segment 9; Vehicle 78
Town 10HD; Seed 74; Segment 9; Vehicle 159
Town 10HD; Seed 75; Segment 4; Vehicle 79
Town 10HD; Seed 75; Segment 4; Vehicle 87
Town 10HD; Seed 75; Segment 4; Vehicle 173
Town 10HD; Seed 75; Segment 6; Vehicle 47
Town 10HD; Seed 85; Segment 2; Vehicle 110
Town 10HD; Seed 85; Segment 11; Vehicle 197
Town 10HD; Seed 87; Segment 9; Vehicle 47
Town 10HD; Seed 87; Segment 9; Vehicle 158
Town 10HD; Seed 89; Segment 2; Vehicle 144
Town 10HD; Seed 89; Segment 4; Vehicle 137
Town 10HD; Seed 89; Segment 6; Vehicle 154
Town 10HD; Seed 89; Segment 9; Vehicle 64
Town 10HD; Seed 89; Segment 9; Vehicle 199
Town 10HD; Seed 90; Segment 1; Vehicle 146
Town 10HD; Seed 90; Segment 2; Vehicle 110
Town 10HD; Seed 90; Segment 10; Vehicle 142
Town 10HD; Seed 91; Segment 2; Vehicle 123
Town 10HD; Seed 91; Segment 4; Vehicle 121
Town 10HD; Seed 91; Segment 4; Vehicle 174
Town 10HD; Seed 92; Segment 1; Vehicle 146
Town 10HD; Seed 92; Segment 1; Vehicle 195
Town 10HD; Seed 92; Segment 4; Vehicle 83
Town 10HD; Seed 92; Segment 4; Vehicle 104
Town 10HD; Seed 92; Segment 4; Vehicle 145
Town 10HD; Seed 92; Segment 4; Vehicle 194
Town 10HD; Seed 92; Segment 10; Vehicle 156
Town 10HD; Seed 92; Segment 10; Vehicle 187
Town 10HD; Seed 95; Segment 4; Vehicle 125
Town 10HD; Seed 95; Segment 4; Vehicle 156
Town 10HD; Seed 95; Segment 4; Vehicle 170
Town 10HD; Seed 98; Segment 4; Vehicle 55
Town 10HD; Seed 98; Segment 5; Vehicle 74
Town 10HD; Seed 98; Segment 7; Vehicle 139

Process finished with exit code 0
 */
