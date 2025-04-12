package de.moritzhank.cmftbl.smt.solver.experiments

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import de.moritzhank.cmftbl.smt.solver.ExperimentLoader
import de.moritzhank.cmftbl.smt.solver.SmtSolver
import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.dsl.FormulaBuilder.Companion.formula
import de.moritzhank.cmftbl.smt.solver.dsl.times
import de.moritzhank.cmftbl.smt.solver.generateSmtLibForSegment
import de.moritzhank.cmftbl.smt.solver.misc.ITreeVisualizationNode
import de.moritzhank.cmftbl.smt.solver.misc.MemoryProfiler
import de.moritzhank.cmftbl.smt.solver.misc.avgWithoutInvalids
import de.moritzhank.cmftbl.smt.solver.misc.emptyVehicle
import de.moritzhank.cmftbl.smt.solver.misc.generateGraphvizCode
import de.moritzhank.cmftbl.smt.solver.misc.renderTree
import de.moritzhank.cmftbl.smt.solver.scripts.LegendPosition
import de.moritzhank.cmftbl.smt.solver.scripts.getDateTimeString
import de.moritzhank.cmftbl.smt.solver.scripts.plotPerf
import de.moritzhank.cmftbl.smt.solver.smtSolverVersion
import de.moritzhank.cmftbl.smt.solver.translation.formula.emitsSomething
import de.moritzhank.cmftbl.smt.solver.translation.formula.generateVisualization
import tools.aqua.stars.data.av.dataclasses.*

private val changedLaneAndNoRollBefore = formula { v: CCB<Vehicle> ->
  binding(term(v * Vehicle::lane)) { l ->
    until(Pair(1, 2)) {
      term(v * Vehicle::rotation * Rotation::roll) lt const(1.5)
      (term(v * Vehicle::lane * Lane::laneId) ne term(l * Lane::laneId)) and
              (term(v * Vehicle::lane * Lane::road * Road::id) eq term(l * Lane::road * Road::id))
    }
  }.apply { ccb.debugInfo = "l" }
}

class ChangedLaneAndNoRollBeforeIncrementalSetup(
  /** For information only. */
  override val x: Int,
  val segment: Segment,
  val evalNode: ITreeVisualizationNode
) : PerfExperimentSetup {

  override val overrideSmt2FileName: String? = "slicedExp_$x"

  override fun toString(): String {
    return "ChangedLaneAndNoRollBeforeIncremental with EvaluationTree cut @ $x"
  }

}

class ChangedLaneAndNoRollBeforeIncrementalTest(useMemProfiler: Boolean = true, timeout: Int = 120):
  PerfExperiment("ChangedLaneAndNoRollBeforeIncremental") {

  init {
    memoryProfilerSampleRateMs = 10
    useMemoryProfiler = useMemProfiler
    timeOutInSeconds = timeout
  }

  override val memoryProfilerWorkingCond: (MemoryProfiler) -> Boolean = { memProfiler ->
    memProfiler.maxProcMemUsageBytes != -1L &&
            memProfiler.maxSysMemUsagePercent != -1.0 &&
            memProfiler.numSamples > 5
  }

  override fun generateSmtLib(
    exp: PerfExperimentSetup,
    solver: SmtSolver,
    logic: String
  ): String {
    require(exp is ChangedLaneAndNoRollBeforeIncrementalSetup)
    val dataSmtLib = generateSmtLibForSegment(exp.segment, solver, logic)
    val formulaSmtLib = de.moritzhank.cmftbl.smt.solver.translation.formula.generateSmtLib(exp.evalNode)
    return "$dataSmtLib$formulaSmtLib"
  }

}

fun runChangedLaneAndNoRollBeforeIncrementalTest(useMemProfiler: Boolean = true, timeout: Int = 120) {
  val resMaxSolverMemUsageGBLambda: (List<Long>) -> String = { list ->
    val avg = list.avgWithoutInvalids()
    if (avg == -1L) "-1" else "${MemoryProfiler.bytesToGB(avg)}"
  }
  val resTimeSLambda : (Array<Long>) -> String = { arr ->
    (1.0 * (arr.fold(0L) { acc, elem -> acc + elem }) / (arr.size * 1_000L)).toString()
  }

  // Setup
  val segment: Segment = ExperimentLoader.loadTestSegments("10HD", "3")[10]
  val vehicleID = 126
  val ticks = segment.tickData.map { it.currentTick.tickSeconds }.toTypedArray()
  val fullEvalNode = changedLaneAndNoRollBefore.generateVisualization(emptyVehicle(id = vehicleID), "v", ticks)
  val nodesThatCanBeSliced = fullEvalNode.iterator().asSequence().filter { it.emitsSomething() }.toList()
  val numberNodes = nodesThatCanBeSliced.size
  val listOfExperiments = mutableListOf<ChangedLaneAndNoRollBeforeIncrementalSetup>()
  for (i in 1 .. numberNodes) {
    listOfExperiments.add(ChangedLaneAndNoRollBeforeIncrementalSetup(i, segment, fullEvalNode.copyAndSlice(i)))
  }

  // View EvalNodeTrees
  println("Slicing ...")
  for (exp in listOfExperiments) {
    val tmp = fullEvalNode.copyAndSlice(exp.x)
    renderTree(tmp.generateGraphvizCode(), false, "slicing_${exp.x}")
  }
  println("Finished ...")

  /*
  // YICES
  val yicesVersion = smtSolverVersion(SmtSolver.YICES)
  val resYices = ChangedLaneAndNoRollBeforeIncrementalTest(useMemProfiler, timeout).runExperiment(
    listOfExperiments,
    SmtSolver.YICES,
    "QF_LIRA",
    1,
    "#44B7C2",
    "Yices v$yicesVersion",
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda,
    false
  )
   */

  // Z3
  val z3Version = smtSolverVersion(SmtSolver.Z3)
  val resZ3 = ChangedLaneAndNoRollBeforeIncrementalTest(useMemProfiler, timeout).runExperiment(
    listOfExperiments,
    SmtSolver.Z3,
    "QF_LIRA",
    1,
    "#034B7B",
    "Z3 v$z3Version",
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda
  )

  /*
  // CVC5
  val cvc5Version = smtSolverVersion(SmtSolver.CVC5)
  val resCVC5 = ChangedLaneAndNoRollBeforeIncrementalTest(useMemProfiler, timeout).runExperiment(
    listOfExperiments,
    SmtSolver.CVC5,
    "QF_LIRA",
    1,
    "#808080",
    "CVC5 v$cvc5Version",
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda
  )
   */

  val outputFile = "${ChangedLaneAndNoRollBeforeIncrementalTest().expFolderPath}/graph_${getDateTimeString()}.png"
  plotPerf(resZ3, title = "ChangedLaneAndNoRollBefore incremental test", xLabel = "Cut level",
    legendPosition = LegendPosition.BEST, outputFile = outputFile, rmMemPlot = !useMemProfiler)
}

class ChangedLaneAndNoRollBeforeIncrementalTestArgs(parser: ArgParser) {
  val disableMemoryProfiler by parser.flagging("-D", "--disable_memory_profiler", help = "Disable memory profiler")
  val timeout by parser.storing("-T", "--timeout", help = "Specifies the timeout for the solver in seconds") {
    this.toInt()
  }.default(120)
}

fun main(args: Array<String>) = mainBody {
  ArgParser(args).parseInto(::ChangedLaneAndNoRollBeforeIncrementalTestArgs).run {
    runChangedLaneAndNoRollBeforeIncrementalTest(!disableMemoryProfiler, 60 * 5)
  }
}