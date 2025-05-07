package de.moritzhank.cmftbl.smt.solver

import de.moritzhank.cmftbl.smt.solver.scripts.LegendPosition
import de.moritzhank.cmftbl.smt.solver.scripts.plotPerf

private fun correctCSVFormatting() {
    val rowBeginnings = arrayOf(2, 501, 1001, 1500, 2000, 2500, 12758, 23017, 33275, 43534, 53793, 64051, 74310, 84568, 94827, 105086, 115344, 125603, 135862, 146120, 156379, 166637, 176896, 187155, 197413, 207672, 217931, 228189, 238448, 248706, 258965, 269224, 279482, 289741, 300000)
    println(rowBeginnings.size)
    val str = "2, 10, -1.00, -1, 0.01, -1501, 10, -1.00, -1, 0.01, -11001, 20, -1.00, -1, 0.02, -11500, 30, -1.00, -1, 0.03, -12000, 40, -1.00, -1, 0.04, -12500, 50, -1.00, -1, 0.05, -112758, 950, -1.00, -1, 0.95, -123017, 2810, -1.00, -1, 2.81, -133275, 6030, -1.00, -1, 6.03, -143534, 9860, -1.00, -1, 9.86, -153793, 14680, -1.00, -1, 14.68, -164051, 20860, -1.00, -1, 20.86, -174310, 29050, -1.00, -1, 29.05, -184568, 37620, -1.00, -1, 37.62, -194827, 48080, -1.00, -1, 48.08, -1105086, 60670, -1.00, -1, 60.67, -1115344, 78210, -1.00, -1, 78.21, -1125603, 103050, -1.00, -1, 103.05, -1135862, 119030, -1.00, -1, 119.03, -1146120, 150490, -1.00, -1, 150.49, -1156379, 179310, -1.00, -1, 179.31, -1166637, 210290, -1.00, -1, 210.29, -1176896, 255460, -1.00, -1, 255.46, -1187155, 280730, 0.02, 1992077312, 280.73, 1.9920773120000002197413, 322250, -1.00, -1, 322.25, -1207672, 362310, -1.00, -1, 362.31, -1217931, 411710, -1.00, -1, 411.71, -1228189, 454430, -1.00, -1, 454.43, -1238448, 515340, -1.00, 2038992896, 515.34, 2.0389928960000003248706, 565050, -1.00, -1, 565.05, -1258965, 621890, -1.00, -1, 621.89, -1269224, 697400, -1.00, -1, 697.4, -1279482, 767670, -1.00, -1, 767.67, -1289741, 876350, 0.02, 3890606080, 876.35, 3.8906060800000004300000, 890820, 0.02, 3897491456, 890.82, 3.897491456"
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

fun main() {
    val basePath = "/Users/moritzhaneke/Desktop/_experiment_07_05/smtDistinctPerf/2025-05-04-02-12-17/"
    var arrayOfFiles = arrayOf("cvc5", "mathsat", "yices", "z3")
    arrayOfFiles = arrayOfFiles.map { "$basePath$it/results.csv" }.toTypedArray()

    val outputFile = "$basePath/graph_tmp.png"
    plotPerf(*arrayOfFiles, title = "Distinct Experiment", xLabel = "Unterschiedliche Individuen",
        legendPosition = LegendPosition.BEST, outputFile = outputFile)
}