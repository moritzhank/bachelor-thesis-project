@file:Suppress("MemberVisibilityCanBePrivate")

package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.dsl.Evaluable
import de.moritzhank.cmftbl.smt.solver.dsl.Formula
import de.moritzhank.cmftbl.smt.solver.dsl.Term
import de.moritzhank.cmftbl.smt.solver.misc.ITreeVisualizationNode
import de.moritzhank.cmftbl.smt.solver.misc.str

/** Abstraction of the translation process of formula AST. */
internal interface IEvalNode: ITreeVisualizationNode {
  /** This node ID should be set automatically based on [evalCtx]. */
  val nodeID: Int?
  override val children: MutableList<IEvalNode>
  val evalCtx: EvaluationContext
  val emissions: MutableList<IEmission>
  /** The satisfiability of the child nodes is not mandatory for this node. */
  var childSatNotRequired: Boolean

  override fun getTVNEdgeStyle(): String? {
    return if (childSatNotRequired) "dashed" else null
  }

  /** Iterator that traverses the tree by breadth search.  */
  override fun iterator() = object : Iterator<IEvalNode> {

    private val queue = ArrayDeque<IEvalNode>().apply {
      add(this@IEvalNode)
    }

    override fun hasNext() = queue.isNotEmpty()

    override fun next(): IEvalNode {
      val next = queue.removeFirst()
      next.children.forEach {
        queue.add(it)
      }
      return next
    }
  }

}

/** Node just for organisation. */
internal class OrgaEvalNode(
  override val children: MutableList<IEvalNode>,
  override val evalCtx: EvaluationContext,
  val content: String,
  override val nodeID: Int? = evalCtx.genConstraintID()
): IEvalNode {

  override val emissions: MutableList<IEmission> = mutableListOf()
  override var childSatNotRequired = false

  override fun getTVNContent(): String {
    return "<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">" +
            "<TR>" +
            "<TD BGCOLOR=\"lightgray\">$nodeID</TD>" +
            "<TD BGCOLOR=\"lightgray\">ORGA $content</TD>" +
            "</TR>" +
            "</TABLE>"
  }

  /** Create an exact copy. */
  override fun copy(): ITreeVisualizationNode {
    val copiedChildren = mutableListOf<IEvalNode>()
    children.forEach {
      copiedChildren.add(it.copy() as IEvalNode)
    }
    val childSatNotRequiredCopy = childSatNotRequired
    return OrgaEvalNode(copiedChildren, evalCtx.copy(), content, nodeID).apply {
      childSatNotRequired = childSatNotRequiredCopy
    }
  }

}

/** Further abstraction that contains an [evaluable]. */
internal sealed interface IEvalNodeWithEvaluable: IEvalNode {
  /** Reference to the term or formula that is being evaluated. */
  val evaluable: Evaluable
}

/** Represents the evaluation of a formula or term at a fixed tick. */
internal class EvalNode(
  override val children: MutableList<IEvalNode>,
  override val evalCtx: EvaluationContext,
  override val emissions: MutableList<IEmission>,
  override val evaluable: Evaluable,
  /** Defines which tick is evaluated. */
  val evaluatedTickIndex: Int,
  /** Purpose is the better readability of the tree. */
  var annotation: String? = null,
  /** If null, nothing happens. Overwrites with null if -1 or with the value otherwise. */
  overwriteNodeID: Int? = null
): IEvalNodeWithEvaluable {

  override val nodeID: Int? = if (overwriteNodeID == null) {
    if (evaluable !is Term<*>) evalCtx.genConstraintID() else null
  } else {
    overwriteNodeID.takeIf { it >= 0 }
  }
  override var childSatNotRequired = false

  override fun getTVNContent(): String {
    val annotationStr = if (annotation == null) "" else "<TR><TD COLSPAN=\"3\"><I>$annotation</I></TD></TR>"
    var emissionsStr = ""
    emissions.forEach {
      emissionsStr += it.tableStr()
    }
    val rows = annotationStr + emissionsStr
    return getTVNTableString(nodeID, "EVAL @ $evaluatedTickIndex", evaluable::class.simpleName!!, rows)
  }

  /** Create an exact copy. */
  override fun copy(): ITreeVisualizationNode {
    val copiedChildren = mutableListOf<IEvalNode>()
    children.forEach {
      copiedChildren.add(it.copy() as IEvalNode)
    }
    return EvalNode(
      copiedChildren,
      evalCtx.copy(),
      emissions.toMutableList(),
      evaluable,
      evaluatedTickIndex,
      annotation,
      nodeID
    )
  }

}

