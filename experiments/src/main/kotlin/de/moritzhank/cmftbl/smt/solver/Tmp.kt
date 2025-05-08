package de.moritzhank.cmftbl.smt.solver

import de.moritzhank.cmftbl.smt.solver.scripts.LegendPosition
import de.moritzhank.cmftbl.smt.solver.scripts.plotPerf
import java.io.File

private fun correctCSVFormatting() {
    //val rowBeginnings = arrayOf(2, 501, 1001, 1500, 2000, 2500, 12758, 23017, 33275, 43534, 53793, 64051, 74310, 84568, 94827, 105086, 115344, 125603, 135862, 146120, 156379, 166637, 176896, 187155, 197413, 207672, 217931, 228189, 238448, 248706, 258965, 269224, 279482, 289741, 300000)
    val rowBeginnings = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
    println(rowBeginnings.size)
    val str = "1, 7160, -1.00, -1, 7.16, -12, 8180, 0.21, 3667898368, 8.18, 3.66789836800000043, 8110, -1.00, -1, 8.11, -14, 8140, 0.21, 3668041728, 8.14, 3.66804172800000045, 642010, 0.21, 1827024896, 642.01, 1.82702489600000026, 748500, 0.22, 3767279616, 748.5, 3.7672796167, 642790, 0.23, 3733008384, 642.79, 3.7330083848, 663740, -1.00, -1, 663.74, -19, 666830, 0.28, 3697270784, 666.83, 3.69727078410, 655260, 0.27, 3699159040, 655.26, 3.69915904"
    val split = str.split(", ").toMutableList()
    print(split.size)
    var result = ""
    var index = 0
    while(split.isNotEmpty()) {
        repeat(5) {
            result += "${split.removeFirst()}, "
        }
        val pivot = split.removeFirst()
        if (split.size == 0) {
            result += pivot
        } else {
            val nextBeginning = rowBeginnings[++index].toString()
            result += "${pivot.removeSuffix(nextBeginning)}${System.lineSeparator()}"
            split.add(0, nextBeginning)
        }
    }
    println("============ [Ergebnis] ============")
    println(result)
    println("====================================")
}

private fun plotOther() {
    val basePath = "/Users/moritzhaneke/Desktop/_experiment_07_05/changedLaneAndNoRollBeforeIncremental/2025-05-04-02-08-02"
    var arrayOfFiles = arrayOf("cvc5", "mathsat", "yices", "z3")
    arrayOfFiles = arrayOfFiles.map { "$basePath$it/results.csv" }.toTypedArray()
    val outputFile = "$basePath/graph_tmp.png"
    plotPerf(arrayOfFiles[0], arrayOfFiles[3], title = "Anderes", xLabel = "KP",
        legendPosition = LegendPosition.BEST, outputFile = outputFile)
}

fun main() {
    //correctCSVFormatting()
    plotOther()
    return
    val basePath = "/Users/moritzhaneke/Desktop/_experiment_07_05/smtDistinctPerf/2025-05-04-02-12-17/"
    var arrayOfFiles = arrayOf("cvc5", "mathsat", "yices", "z3")
    arrayOfFiles = arrayOfFiles.map { "$basePath$it/results.csv" }.toTypedArray()

    // Subplot
    val newBasePath = "${basePath}_subplot/"
    File(newBasePath).apply { deleteRecursively() }.mkdirs()
    var arrayOfNewFiles = arrayOf("cvc5", "mathsat", "yices", "z3")
    arrayOfNewFiles = arrayOfNewFiles.map { "${newBasePath}results_$it.csv" }.toTypedArray()
    arrayOfFiles.forEachIndexed { i, e ->
        val newFile = File(arrayOfNewFiles[i])
        File(e).copyTo(newFile)
        var dataLines = newFile.readText().lines().filter { it.isNotEmpty() }.toMutableList()
        val header = dataLines.take(14)
        dataLines = dataLines.drop(14).toMutableList()
        val lowerBound = 0
        val upperBound = Math.min(6, dataLines.size)
        if (lowerBound > dataLines.size) {
            dataLines = mutableListOf()
        } else {
            dataLines = dataLines.subList(lowerBound, upperBound).toMutableList()
        }
        dataLines.addAll(0, header)
        val newFileContent = dataLines.joinToString(System.lineSeparator())
        newFile.writeText(newFileContent)
    }
    plotPerf(*arrayOfNewFiles, title = "Distinct Experiment", xLabel = "Unterschiedliche Individuen",
        legendPosition = LegendPosition.BEST, outputFile = "$newBasePath/graph.png", rmMemPlot = true)

    val outputFile = "$basePath/graph_tmp.png"
    plotPerf(*arrayOfFiles, title = "Distinct Experiment", xLabel = "Unterschiedliche Individuen",
        legendPosition = LegendPosition.BEST, outputFile = outputFile)
}