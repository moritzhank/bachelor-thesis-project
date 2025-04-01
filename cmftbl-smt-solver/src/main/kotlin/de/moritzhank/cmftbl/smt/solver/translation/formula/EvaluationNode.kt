package de.moritzhank.cmftbl.smt.solver.translation.formula

import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.dsl.Evaluable
import de.moritzhank.cmftbl.smt.solver.dsl.Formula
import de.moritzhank.cmftbl.smt.solver.dsl.str
import de.moritzhank.cmftbl.smt.solver.misc.ITreeVisualizationNode
import de.moritzhank.cmftbl.smt.solver.misc.check

/** Abstraction of the translation process of formula AST. */
internal interface IEvalNode: ITreeVisualizationNode {
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

  override val emissions: MutableList<IEmission> = mutableListOf()

  override fun getTVNContent(): String {
    return "ORGA $content<BR/>"
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

  override fun getTVNContent(): String {
    val line1 = "EVALUATE <B>${evaluable::class.simpleName}  </B> @ $evaluatedTickIndex<BR/>"
    val line2 = if (tickPrecondition == null) "" else "TickPrecond: ${tickPrecondition.toHTMLString()}<BR/>"
    var assertionLines = ""
    emissions.forEach {
      val content = it.str()
      if (content.isNotEmpty()) {
        assertionLines += "${content}<BR/>"
      }
    }
    val annotation_ = if (annotation == null) "" else "<I>$annotation</I>"
    return line1 + line2 + assertionLines + annotation_
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

  init {
    interval.check()
  }

  override fun getTVNContent(): String {
    val line1 = "WITNESS <B>${evaluable::class.simpleName}  </B> in ${interval.str()}<BR/>"
    val line2 = if (tickPrecondition == null) "" else "TickPrecond: ${tickPrecondition.toHTMLString()}<BR/>"
    var assertionLines = ""
    emissions.forEach {
      val content = it.str()
      if (content.isNotEmpty()) {
        assertionLines += "${content}<BR/>"
      }
    }
    val annotation_ = if (annotation == null) "" else "<I>$annotation</I>"
    return line1 + line2 + assertionLines + annotation_
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
  val evaluatedInterval: Pair<Int, Int>?,
  formulaHoldsVariable: String,
  subFormulaHoldsVariable: String,
): IEvalNode {

  override val emissions = mutableListOf<IEmission>()

  init {
    emissions.add(NewInstanceEmission(emittedID))
    emissions.add(NewInstanceEmission(subFormulaHoldsVariable, true))
    val subFormulaHoldsVariable1 = "subFormulaHolds_${evaluationContext.evaluationIDGenerator.generateID()}"
    val subFormulaHoldsVariable2 = "subFormulaHolds_${evaluationContext.evaluationIDGenerator.generateID()}"
    emissions.add(NewInstanceEmission(subFormulaHoldsVariable1, true))
    emissions.add(NewInstanceEmission(subFormulaHoldsVariable2, true))
    emissions.add(ConstrainIDEmission(emittedID, assertedID, subFormulaHoldsVariable1))
    if (evaluatedInterval != null) {
      evaluatedInterval.check()
      emissions.add(EvalInIntervalConstraintEmission(emittedID, evaluatedInterval, subFormulaHoldsVariable2))
    } else {
      emissions.add(EvalAtTickConstraintEmission(emittedID, evaluatedTickIndex, subFormulaHoldsVariable2))
    }
    emissions.add(SubFormulaeHoldEmission(formulaHoldsVariable,
      listOf(subFormulaHoldsVariable, subFormulaHoldsVariable1, subFormulaHoldsVariable2)))
  }

  override fun getTVNContent(): String {
    val line1 = "VAR_INTRO $referenceCCB<BR/>"
    var assertionLines = ""
    emissions.forEach {
      val content = it.str()
      if (content.isNotEmpty()) {
        assertionLines += "${content}<BR/>"
      }
    }
    return line1 + assertionLines
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

  override val children: MutableList<IEvalNode> = mutableListOf()
  override val emissions: MutableList<IEmission> = mutableListOf()

  override fun getTVNContent(): String {
    val rightIntervalStr = if (rightBorderOfInterval == null) "∞)" else "$rightBorderOfInterval]"
    val line1 = "UNIV_INST <B>${evaluable::class.simpleName}  </B> for [$evaluatedTickIndex,$rightIntervalStr<BR/>"
    val line2 = if (tickPrecondition == null) "" else "TickPrecond: ${tickPrecondition.toHTMLString()}<BR/>"
    var assertionLines = ""
    emissions.forEach {
      val content = it.str()
      if (content.isNotEmpty()) {
        assertionLines += "${content}<BR/>"
      }
    }
    return line1 + line2 + assertionLines
  }

}