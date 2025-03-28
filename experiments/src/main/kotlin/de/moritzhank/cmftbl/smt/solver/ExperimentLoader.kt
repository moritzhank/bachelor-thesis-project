@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tools.aqua.stars.data.av.dataclasses.Segment
import tools.aqua.stars.importer.carla.CarlaSimulationRunsWrapper
import tools.aqua.stars.importer.carla.loadSegments

object ExperimentLoader {

  private var setup: Boolean = false
  lateinit var carlaSeeds: CarlaSeeds
  lateinit var pathToCarlaData: String

  fun loadTestSegment(town: String = "01", seed: String = "2"): Segment {
    return loadTestSegments(town, seed).first()
  }

  fun loadTestSegments(town: String = "01", seed: String = "2"): List<Segment> {
    init()
    val dynamic =
        Path(
            "$pathToCarlaData/_Game_Carla_Maps_Town$town/dynamic_data__Game_Carla_Maps_Town${town}_seed$seed.json")
    val static =
        Path(
            "$pathToCarlaData/_Game_Carla_Maps_Town$town/static_data__Game_Carla_Maps_Town${town}.json")
    val wrapper = CarlaSimulationRunsWrapper(static, listOf(dynamic))
    return loadSegments(listOf(wrapper), false, 10, true).toList()
  }

  /** Segments are sorted ascending by length. */
  fun loadTestSegmentsSortedByLength(town: String = "01", seed: String = "2"): List<Segment> {
    return loadTestSegments(town, seed).sortedBy {
      val startTick = it.mainInitList.first().currentTick.tickSeconds
      val endTick = it.mainInitList.last().currentTick.tickSeconds
      endTick - startTick
    }
  }

  fun getSeeds(town: String): Array<Int> {
    init()
    return when (town) {
      "01" -> carlaSeeds.town01
      "02" -> carlaSeeds.town02
      "10HD" -> carlaSeeds.town10HD
      else -> throw IllegalArgumentException()
    }
  }

  private fun init() {
    if (!setup) {
      val pathToCarlaSeedsJson = getPathToResource("/data/carla_seeds.json").toString()
      val fileContent = File(pathToCarlaSeedsJson).readText()
      carlaSeeds = Json.decodeFromString(fileContent)
      pathToCarlaData = requirePathToCarlaData().trimEnd('/', '\\').replace('\\', '/')
      setup = true
    }
  }

  fun getPathToResource(name: String): Path {
    val uri = ExperimentLoader::class.java.getResource(name)!!.toURI()
    return try {
      Paths.get(uri)
    } catch (_: Exception) {
      Paths.get(name.trimStart('/', '\\')).absolute()
    }
  }
}

@Serializable
data class CarlaSeeds(val town01: Array<Int>, val town02: Array<Int>, val town10HD: Array<Int>)
