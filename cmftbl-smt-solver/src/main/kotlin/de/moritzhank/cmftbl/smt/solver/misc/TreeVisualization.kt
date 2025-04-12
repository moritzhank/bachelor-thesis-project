@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.misc

import de.moritzhank.cmftbl.smt.solver.translation.formula.emitsSomething
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.util.UUID
import kotlin.collections.ArrayDeque

interface ITreeVisualizationNode {

  fun getTVNContent(): String

  fun getTVNColors(): Pair<String, String>? = null

  fun getTVNEdgeStyle(): String? = null

  val children: List<ITreeVisualizationNode>

  fun copy(): ITreeVisualizationNode

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

  /**
   * Creates a new tree that only contains [remainingNodes] nodes (that emit something) in breath first order.
   * This works only if [ITreeVisualizationNode.children] is implemented as a [MutableList].
   */
  fun copyAndSlice(remainingNodes: Int): ITreeVisualizationNode  {
    val copy = copy()
    val breathFirstIteration = copy.iterator().asSequence().toList()
    val nodesThatCanBeRemoved = breathFirstIteration.filter { it.emitsSomething() }
    if (nodesThatCanBeRemoved.size - remainingNodes > 0) {
      val nodesToBeRemoved = nodesThatCanBeRemoved.subList(remainingNodes, nodesThatCanBeRemoved.size)
      breathFirstIteration.forEach {
        val parent = it
        (parent.children as MutableList<*>).removeIf {
          nodesToBeRemoved.contains(it)
        }
      }
    }
    return copy
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
    val entry = queue.removeFirst()
    val node = entry.second
    val colorsOfNode = node.getTVNColors()
    val colorAppendix = if (colorsOfNode == null) "" else ", color=${colorsOfNode.first}, fontcolor=${colorsOfNode.second}"
    result.append("n${entry.first} [label=<${node.getTVNContent()}>$colorAppendix];")
    node.children.forEach {
      val childID = nextId++
      queue.add(Pair(childID, it))
      val edgeStyle = node.getTVNEdgeStyle()
      val edgeStyleStr = if (edgeStyle == null) "" else " [style=\"$edgeStyle\"]"
      result.append("n${entry.first} -> n$childID$edgeStyleStr;")
    }
  }
  result.append("}")
  return result.toString()
}

/** Generate an SVG of the input tree by calling quickchart.io with [graphviz]. */
fun renderTree(graphviz: String, deletePrevSvgs: Boolean = true, fileName: String? = null) {
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
  val name = fileName ?: UUID.randomUUID().toString()
  val imageFilePath = "$treeImgs${File.separator}${name}.svg"
  File(imageFilePath).apply { writeBytes(imageData) }
}
