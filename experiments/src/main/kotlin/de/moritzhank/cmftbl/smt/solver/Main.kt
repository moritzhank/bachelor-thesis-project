package de.moritzhank.cmftbl.smt.solver

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import de.moritzhank.cmftbl.smt.solver.experiments.runChangedLaneAndNoRollBeforeIncrementalTest
import de.moritzhank.cmftbl.smt.solver.experiments.runSmtDistinctPerformanceTest

private val availableExperiments = mutableMapOf<String, (Boolean, Int?, Int, String) -> Unit>().apply {
  put("ChangedLaneAndNoRollBeforeIncremental") { useMemProfiler, timeout, reps, params ->
    runChangedLaneAndNoRollBeforeIncrementalTest(useMemProfiler, timeout, reps, params)
  }
  put("SmtDistinctPerformance") { useMemProfiler, timeout, reps, params ->
    runSmtDistinctPerformanceTest(useMemProfiler, timeout, reps)
  }
}

private class ExperimentArgs(parser: ArgParser) {
  val disableMemoryProfiler by parser.flagging("-D", "--disable_memory_profiler", help = "Disable memory profiler")
  val listElements by parser.flagging("--list", help = "Lists all available experiments")
  val timeout by parser.storing("-T", "--timeout", help = "Specifies the timeout for the solver in seconds") {
    this.toInt()
  }.default(null)
  val experimentName by parser.storing("--experiment", help = "Specifies the experiment name").default("")
  val experimentParams by parser.storing("--params", help = "Specifies the parameters for the experiment").default("")
  val repetitions by parser.storing("--repetitions", help = "Specifies the number of repetitions") {
    this.toInt()
  }.default(1)
}

fun main(args: Array<String>) = mainBody {
  ArgParser(args).parseInto(::ExperimentArgs).run {
    if (listElements) {
      println("Available experiments:")
      availableExperiments.keys.forEach {
        println(it)
      }
      return@mainBody
    }
    if (experimentName.isEmpty()) {
      println("You need to specify an experiment name. " +
              "A list of available experiments can be retrieved with --list.")
      return@mainBody
    }
    if (repetitions <= 0) {
      println("The specified number of repetitions must be greater than zero.")
      return@mainBody
    }
    val experiment = availableExperiments[experimentName]
    if (experiment == null) {
      println("The Experiment $experimentName does not exist. " +
              "A list of available experiments can be retrieved with --list.")
      return@mainBody
    }
    experiment.invoke(!disableMemoryProfiler, timeout, repetitions, experimentParams)
  }
}