/** Represents the evaluation of a formula or term in an interval in order to find a witness. */
internal class WitnessEvalNode(
  override val children: MutableList<IEvalNode>,
  override val evalCtx: EvaluationContext,
  override val emissions: MutableList<IEmission>,
  override val evaluable: Evaluable,
  /** Defines **relative** search "radius". */
  val interval: Pair<Double, Double>,
  /** Purpose is the better readability of the tree. */
  var annotation: String? = null,
  /** If null, nothing happens. Overwrites with null if -1 or with the value otherwise. */
  overwriteNodeID: Int? = null
) : IEvalNodeWithEvaluable {

  override val nodeID: Int? = if (overwriteNodeID == null) {
    if (evaluable !is Term<*>) evalCtx.genConstraintID() else null
  } else {
    overwriteNodeID.takeIf { it >= 0 }
  }
  override var childSatNotRequired = false

  override fun getTVNContent(): String {
    val annotationStr = if (annotation == null) "" else "<TR><TD COLSPAN=\"3\"><I>$annotation</I></TD></TR>"
    var emissionsStr = ""
    emissions.forEach {
      emissionsStr += it.tableStr()
    }
    val rows = annotationStr + emissionsStr
    return getTVNTableString(nodeID, "WTNS in ${interval.str()}", evaluable::class.simpleName!!, rows)
  }

  /** Create an exact copy. */
  override fun copy(): ITreeVisualizationNode {
    val copiedChildren = mutableListOf<IEvalNode>()
    children.forEach {
      copiedChildren.add(it.copy() as IEvalNode)
    }
    val childSatNotRequiredCopy = childSatNotRequired
    return WitnessEvalNode(
      copiedChildren,
      evalCtx.copy(),
      emissions.toMutableList(),
      evaluable,
      interval.copy(),
      annotation,
      nodeID
    ).apply {
      childSatNotRequired = childSatNotRequiredCopy
    }
  }

}

/** Represents the introduction of a variable that is needed for the translation process but is not part of the AST. */
internal class VarIntroNode(
  override val children: MutableList<IEvalNode>,
  override val evalCtx: EvaluationContext,
  /** SMT-ID of the emitted variable. */
  val emittedID: String,
  /** Reference to the [CCB] the variable is based on. */
  val referenceCCB: CCB<*>,
  /** Asserted ID of the emitted variable of the underlying [tools.aqua.stars.core.types.EntityType]. */
  val assertedID: Int,
  /** Defines which tick is evaluated. */
  val evaluatedTickIndex: Int,
  /** Defines which interval is evaluated. */
  val evaluatedInterval: Pair<Double, Double>?,
  /**
   * The precondition describes a constraint on the evaluated interval: precondition => phi.
   * This is needed, because certain constraints on the evaluation interval are not known before the evaluation.
   */
  val tickPrecondition: EvaluationTickPrecondition?,
  /** Changes the emission of [EvalInIntervalConstraintEmission] and [EvalAtTickConstraintEmission]. */
  val sameTimeAs: String?,
  val sameTimeAsCCB: CCB<*>?,
  override val nodeID: Int = evalCtx.constraintIDGenerator.generateID()
): IEvalNode {

  /**
   * The emissions of [VarIntroNode] are automatically generated (with IDs from [evalCtx]) and contain the
   * following emissions:
   * - [NewInstanceEmission] with [emittedID]
   * - [ConstrainIDEmission] with [emittedID] and [assertedID]
   * - [EvalInIntervalConstraintEmission] with [emittedID] and [evaluatedInterval] or
   * - [EvalAtTickConstraintEmission] with [emittedID] and [evaluatedTickIndex] or
   * - [SameTimeEmission] with [sameTimeAs] and [sameTimeAsCCB]
   */
  override val emissions = mutableListOf<IEmission>()

  override var childSatNotRequired = false

  init {
    emissions.add(NewInstanceEmission(emittedID))
    emissions.add(ConstrainIDEmission(evalCtx.genConstraintID(), emittedID, assertedID))
    if (sameTimeAs != null) {
      emissions.add(SameTimeEmission(evalCtx.genConstraintID(), sameTimeAs, sameTimeAsCCB!!))
    } else {
      if (evaluatedInterval != null) {
        emissions.add(EvalInIntervalConstraintEmission(evalCtx.genConstraintID(), emittedID, evaluatedInterval))
      } else {
        emissions.add(EvalAtTickConstraintEmission(evalCtx.genConstraintID(), emittedID, evaluatedTickIndex))
      }
    }
  }

  override fun getTVNContent(): String {
    val tickPrecondStr = if (tickPrecondition == null) "" else "<TR><TD COLSPAN=\"3\">TickPrecond: " +
            "${tickPrecondition.toHTMLString()}</TD></TR>"
    var emissionsStr = ""
    emissions.forEach {
      emissionsStr += it.tableStr()
    }
    return getTVNTableString(nodeID, "VAR_INTRO for $referenceCCB", tickPrecondStr + emissionsStr)
  }

  /** Create an exact copy. */
  override fun copy(): ITreeVisualizationNode {
    val copiedChildren = mutableListOf<IEvalNode>()
    children.forEach {
      copiedChildren.add(it.copy() as IEvalNode)
    }
    val emissionsRef = emissions
    val childSatNotRequiredCopy = childSatNotRequired
    return VarIntroNode(
      copiedChildren,
      evalCtx.copy(),
      emittedID,
      referenceCCB,
      assertedID,
      evaluatedTickIndex,
      evaluatedInterval?.copy(),
      tickPrecondition?.copy(),
      sameTimeAs,
      sameTimeAsCCB,
      nodeID
    ).apply {
      emissions.clear()
      emissions.addAll(emissionsRef)
      childSatNotRequired = childSatNotRequiredCopy
    }
  }

}

