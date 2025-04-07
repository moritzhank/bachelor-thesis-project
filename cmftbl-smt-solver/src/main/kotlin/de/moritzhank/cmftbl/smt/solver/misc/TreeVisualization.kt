@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.misc

import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.collections.Iterator
import kotlin.collections.List
import kotlin.collections.forEach
import kotlin.collections.isNotEmpty

interface ITreeVisualizationNode {

  fun getTVNContent(): String

  fun getTVNColors(): Pair<String, String>? = null

  fun getTVNEdgeStyle(): String? = null

  val children: List<ITreeVisualizationNode>

  /** Iterator that traverses the tree by breadth search.  */
  fun iterator() = object : Iterator<ITreeVisualizationNode> {

    private val queue = ArrayDeque<ITreeVisualizationNode>().apply {
      add(this@ITreeVisualizationNode)
    }

    override fun hasNext() = queue.isNotEmpty()

    override fun next(): ITreeVisualizationNode {
      val next = queue.removeFirst()
      next.children.forEach {
        queue.add(it)
      }
      return next
    }
  }

}

fun ITreeVisualizationNode.generateGraphvizCode(): String {
  val result = StringBuilder()
  result.append("digraph G {")
  result.append("node [shape=plaintext];")
  val queue =
      ArrayDeque<Pair<Int, ITreeVisualizationNode>>().apply {
        add(Pair(0, this@generateGraphvizCode))
      }
  var nextId = 1
  while (queue.isNotEmpty()) {
    val node = queue.removeFirst()
    val colorsOfNode = node.second.getTVNColors()
    val colorAppendix = if (colorsOfNode == null) "" else ", color=${colorsOfNode.first}, fontcolor=${colorsOfNode.second}"
    result.append("n${node.first} [label=<${node.second.getTVNContent()}>$colorAppendix];")
    node.second.children.forEach {
      val childID = nextId++
      queue.add(Pair(childID, it))
      val edgeStyle = it.getTVNEdgeStyle()
      val edgeStyleStr = if (edgeStyle == null) "" else " [style=\"$edgeStyle\"]"
      result.append("n${node.first} -> n$childID$edgeStyleStr;")
    }
  }
  result.append("}")
  return result.toString()
}

/** Generate an SVG of the input tree by calling quickchart.io with [graphviz]. */
fun renderTree(graphviz: String, deletePrevSvgs: Boolean = true) {
  val treeImgs = getAbsolutePathFromProjectDir("_treeSvgs")
  File(treeImgs)
      .apply {
        if (deletePrevSvgs) {
          deleteRecursively()
        }
      }
      .mkdir()
  val content = graphviz.replace("\"", "\\\"")
  val jsonRequestBody = "{\"graph\": \"$content\",\"layout\": \"dot\",\"format\": \"svg\"}"
  val url = URI("https://quickchart.io/graphviz").toURL()
  val con = url.openConnection() as HttpURLConnection
  con.requestMethod = "POST"
  con.setRequestProperty("Content-Type", "application/json")
  con.doOutput = true
  OutputStreamWriter(con.outputStream).use {
    it.write(jsonRequestBody)
    it.flush()
  }
  val imageData = con.inputStream.readBytes()
  val imageFilePath = "$treeImgs${File.separator}${UUID.randomUUID()}.svg"
  File(imageFilePath).apply { writeBytes(imageData) }
}
