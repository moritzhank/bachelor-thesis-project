package de.moritzhank.cmftbl.smt.solver

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import de.moritzhank.cmftbl.smt.solver.experiments.runChangedLaneAndNoRollBeforeIncrementalTest
import de.moritzhank.cmftbl.smt.solver.experiments.runSmtDistinctPerformanceTest

private val availableExperiments = mutableMapOf<String, (Boolean, Int?, String) -> Unit>().apply {
  put("ChangedLaneAndNoRollBeforeIncremental") { useMemProfiler, timeout, params ->
    runChangedLaneAndNoRollBeforeIncrementalTest(useMemProfiler, timeout, params)
  }
  put("SmtDistinctPerformance") { useMemProfiler, timeout, params ->
    runSmtDistinctPerformanceTest(useMemProfiler, timeout)
  }
}

private class ExperimentArgs(parser: ArgParser) {
  val disableMemoryProfiler by parser.flagging("-D", "--disable_memory_profiler", help = "Disable memory profiler")
  val timeout by parser.storing("-T", "--timeout", help = "Specifies the timeout for the solver in seconds") {
    this.toInt()
  }.default(null)
  val experimentName by parser.storing("--experiment", help = "Specifies the experiment name")
  val experimentParams by parser.storing("--params", help = "Specifies the parameters for the experiment").default("")
}

fun main(args: Array<String>) = mainBody {
  ArgParser(args).parseInto(::ExperimentArgs).run {
    val experiment = availableExperiments[experimentName]
    requireNotNull(experiment) { "Experiment $experimentName does not exist." }
    experiment.invoke(!disableMemoryProfiler, timeout, experimentParams)
  }
}