/** Is used as an intermediate step. */
internal class UniversalEvalNode(
  override val evalCtx: EvaluationContext,
  /** Reference to the formula. */
  val evaluable: Formula,
  /** Defines which tick is evaluated. */
  val evaluatedTickIndex: Int,
  /**
   * This induces the largest (or smallest) tick that has to be instantiated and that possibly can be sat if the
   * [tickPrecondition] allows this.
   */
  val tickPrecondition: EvaluationTickPrecondition?,
  val interval: Pair<Double, Double>
) : IEvalNode {

  override val nodeID: Int = evalCtx.constraintIDGenerator.generateID()
  override val children: MutableList<IEvalNode> = mutableListOf()
  override val emissions: MutableList<IEmission> = mutableListOf()
  override var childSatNotRequired = false

  override fun getTVNContent(): String {
    val tickPrecondStr = if (tickPrecondition == null) "" else "<TR><TD COLSPAN=\"3\">TickPrecond: " +
            "${tickPrecondition.toHTMLString()}</TD></TR>"
    return getTVNTableString(nodeID, "UNIV in ${interval.str()}", evaluable::class.simpleName!!, tickPrecondStr)
  }

  /** Create an exact copy. */
  override fun copy(): ITreeVisualizationNode {
    val copiedChildren = mutableListOf<IEvalNode>()
    children.forEach {
      copiedChildren.add(it.copy() as IEvalNode)
    }
    val childSatNotRequiredCopy = childSatNotRequired
    return UniversalEvalNode(
      evalCtx.copy(),
      evaluable,
      evaluatedTickIndex,
      tickPrecondition?.copy(),
      interval.copy()
    ).apply {
      childSatNotRequired = childSatNotRequiredCopy
    }
  }

}

private fun getTVNTableString(nodeID: Int?, modeStr: String, titleStr: String, rows: String) : String {
  val nodeIDRow = if (nodeID != null) "<TD BGCOLOR=\"lightgray\">$nodeID</TD>" else ""
  val colSpan = if (nodeID == null) "COLSPAN=\"2\" " else ""
  return "<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">" +
          "<TR>" +
            nodeIDRow +
            "<TD ${colSpan}BGCOLOR=\"lightgray\">$modeStr</TD>" +
            "<TD BGCOLOR=\"lightgray\"><B>$titleStr</B></TD>" +
          "</TR>" +
          rows +
          "</TABLE>"
}

private fun getTVNTableString(nodeID: Int, titleStr: String, rows: String) : String {
  return "<TABLE BORDER=\"0\" CELLBORDER=\"1\" CELLSPACING=\"0\">" +
          "<TR>" +
          "<TD BGCOLOR=\"lightgray\">$nodeID</TD>" +
          "<TD COLSPAN=\"2\" BGCOLOR=\"lightgray\">$titleStr</TD>" +
          "</TR>" +
          rows +
          "</TABLE>"
}

/** Does node emit something? */
fun ITreeVisualizationNode.emitsSomething(): Boolean {
  require(this is IEvalNode)
  return this.nodeID != null
}
