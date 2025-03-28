@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import de.moritzhank.cmftbl.smt.solver.misc.getAbsolutePathFromProjectDir

/** Contains all relevant settings for the data of experiments. */
@Serializable
data class DataSettings(var pathToCarlaData: String) {

  companion object {

    private val settingsFilePath = getAbsolutePathFromProjectDir("dataSettings.json")

    /** Loads the [DataSettings] from "dataSettings.json". */
    fun load(): DataSettings? {
      val settingsFile = File(settingsFilePath)
      if (!settingsFile.exists()) {
        return null
      }
      return Json.decodeFromString(settingsFile.readText())
    }

    /** Generate empty "dataSettings.json". */
    fun generateTemplate() {
      File(settingsFilePath).writeText(Json.encodeToString(DataSettings("")))
    }
  }
}

fun requirePathToCarlaData(): String {
  val settings = DataSettings.load()
  requireNotNull(settings) {
    DataSettings.generateTemplate()
    "The file dataSettings.json must be specified."
  }
  val dataPath = settings.pathToCarlaData
  require(File(dataPath).exists()) { "The specified dictionary (at \"$dataPath\") does not exist." }
  return dataPath
}
