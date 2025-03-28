@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.scripts

enum class LegendPosition {
  LOWER_LEFT, LOWER_RIGHT, UPPER_LEFT, UPPER_RIGHT, BEST;

  override fun toString() = super.toString().lowercase().replace('_', ' ')
}

/** @param outputFile If null the plot will not be saved but displayed on screen */
fun plotPerf(
    vararg files: String,
    width: Int? = null,
    height: Int? = null,
    rmMemPlot: Boolean = false,
    title: String? = null,
    xLabel: String? = null,
    legendPosition: LegendPosition? = null,
    outputFile: String? = null)
{
  var args = mutableListOf(*files)
  if (width != null) {
    args.addAll(0, listOf("-W", "$width"))
  }
  if (height != null) {
    args.addAll(0, listOf("-H", "$height"))
  }
  if (rmMemPlot) {
    args.add(0, "--rm_mem_plot")
  }
  if (title != null) {
    args.addAll(0, listOf("--title", "\"$title\""))
  }
  if (xLabel != null) {
    args.addAll(0, listOf("--x_label", "\"$xLabel\""))
  }
  if (legendPosition != null) {
    args.addAll(0, listOf("--legend_pos", "\"$legendPosition\""))
  }
  if (outputFile != null) {
    args.addAll(0, listOf("-S", outputFile))
  }
  val proc = PythonCommandLineWrapper.runScript("plotPerf.py", *args.toTypedArray())
  if (proc.exitValue() != 0) {
    throw RuntimeException(proc.inputReader().readText() + proc.errorReader().readText())
  }
}