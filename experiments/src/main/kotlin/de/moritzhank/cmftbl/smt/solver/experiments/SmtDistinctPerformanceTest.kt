package de.moritzhank.cmftbl.smt.solver.experiments

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import de.moritzhank.cmftbl.smt.solver.SmtSolver
import de.moritzhank.cmftbl.smt.solver.misc.MemoryProfiler
import de.moritzhank.cmftbl.smt.solver.misc.avgWithoutInvalids
import de.moritzhank.cmftbl.smt.solver.scripts.LegendPosition
import de.moritzhank.cmftbl.smt.solver.scripts.getDateTimeString
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

fun runSmtDistinctPerformanceTest(useMemProfiler: Boolean = true, timeout: Int? = null) {
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
  val resCVC5 = SmtDistinctPerformanceTest(useMemProfiler, timeout).runExperiment(
    rangeOfDistinctStatements,
    SmtSolver.CVC5,
    "UF",
    1,
    "#808080",
    "CVC5 v$cvc5Version",
    runID,
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda
  )

  // Z3
  val z3Version = smtSolverVersion(SmtSolver.Z3)
  val resZ3 = SmtDistinctPerformanceTest(useMemProfiler, timeout).runExperiment(
    rangeOfDistinctStatements,
    SmtSolver.Z3,
    "UF",
    1,
    "#034B7B",
    "Z3 v$z3Version",
    runID,
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda
  )

  // YICES
  val yicesVersion = smtSolverVersion(SmtSolver.YICES)
  val resYices = SmtDistinctPerformanceTest(useMemProfiler, timeout).runExperiment(
    rangeOfDistinctStatements,
    SmtSolver.YICES,
    "UF",
    1,
    "#44B7C2",
    "Yices v$yicesVersion",
    runID,
    resTimeSLambda,
    resMaxSolverMemUsageGBLambda
  )
  val fSep = File.separator
  val outputFile = "${SmtDistinctPerformanceTest().getRunDirectoryPath(runID)}${fSep}graph_${getDateTimeString()}.png"
  plotPerf(resZ3, resYices, resCVC5, title = "Distinct Experiment", xLabel = "Unterschiedliche Individuen",
    legendPosition = LegendPosition.BEST, outputFile = outputFile, rmMemPlot = !useMemProfiler)
}

private class SmtDistinctPerformanceArgs(parser: ArgParser) {
  val disableMemoryProfiler by parser.flagging("-D", "--disable_memory_profiler", help = "Disable memory profiler")
  val timeout by parser.storing("-T", "--timeout", help = "Specifies the timeout for the solver in seconds") {
    this.toInt()
  }.default(null)
}

fun main(args: Array<String>) = mainBody {
  ArgParser(args).parseInto(::SmtDistinctPerformanceArgs).run {
    runSmtDistinctPerformanceTest(!disableMemoryProfiler, timeout)
  }
}
