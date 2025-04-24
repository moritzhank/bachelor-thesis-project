package de.moritzhank.cmftbl.smt.solver.experiments

import de.moritzhank.cmftbl.smt.solver.ExperimentLoader
import de.moritzhank.cmftbl.smt.solver.SmtSolver
import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.dsl.FormulaBuilder.Companion.formula
import de.moritzhank.cmftbl.smt.solver.dsl.times
import de.moritzhank.cmftbl.smt.solver.generateSmtLibForSegment
import de.moritzhank.cmftbl.smt.solver.misc.*
import de.moritzhank.cmftbl.smt.solver.scripts.LegendPosition
import de.moritzhank.cmftbl.smt.solver.scripts.getDateTimeString
import de.moritzhank.cmftbl.smt.solver.scripts.plotPerf
import de.moritzhank.cmftbl.smt.solver.smtSolverVersion
import de.moritzhank.cmftbl.smt.solver.translation.formula.emitsSomething
import de.moritzhank.cmftbl.smt.solver.translation.formula.generateVisualization
import tools.aqua.stars.data.av.dataclasses.*
import java.io.File

private class ChangedLaneAndNoRollBeforeIncrementalSetup(
  /** How many nodes do remain in [evalNode]. */
  override val identifier: Int,
  /** This specifies the segment for which [evalNode] is evaluated. */
  val segment: Segment,
  /** This holds the sliced evaluation node tree. */
  val evalNode: ITreeVisualizationNode
) : PerfExperimentSetup {

  override fun toString(): String {
    return "ChangedLaneAndNoRollBeforeIncremental with EvaluationTree cut @ $identifier"
  }

}

private class ChangedLaneAndNoRollBeforeIncrementalTest(useMemProfiler: Boolean = true, timeout: Int? = null):
  PerfExperiment<ChangedLaneAndNoRollBeforeIncrementalSetup>("ChangedLaneAndNoRollBeforeIncremental") {

  init {
    memoryProfilerSampleRateMs = 10
    useMemoryProfiler = useMemProfiler
    timeOutInSeconds = timeout
    fileName = { expSetup -> "slicing_${expSetup.identifier}.smt2" }
  }

  override val memoryProfilerWorkingCond: (MemoryProfiler) -> Boolean = { memProfiler ->
    memProfiler.maxProcMemUsageBytes != -1L &&
            memProfiler.maxSysMemUsagePercent != -1.0 &&
            memProfiler.numSamples > 5
  }

  override fun generateSmtLib(
    expSetup: ChangedLaneAndNoRollBeforeIncrementalSetup,
    solver: SmtSolver,
    logic: String
  ): String {
    val dataSmtLib = generateSmtLibForSegment(expSetup.segment, solver, logic)
    val formulaSmtLib = de.moritzhank.cmftbl.smt.solver.translation.formula.generateSmtLib(expSetup.evalNode)
    return "$dataSmtLib$formulaSmtLib"
  }

}

/** Predicate */
private val changedLaneAndNoRollBefore = formula { v: CCB<Vehicle> ->
  binding(term(v * Vehicle::lane)) { l ->
    until(Pair(1, 2)) {
      term(v * Vehicle::rotation * Rotation::roll) lt const(1.5)
      (term(v * Vehicle::lane * Lane::laneId) ne term(l * Lane::laneId)) and
              (term(v * Vehicle::lane * Lane::road * Road::id) eq term(l * Lane::road * Road::id))
    }
  }.apply { ccb.debugInfo = "l" }
}

fun runChangedLaneAndNoRollBeforeIncrementalTest(useMemProfiler: Boolean = true, timeout: Int? = null, params: String) {
  val params = params.split(" ")
  val town = params[0]
  val seed = params[1]
  val segmentID = params[2].toInt()
  val vehicleID = params[3].toInt()
  val paramStr = "(town: $town, seed: $seed, segmentID: $segmentID, vehicleID: $vehicleID)"
  println("Running ChangedLaneAndNoRollBeforeIncremental with $paramStr")

  val resMaxSolverMemUsageGBLambda: (List<Long>) -> String = { list ->
    val avg = list.avgWithoutInvalids()
    if (avg == -1L) "-1" else "${MemoryProfiler.bytesToGB(avg)}"
  }
  val resTimeSLambda : (Array<Long>) -> String = { arr ->
    (1.0 * (arr.fold(0L) { acc, elem -> acc + elem }) / (arr.size * 1_000L)).toString()
  }
  // Setup
  val segment: Segment = ExperimentLoader.loadTestSegments(town, seed)[segmentID]
  val ticks = segment.tickData.map { it.currentTick.tickSeconds }.toTypedArray()
  val fullEvalNode = changedLaneAndNoRollBefore.generateVisualization(emptyVehicle(id = vehicleID), "v", ticks)
  val nodesThatCanBeSliced = fullEvalNode.iterator().asSequence().filter { it.emitsSomething() }.toList()
  val numberNodes = nodesThatCanBeSliced.size
  val listOfExperiments = mutableListOf<ChangedLaneAndNoRollBeforeIncrementalSetup>()
  for (i in 1 .. numberNodes) {
    listOfExperiments.add(ChangedLaneAndNoRollBeforeIncrementalSetup(i, segment, fullEvalNode.copyAndSlice(i)))
  }
  val runID = getDateTimeString('-', '-', "-", false)
  val runDir = ChangedLaneAndNoRollBeforeIncrementalTest().getRunDirectoryPath(runID)
  File(runDir).mkdirs()

  // Generate visualization
  print("Generating visualization for sliced formulae ...")
  for (exp in listOfExperiments) {
    val tmp = fullEvalNode.copyAndSlice(exp.identifier)
    val svgFileName = "slicing_${exp.identifier}"
    val path = "$runDir${File.separator}$svgFileName.svg"
    renderTree(tmp.generateGraphvizCode(), path)
  }
  println(" Finished")

  // YICES
  val yicesVersion = smtSolverVersion(SmtSolver.YICES)
  val resYices = ChangedLaneAndNoRollBeforeIncrementalTest(useMemProfiler, timeout).runExperiment(
    listOfExperiments,
    SmtSolver.YICES,
    "QF_LIRA",
    1,
    "#44B7C2",
    "Yices v$yicesVersion",
    runID,
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda
  )

  // Z3
  val z3Version = smtSolverVersion(SmtSolver.Z3)
  val resZ3 = ChangedLaneAndNoRollBeforeIncrementalTest(useMemProfiler, timeout).runExperiment(
    listOfExperiments,
    SmtSolver.Z3,
    "QF_LIRA",
    1,
    "#034B7B",
    "Z3 v$z3Version",
    runID,
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda
  )

  // CVC5
  val cvc5Version = smtSolverVersion(SmtSolver.CVC5)
  val resCVC5 = ChangedLaneAndNoRollBeforeIncrementalTest(useMemProfiler, timeout).runExperiment(
    listOfExperiments,
    SmtSolver.CVC5,
    "QF_LIRA",
    1,
    "#808080",
    "CVC5 v$cvc5Version",
    runID,
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda
  )

  val outputFile = "$runDir${File.separator}graph_${getDateTimeString()}.png"
  plotPerf(resYices, resZ3, resCVC5, title = "ChangedLaneAndNoRollBefore incremental test", xLabel = "Cut level",
    legendPosition = LegendPosition.BEST, outputFile = outputFile, rmMemPlot = !useMemProfiler)
}
