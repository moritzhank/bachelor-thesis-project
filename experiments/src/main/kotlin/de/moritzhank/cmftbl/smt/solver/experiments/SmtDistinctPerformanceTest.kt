package de.moritzhank.cmftbl.smt.solver.experiments

import de.moritzhank.cmftbl.smt.solver.SmtSolver
import de.moritzhank.cmftbl.smt.solver.misc.*
import de.moritzhank.cmftbl.smt.solver.scripts.LegendPosition
import de.moritzhank.cmftbl.smt.solver.scripts.linSpaceArr
import de.moritzhank.cmftbl.smt.solver.scripts.plotPerf
import de.moritzhank.cmftbl.smt.solver.smtSolverVersion
import java.io.File

private class SmtDistinctPerformanceSetup(override val identifier: Int) : PerfExperimentSetup {

  override fun toString(): String {
    return "SmtDistinctPerf with $identifier distinct individuals"
  }

}

private class SmtDistinctPerformanceTest(useMemProfiler: Boolean = true, timeout: Int? = null):
  PerfExperiment<SmtDistinctPerformanceSetup>("SmtDistinctPerf") {

  init {
    memoryProfilerSampleRateMs = 10
    useMemoryProfiler = useMemProfiler
    timeOutInSeconds = timeout
    fileName = { expSetup -> "distinctIndividuals_${expSetup.identifier}.smt2" }
  }

  override val memoryProfilerWorkingCond: (MemoryProfiler) -> Boolean = { memProfiler ->
    memProfiler.maxProcMemUsageBytes != -1L &&
            memProfiler.maxSysMemUsagePercent != -1.0 &&
            memProfiler.numSamples > 5
  }

  override fun generateSmtLib(
    expSetup: SmtDistinctPerformanceSetup,
    solver: SmtSolver,
    logic: String
  ): String {
    val result = StringBuilder()
    result.appendLine("(set-logic $logic)")
    result.appendLine("(declare-sort TestSort 0)")
    for (i in 1..expSetup.identifier) {
      result.appendLine("(declare-const ind_$i TestSort)")
    }
    result.append("(assert (distinct ")
    for (i in 1..expSetup.identifier) {
      result.append("ind_$i ")
    }
    result.appendLine("))")
    result.appendLine("(check-sat)")
    return result.toString()
  }

}

fun runSmtDistinctPerformanceTest(useMemProfiler: Boolean = true, timeout: Int? = null, reps: Int) {
  val repsAddition = if (reps > 1) " (avg. ${reps}x)" else ""
  val resMaxSolverMemUsageGBLambda: (List<Long>) -> String = { list ->
    val avg = list.avgWithoutInvalids()
    if (avg == -1L) "-1" else "${MemoryProfiler.bytesToGB(avg)}"
  }
  val resTimeSLambda : (Array<Long>) -> String = { arr ->
    (1.0 * (arr.fold(0L) { acc, elem -> acc + elem }) / (arr.size * 1_000L)).toString()
  }

  // Setup
  var rangeOfDistinctStatements = linSpaceArr(2, 2_000, 5).map { SmtDistinctPerformanceSetup(it) }.toMutableList()
  rangeOfDistinctStatements.addAll(linSpaceArr(2_500, 300_000, 30).map { SmtDistinctPerformanceSetup(it) })
  val runID = getDateTimeString('-', '-', "-", false)

  // CVC5
  val cvc5Version = smtSolverVersion(SmtSolver.CVC5)
  val cvc5Dir = SmtDistinctPerformanceTest().getRunDirectoryPath(runID, SmtSolver.CVC5)
  val resCVC5 = SmtDistinctPerformanceTest(useMemProfiler, timeout).apply {
    logger = Logger.new("$cvc5Dir${File.separator}log.txt")
  }.runExperiment(
    rangeOfDistinctStatements,
    SmtSolver.CVC5,
    "QF_UF",
    reps,
    diagramColor(SmtSolver.CVC5),
    "CVC5 v$cvc5Version$repsAddition",
    runID,
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda
  )

  // MathSAT
  val mathSATVersion = smtSolverVersion(SmtSolver.MATHSAT)
  val mathSATDir = SmtDistinctPerformanceTest().getRunDirectoryPath(runID, SmtSolver.MATHSAT)
  val resMathSAT = SmtDistinctPerformanceTest(useMemProfiler, timeout).apply {
    logger = Logger.new("$mathSATDir${File.separator}log.txt")
  }.runExperiment(
    rangeOfDistinctStatements,
    SmtSolver.MATHSAT,
    "QF_UF",
    reps,
    diagramColor(SmtSolver.MATHSAT),
    "MathSAT v$mathSATVersion$repsAddition",
    runID,
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda
  )

  // Z3
  val z3Version = smtSolverVersion(SmtSolver.Z3)
  val z3Dir = SmtDistinctPerformanceTest().getRunDirectoryPath(runID, SmtSolver.Z3)
  val resZ3 = SmtDistinctPerformanceTest(useMemProfiler, timeout).apply {
    logger = Logger.new("$z3Dir${File.separator}log.txt")
  }.runExperiment(
    rangeOfDistinctStatements,
    SmtSolver.Z3,
    "QF_UF",
    reps,
    diagramColor(SmtSolver.Z3),
    "Z3 v$z3Version$repsAddition",
    runID,
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda
  )

  // YICES
  val yicesVersion = smtSolverVersion(SmtSolver.YICES)
  val yicesDir = SmtDistinctPerformanceTest().getRunDirectoryPath(runID, SmtSolver.YICES)
  val resYices = SmtDistinctPerformanceTest(useMemProfiler, timeout).apply {
    logger = Logger.new("$yicesDir${File.separator}log.txt")
  }.runExperiment(
    rangeOfDistinctStatements,
    SmtSolver.YICES,
    "QF_UF",
    reps,
    diagramColor(SmtSolver.YICES),
    "Yices v$yicesVersion$repsAddition",
    runID,
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda
  )

  val fSep = File.separator
  val outputFile = "${SmtDistinctPerformanceTest().getRunDirectoryPath(runID)}${fSep}graph_${getDateTimeString()}.png"
  plotPerf(resZ3, resYices, resCVC5, resMathSAT, title = "Distinct Experiment", xLabel = "Unterschiedliche Individuen",
    legendPosition = LegendPosition.BEST, outputFile = outputFile, rmMemPlot = !useMemProfiler)
}
