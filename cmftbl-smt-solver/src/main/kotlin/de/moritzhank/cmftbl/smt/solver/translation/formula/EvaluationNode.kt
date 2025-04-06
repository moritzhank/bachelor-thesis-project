package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.dsl.Evaluable
import de.moritzhank.cmftbl.smt.solver.dsl.Formula
import de.moritzhank.cmftbl.smt.solver.dsl.Term
import de.moritzhank.cmftbl.smt.solver.dsl.str
import de.moritzhank.cmftbl.smt.solver.misc.ITreeVisualizationNode
import de.moritzhank.cmftbl.smt.solver.misc.check

/** Abstraction of the translation process of formula AST. */
internal interface IEvalNode: ITreeVisualizationNode {
  /** This node ID should be set automatically based on [evaluationContext]. */
  val nodeID: Int?
  override val children: MutableList<IEvalNode>
  val evaluationContext: EvaluationContext
  val emissions: MutableList<IEmission>

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
  override val evaluationContext: EvaluationContext,
  val content: String
): IEvalNode {

  override val nodeID: Int? = null
  override val emissions: MutableList<IEmission> = mutableListOf()

  override fun getTVNContent(): String {
    return "ORGA $content"
  }

  override fun getTVNColors(): Pair<String, String>? = Pair("black", "gray")

}

/** Further abstraction that contains an [evaluable]. */
internal sealed interface IEvalNodeWithEvaluable: IEvalNode {
  /** Reference to the term or formula that is being evaluated. */
  val evaluable: Evaluable
}

/** Represents the evaluation of a formula or term at a fixed tick. */
internal class EvalNode(
  override val children: MutableList<IEvalNode>,
  override val evaluationContext: EvaluationContext,
  override val emissions: MutableList<IEmission>,
  override val evaluable: Evaluable,
  /** Defines which tick is evaluated. */
  val evaluatedTickIndex: Int,
  /**
   * The precondition describes a constraint on the evaluated interval: precondition => phi.
   * This is needed, because certain constraints on the evaluation interval are not known before the evaluation.
   */
  val tickPrecondition: EvaluationTickPrecondition?,
  /** Purpose is the better readability of the tree. */
  var annotation: String? = null
): IEvalNodeWithEvaluable {

  override val nodeID: Int? = if (evaluable !is Term<*>) evaluationContext.constraintIDGenerator.generateID() else null

  override fun getTVNContent(): String {
    val annotationStr = if (annotation == null) "" else "<TR><TD COLSPAN=\"3\"><I>$annotation</I></TD></TR>"
    val tickPrecondStr = if (tickPrecondition == null) "" else "<TR><TD COLSPAN=\"3\">TickPrecond: " +
            "${tickPrecondition.toHTMLString()}</TD></TR>"
    var emissionsStr = ""
    emissions.forEach {
      emissionsStr += it.tableStr()
    }
    val rows = annotationStr + tickPrecondStr + emissionsStr
    return getTVNTableString(nodeID, "EVAL @ $evaluatedTickIndex", evaluable::class.simpleName!!, rows)
  }

}

/** Represents the evaluation of a formula or term in an interval in order to find a witness. */
internal class WitnessEvalNode(
  override val children: MutableList<IEvalNode>,
  override val evaluationContext: EvaluationContext,
  override val emissions: MutableList<IEmission>,
  override val evaluable: Evaluable,
  /** Defines **relative** search "radius". */
  val interval: Pair<Int, Int>?,
  /**
   * The precondition describes a constraint on the evaluated interval: precondition => phi.
   * This is needed, because certain constraints on the evaluation interval are not known before the evaluation.
   * Preconditions can occur in witness mode, for formulae like (phi1 until (phi2 until phi3))
   */
  val tickPrecondition: EvaluationTickPrecondition?,
  /** Purpose is the better readability of the tree. */
  var annotation: String? = null
) : IEvalNodeWithEvaluable {

  override val nodeID: Int = evaluationContext.constraintIDGenerator.generateID()

  init {
    interval.check()
  }

  override fun getTVNContent(): String {
    val annotationStr = if (annotation == null) "" else "<TR><TD COLSPAN=\"3\"><I>$annotation</I></TD></TR>"
    val tickPrecondStr = if (tickPrecondition == null) "" else "<TR><TD COLSPAN=\"3\">TickPrecond: " +
            "${tickPrecondition.toHTMLString()}</TD></TR>"
    var emissionsStr = ""
    emissions.forEach {
      emissionsStr += it.tableStr()
    }
    val rows = annotationStr + tickPrecondStr + emissionsStr
    return getTVNTableString(nodeID, "WTNS in ${interval.str()}", evaluable::class.simpleName!!, rows)
  }

}

