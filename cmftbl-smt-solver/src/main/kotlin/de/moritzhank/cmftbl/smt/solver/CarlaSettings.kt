@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import de.moritzhank.cmftbl.smt.solver.misc.getAbsolutePathFromProjectDir

/** Contains all relevant settings for the usage of Carla simulator. */
@Serializable
data class CarlaSettings(
  var carlaPyVersion: String,
  var pathToCarlaReplayPy: String,
  var pathToCarlaVenvParentDir: String
) {

  companion object {

    private val settingsFilePath = getAbsolutePathFromProjectDir("carlaSettings.json")

    /** Loads the [CarlaSettings] from "carlaSettings.json". */
    fun load(): CarlaSettings? {
      val settingsFile = File(settingsFilePath)
      if (!settingsFile.exists()) {
        return null
      }
      return Json.decodeFromString(settingsFile.readText())
    }

    /** Generate empty "carlaSettings.json". */
    fun generateTemplate() {
      File(settingsFilePath).writeText(Json.encodeToString(CarlaSettings("0.9.14", "", "")))
    }
  }
}

fun requireCarlaPyVersion(): String {
  val settings = CarlaSettings.load()
  requireNotNull(settings) {
    CarlaSettings.generateTemplate()
    "The file carlaSettings.json must be specified."
  }
  return settings.carlaPyVersion
}

fun requirePathToCarlaReplayPy(): String {
  val settings = CarlaSettings.load()
  requireNotNull(settings) {
    CarlaSettings.generateTemplate()
    "The file carlaSettings.json must be specified."
  }
  val replayPyPath = settings.pathToCarlaReplayPy
  require(File(replayPyPath).exists()) { "The specified file (at \"$replayPyPath\") does not exist." }
  return replayPyPath
}

fun requirePathToCarlaVenvParentDir(): String {
  val settings = CarlaSettings.load()
  requireNotNull(settings) {
    CarlaSettings.generateTemplate()
    "The file carlaSettings.json must be specified."
  }
  val path = settings.pathToCarlaVenvParentDir
  require(File(path).exists()) { "The specified directory (at \"$path\") does not exist." }
  return path
}
