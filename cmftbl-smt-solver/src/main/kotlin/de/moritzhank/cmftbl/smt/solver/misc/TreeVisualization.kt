@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.misc

import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.util.UUID

interface ITreeVisualizationNode {

  fun getTVNContent(): String

  fun getTVNColors(): Pair<String, String>? = null

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
  result.append("node [shape=box];")
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
      result.append("n${node.first} -> n$childID;")
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
  val encodedGraphviz = URLEncoder.encode(graphviz, "utf-8")
  val url = URI("https://quickchart.io/graphviz?graph=$encodedGraphviz").toURL()
  val imageData = url.readBytes()
  val imageFilePath = "$treeImgs${File.separator}${UUID.randomUUID()}.svg"
  File(imageFilePath).apply { writeBytes(imageData) }
}
