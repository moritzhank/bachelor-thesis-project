@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.misc

import de.moritzhank.cmftbl.smt.solver.requireCarlaPyVersion
import de.moritzhank.cmftbl.smt.solver.requirePathToCarlaData
import de.moritzhank.cmftbl.smt.solver.requirePathToCarlaReplayPy
import de.moritzhank.cmftbl.smt.solver.requirePathToCarlaVenvParentDir
import de.moritzhank.cmftbl.smt.solver.scripts.PythonCommandLineWrapper
import tools.aqua.stars.data.av.dataclasses.Segment
import java.io.File
import kotlin.io.path.Path
import kotlin.math.max

private fun setupCarlaVenv(path: String, carlaVersion: String, reinstall: Boolean = false): String {
  // Preparation
  val execEnding = if (System.getProperty("os.name").lowercase().contains("windows")) ".exe" else ""
  val carlaVenvPath = Path(path, "/carlaVenv/").toString().replace('\\', '/').trimEnd('/')
  if (File(carlaVenvPath).exists() && !reinstall) {
    return "$carlaVenvPath/Scripts/python$execEnding"
  }
  val py = PythonCommandLineWrapper.pythonBaseCmd()
  val carlaVenvRequirements = "carla~=$carlaVersion\n" +
          "numpy\n" +
          "dataclass-wizard\n" +
          "opencv-python\n" +
          "scipy"
  // Installation
  File(carlaVenvPath).deleteRecursively()
  runCommand("$py -m pip install --user --upgrade pip", path)
  runCommand("$py -m pip install --user virtualenv", path)
  runCommand("$py -m venv carlaVenv", path)
  runCommand("$carlaVenvPath/Scripts/python$execEnding -m pip install --upgrade pip", path)
  File("$carlaVenvPath/requirements.txt").writeText(carlaVenvRequirements)
  runCommand("$carlaVenvPath/Scripts/pip$execEnding install -r $carlaVenvPath/requirements.txt", path)
  return "$carlaVenvPath/Scripts/python$execEnding"
}

private fun runCommand(command: String, dir: String? = null) {
  println("Run \"$command\" ...")
  var pBuilder = ProcessBuilder(command.split(" "))
  if (dir != null) {
    pBuilder = pBuilder.directory(File(dir))
  }
  val p = pBuilder.start().apply { waitFor() }
  println(p.inputReader().readText())
  val error = p.errorReader().readText()
  if (error.isNotEmpty()) {
    println(error)
  }
  require(p.exitValue() == 0)
}

fun carlaReplay(
  town: String,
  seed: String,
  vehicleID: Int,
  start: Double,
  duration: Double,
  speed: Double = 1.0,
) {
  val replayDataPath = requirePathToCarlaData().replace('\\', '/').trimEnd('/')
  val replayPy = requirePathToCarlaReplayPy()
  val venvPy = setupCarlaVenv(requirePathToCarlaVenvParentDir(), requireCarlaPyVersion())
  val pathToFile = "$replayDataPath/records/_Game_Carla_Maps_Town$town/_Game_Carla_Maps_Town${town}_seed$seed.log"
  runCommand("$venvPy $replayPy -x $speed -c $vehicleID -s $start -d $duration -f $pathToFile")
}

fun carlaReplaySegment(town: String, seed: String, seg: Segment, vehicleID: Int, duration: Double? = null,
                     startOffset: Double? = null) {
  val firstTickSec = max(seg.tickData.first().currentTick.tickSeconds + (startOffset ?: 0.0), 0.0)
  val lastTickSec = seg.tickData.last().currentTick.tickSeconds
  val tickSecDuration = duration ?: (lastTickSec - firstTickSec)
  carlaReplay(town, seed, vehicleID, firstTickSec, tickSecDuration)
}
