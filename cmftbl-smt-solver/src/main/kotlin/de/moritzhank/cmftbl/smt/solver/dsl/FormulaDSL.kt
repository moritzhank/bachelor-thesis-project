@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.dsl

import kotlin.reflect.KCallable
import tools.aqua.stars.core.types.*

class FormulaBuilder(
    allowedCCBs: List<CallContextBase<*>>,
    registeredFunctions: MutableMap<KCallable<*>, TNFunction<*>>,
    private val phi: MutableList<Formula> = mutableListOf()
) : DSLBuilder(allowedCCBs, registeredFunctions) {

  companion object {

    /** Define formula with no free variables. */
    fun formula(init: FormulaBuilder.() -> Unit): Formula {
      val builder = FormulaBuilder(listOf(), mutableMapOf())
      init.invoke(builder)
      return builder.phi[0]
    }

    /** Define formula with one free variable (Does not return formula!). */
    fun <
        E1 : E,
        E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>> formula(
        init: FormulaBuilder.(CallContextBase<E1>) -> Unit
    ): (CallContextBase<E1>) -> FormulaBuilder {
      return { ccb: CallContextBase<E1> ->
        val builder = FormulaBuilder(listOf(ccb), mutableMapOf())
        ccb.dslBuilder = builder
        init.invoke(builder, ccb)
        builder
      }
    }

    /** Define formula with two free variables (Does not return formula!). */
    fun <
        E1 : E,
        E : EntityType<E, T, S, U, D>,
        T : TickDataType<E, T, S, U, D>,
        S : SegmentType<E, T, S, U, D>,
        U : TickUnit<U, D>,
        D : TickDifference<D>> formula(
        init: FormulaBuilder.(CallContextBase<E1>, CallContextBase<E1>) -> Unit
    ): (CallContextBase<E1>, CallContextBase<E1>) -> FormulaBuilder {
      return { ccb1: CallContextBase<E1>, ccb2: CallContextBase<E1> ->
        val builder = FormulaBuilder(listOf(ccb1, ccb2), mutableMapOf())
        ccb1.dslBuilder = builder
        ccb2.dslBuilder = builder
        init.invoke(builder, ccb1, ccb2)
        builder
      }
    }
  }

  private fun deriveFormulaBuilder() =
      FormulaBuilder(allowedCCBs, registeredFunctions.toMutableMap())

  private fun deriveFormulaBuilder(newAllowedCCB: CCB<*>) =
      FormulaBuilder(allowedCCBs.plus(newAllowedCCB), registeredFunctions.toMutableMap())

  private fun buildNeg(): Neg = assert(phi.size == 1).let { Neg(phi.first()) }

  private fun buildAnd(): And = assert(phi.size == 2).let { And(phi[0], phi[1]) }

  private fun buildOr(): Or = assert(phi.size == 2).let { Or(phi[0], phi[1]) }

  private fun buildImpl(): Implication = assert(phi.size == 2).let { Implication(phi[0], phi[1]) }

  private fun buildIff(): Iff = assert(phi.size == 2).let { Iff(phi[0], phi[1]) }

  private fun buildPrev(interval: Pair<Int, Int>?): Prev {
    assert(phi.size == 1)
    return Prev(interval, phi.first())
  }

  private fun buildNext(interval: Pair<Int, Int>?): Next {
    assert(phi.size == 1)
    return Next(interval, phi.first())
  }

  private fun buildOnce(interval: Pair<Int, Int>?): Once {
    assert(phi.size == 1)
    return Once(interval, phi.first())
  }

  private fun buildHistorically(interval: Pair<Int, Int>?): Historically {
    assert(phi.size == 1)
    return Historically(interval, phi.first())
  }

  private fun buildEventually(interval: Pair<Int, Int>?): Eventually {
    assert(phi.size == 1)
    return Eventually(interval, phi.first())
  }

  private fun buildAlways(interval: Pair<Int, Int>? = null): Always {
    assert(phi.size == 1)
    return Always(interval, inner = phi[0])
  }

  private fun buildSince(interval: Pair<Int, Int>? = null): Since {
    assert(phi.size == 2)
    return Since(interval, lhs = phi[0], rhs = phi[1])
  }

  private fun buildUntil(interval: Pair<Int, Int>? = null): Until {
    assert(phi.size == 2)
    return Until(interval, lhs = phi[0], rhs = phi[1])
  }

  private fun <
      E1 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> buildForall(ccb: CallContextBase<E1>): Forall<E1> {
    assert(phi.size == 1)
    return Forall(ccb, phi[0])
  }

  private fun <
      E1 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> buildExists(ccb: CallContextBase<E1>): Exists<E1> {
    assert(phi.size == 1)
    return Exists(ccb, phi[0])
  }

  private fun buildMinPrevalence(
      interval: Pair<Int, Int>? = null,
      fraction: Double
  ): MinPrevalence {
    assert(phi.size == 1)
    return MinPrevalence(interval, fraction, phi[0])
  }

  private fun buildMaxPrevalence(
      interval: Pair<Int, Int>? = null,
      fraction: Double
  ): MaxPrevalence {
    assert(phi.size == 1)
    return MaxPrevalence(interval, fraction, phi[0])
  }

  private fun buildPastMinPrevalence(
      interval: Pair<Int, Int>? = null,
      fraction: Double
  ): PastMinPrevalence {
    assert(phi.size == 1)
    return PastMinPrevalence(interval, fraction, phi[0])
  }

  private fun buildPastMaxPrevalence(
      interval: Pair<Int, Int>? = null,
      fraction: Double
  ): PastMaxPrevalence {
    assert(phi.size == 1)
    return PastMaxPrevalence(interval, fraction, phi[0])
  }

  private fun <Type> buildBinding(ccb: CallContextBase<Type>, term: Term<Type>): Binding<Type> {
    assert(phi.size == 1)
    return Binding(ccb, term, phi[0])
  }

  fun <
      E1 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> ((CallContextBase<E1>) -> FormulaBuilder).holds(
      ccb: CallContextBase<E1>
  ): Formula = this(ccb).phi[0].also { phi.add(copyFormula(it)) }

  fun <
      E1 : E,
      E2 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> ((CallContextBase<E1>, CallContextBase<E2>) -> FormulaBuilder).holds(
      ccb1: CallContextBase<E1>,
      ccb2: CallContextBase<E2>
  ): Formula {
    val x = this(ccb1, ccb2)
    return x.phi[0].also { phi.add(copyFormula(it)) }
  }

  fun FormulaBuilder.tt(): TT = TT.also { phi.add(it) }

  fun FormulaBuilder.ff(): FF = FF.also { phi.add(it) }

  fun FormulaBuilder.neg(input: Formula): Neg {
    return Neg(input).also { phi.add(it) }
  }

  fun FormulaBuilder.neg(init: FormulaBuilder.() -> Unit = {}): Neg {
    return FormulaBuilder(allowedCCBs, registeredFunctions.toMutableMap())
        .apply(init)
        .buildNeg()
        .also { phi.add(it) }
  }

  infix fun Formula.and(other: Formula): And =
      And(this, other).also {
        phi.removeLast()
        phi.removeLast()
        phi.add(it)
      }

  infix fun Formula.or(other: Formula): Or =
      Or(this, other).also {
        phi.clear()
        phi.add(it)
      }

  infix fun Formula.impl(other: Formula): Implication =
      Implication(this, other).also {
        phi.clear()
        phi.add(it)
      }

  infix fun Formula.iff(other: Formula): Iff =
      Iff(this, other).also {
        phi.clear()
        phi.add(it)
      }

  fun FormulaBuilder.prev(
      interval: Pair<Int, Int>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Prev {
    return deriveFormulaBuilder().apply(init).buildPrev(interval).also { phi.add(it) }
  }

  fun FormulaBuilder.next(
      interval: Pair<Int, Int>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Next {
    return deriveFormulaBuilder().apply(init).buildNext(interval).also { phi.add(it) }
  }

  fun FormulaBuilder.once(
      interval: Pair<Int, Int>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Once {
    return deriveFormulaBuilder().apply(init).buildOnce(interval).also { phi.add(it) }
  }

  fun FormulaBuilder.historically(
      interval: Pair<Int, Int>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Historically {
    return deriveFormulaBuilder().apply(init).buildHistorically(interval).also { phi.add(it) }
  }

  fun eventually(
      interval: Pair<Int, Int>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Eventually {
    return deriveFormulaBuilder().apply(init).buildEventually(interval).also { phi.add(it) }
  }

  fun FormulaBuilder.always(
      interval: Pair<Int, Int>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Always {
    return deriveFormulaBuilder().apply(init).buildAlways(interval).also { phi.add(it) }
  }

  fun FormulaBuilder.since(
      interval: Pair<Int, Int>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Since {
    return deriveFormulaBuilder().apply(init).buildSince(interval).also { phi.add(it) }
  }

  fun FormulaBuilder.until(
      interval: Pair<Int, Int>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): Until {
    return deriveFormulaBuilder().apply(init).buildUntil(interval).also { phi.add(it) }
  }

  fun <
      E1 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> FormulaBuilder.forall(
      init: FormulaBuilder.(CallContextBase<E1>) -> Unit = {}
  ): Forall<E1> {
    val ccb = CallContextBase<E1>()
    val fb = deriveFormulaBuilder(ccb)
    ccb.dslBuilder = fb
    return fb.apply { init(ccb) }.buildForall(ccb).also { phi.add(it) }
  }

  fun <
      E1 : E,
      E : EntityType<E, T, S, U, D>,
      T : TickDataType<E, T, S, U, D>,
      S : SegmentType<E, T, S, U, D>,
      U : TickUnit<U, D>,
      D : TickDifference<D>> FormulaBuilder.exists(
      init: FormulaBuilder.(CallContextBase<E1>) -> Unit = {}
  ): Exists<E1> {
    val ccb = CallContextBase<E1>()
    val fb = deriveFormulaBuilder(ccb)
    ccb.dslBuilder = fb
    return fb.apply { init(ccb) }.buildExists(ccb).also { phi.add(it) }
  }

  fun FormulaBuilder.minPrevalence(
      fraction: Double,
      interval: Pair<Int, Int>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): MinPrevalence {
    return deriveFormulaBuilder().apply(init).buildMinPrevalence(interval, fraction).also {
      phi.add(it)
    }
  }

  fun FormulaBuilder.maxPrevalence(
      fraction: Double,
      interval: Pair<Int, Int>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): MaxPrevalence {
    return deriveFormulaBuilder().apply(init).buildMaxPrevalence(interval, fraction).also {
      phi.add(it)
    }
  }

  fun FormulaBuilder.pastMinPrevalence(
      fraction: Double,
      interval: Pair<Int, Int>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): PastMinPrevalence {
    return deriveFormulaBuilder().apply(init).buildPastMinPrevalence(interval, fraction).also {
      phi.add(it)
    }
  }

  fun FormulaBuilder.pastMaxPrevalence(
      fraction: Double,
      interval: Pair<Int, Int>? = null,
      init: FormulaBuilder.() -> Unit = {}
  ): PastMaxPrevalence {
    return deriveFormulaBuilder().apply(init).buildPastMaxPrevalence(interval, fraction).also {
      phi.add(it)
    }
  }

  fun <Type> FormulaBuilder.binding(
      term: Term<Type>,
      init: FormulaBuilder.(CallContextBase<Type>) -> Unit = {}
  ): Binding<Type> {
    val ccb = CallContextBase<Type>()
    val fb = deriveFormulaBuilder(ccb)
    ccb.dslBuilder = fb
    return fb.apply { init(ccb) }.buildBinding(ccb, term).also { phi.add(it) }
  }

  infix fun <Type> Term<Type>.leq(other: Term<Type>): Leq<Type> =
      Leq(this, other).also { phi.add(it) }

  infix fun <Type> Term<Type>.lt(other: Term<Type>): Lt<Type> = Lt(this, other).also { phi.add(it) }

  infix fun <Type> Term<Type>.geq(other: Term<Type>): Geq<Type> =
      Geq(this, other).also { phi.add(it) }

  infix fun <Type> Term<Type>.gt(other: Term<Type>): Gt<Type> = Gt(this, other).also { phi.add(it) }

  infix fun <Type> Term<Type>.eq(other: Term<Type>): Eq<Type> = Eq(this, other).also { phi.add(it) }

  infix fun <Type> Term<Type>.ne(other: Term<Type>): Ne<Type> = Ne(this, other).also { phi.add(it) }

  fun <Type> term(cc: CallContext<*, Type>): Variable<Type> =
      assertCallContextAllowed(cc).let { Variable(cc) }

  fun <Type> const(value: Type): Constant<Type> = Constant(value)

  /** This function is not intended to be used by the user. */
  fun getPhi(): List<Formula> = phi.toList()
}