/** Represents the introduction of a variable that is needed for the translation process but is not part of the AST. */
internal class VarIntroNode(
  override val children: MutableList<IEvalNode>,
  override val evaluationContext: EvaluationContext,
  /** SMT-ID of the emitted variable. */
  val emittedID: String,
  /** Reference to the [CCB] the variable is based on. */
  val referenceCCB: CCB<*>,
  /** Asserted ID of the emitted variable of the underlying [tools.aqua.stars.core.types.EntityType]. */
  val assertedID: Int,
  /** Defines which tick is evaluated. */
  val evaluatedTickIndex: Int,
  /** Defines which interval is evaluated. */
  val evaluatedInterval: Pair<Int, Int>?
): IEvalNode {

  override val nodeID: Int = evaluationContext.constraintIDGenerator.generateID()

  /**
   * The emissions of [VarIntroNode] are automatically generated (with IDs from [evaluationContext]) and contain the
   * following emissions:
   * - [NewInstanceEmission] with [emittedID]
   * - [ConstrainIDEmission] with [emittedID] and [assertedID]
   * - [EvalInIntervalConstraintEmission] with [emittedID] and [evaluatedInterval] or
   * - [EvalAtTickConstraintEmission] with [emittedID] and [evaluatedTickIndex]
   */
  override val emissions = mutableListOf<IEmission>()

  init {
    val cIDGenerator = evaluationContext.constraintIDGenerator
    emissions.add(NewInstanceEmission(null, emittedID))
    emissions.add(ConstrainIDEmission(cIDGenerator.generateID(), emittedID, assertedID))
    if (evaluatedInterval != null) {
      evaluatedInterval.check()
      emissions.add(EvalInIntervalConstraintEmission(cIDGenerator.generateID(), emittedID, evaluatedInterval))
    } else {
      emissions.add(EvalAtTickConstraintEmission(cIDGenerator.generateID(), emittedID, evaluatedTickIndex))
    }
  }

  override fun getTVNContent(): String {
    var emissionsStr = ""
    emissions.forEach {
      emissionsStr += it.tableStr()
    }
    return getTVNTableString(nodeID, "VAR_INTRO for $referenceCCB", emissionsStr)
  }

}

/** Is used as an intermediate step. */
internal class UniversalEvalNode(
  override val evaluationContext: EvaluationContext,
  /** Reference to the formula. */
  val evaluable: Formula,
  /** Defines which tick is evaluated. */
  val evaluatedTickIndex: Int,
  /**
   * The precondition describes a constraint on the evaluated interval: precon => phi.
   * This is needed, because certain constraints on the evaluation interval are not known before the evaluation.
   */
  val tickPrecondition: EvaluationTickPrecondition?,
  /**
   * This induces the largest tick that has to be instantiated and that possibly can be sat if the [tickPrecondition]
   * allows this.
   */
  val rightBorderOfInterval: Int?
) : IEvalNode {

  override val nodeID: Int = evaluationContext.constraintIDGenerator.generateID()
  override val children: MutableList<IEvalNode> = mutableListOf()
  override val emissions: MutableList<IEmission> = mutableListOf()

  override fun getTVNContent(): String {
    val tickPrecondStr = if (tickPrecondition == null) "" else "<TR><TD COLSPAN=\"3\">TickPrecond: " +
            "${tickPrecondition.toHTMLString()}</TD></TR>"
    val rightIntervalStr = if (rightBorderOfInterval == null) "∞)" else "$rightBorderOfInterval]"
    val intervalStr = "[$evaluatedTickIndex,$rightIntervalStr"
    return getTVNTableString(nodeID, "UNIV in $intervalStr", evaluable::class.simpleName!!, tickPrecondStr)
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
