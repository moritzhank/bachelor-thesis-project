@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.dsl

import kotlin.reflect.*
import de.moritzhank.cmftbl.smt.solver.translation.data.smtTranslationClassInfo

typealias CCB<Type> = CallContextBase<Type>

/** Symbolic representation of a call to a member of [Caller] which returns [Return]. */
sealed interface CallContext<Caller, Return> {

  val before: CallContext<*, Caller>?

  operator fun <W> times(func: KFunction1<Return, W>): CallContext<Return, W> =
      Callable1CallContextImpl(func, this)

  operator fun <W, P> times(
      func: KFunction2<Return, P, W>
  ): IntermediateCallable2CallContext<Return, P, W> =
      IntermediateCallable2CallContextImpl(func, this, base())

  operator fun <W, P1, P2> times(
      func: KFunction3<Return, P1, P2, W>
  ): IntermediateCallable3CallContext<Return, P1, P2, W> =
      IntermediateCallable3CallContextImpl(func, this, base())

  fun base(): CallContextBase<*> {
    val before = this.before
    if (before == null) {
      require(this is CallContextBase<*>) { "No CallContextBase was found." }
      return this
    }
    return before.base()
  }
}

/** Represents a symbolic call to a property [prop]. */
sealed interface PropertyCallContext<Caller, Return> : CallContext<Caller, Return> {

  val prop: KProperty1<Caller, Return>
}

/** Represents a symbolic call to a 0-ary function [func]. */
sealed interface Callable1CallContext<Caller, Return> : CallContext<Caller, Return> {

  val func: KFunction1<Caller, Return>
}

/** Represents a symbolic call to a 1-ary function for which the parameter must still be defined. */
sealed interface IntermediateCallable2CallContext<Caller, Param, Return> {

  fun withParam(cc: CallContext<*, Param>): Callable2CallContext<Caller, Param, Return>
}

/** Represents a symbolic call to a 1-ary function [func]. */
sealed interface Callable2CallContext<Caller, Param, Return> : CallContext<Caller, Return> {

  val func: KFunction2<Caller, Param, Return>
  val param: CallContext<*, Param>
}

/**
 * Represents a symbolic call to a 2-ary function for which the parameters must still be defined.
 */
sealed interface IntermediateCallable3CallContext<Caller, Param1, Param2, Return> {

  fun withParams(
      cc1: CallContext<*, Param1>,
      cc2: CallContext<*, Param2>
  ): Callable3CallContext<Caller, Param1, Param2, Return>
}

/** Represents a symbolic call to a 2-ary function [func]. */
sealed interface Callable3CallContext<Caller, Param1, Param2, Return> :
    CallContext<Caller, Return> {

  val func: KFunction3<Caller, Param1, Param2, Return>
  val param1: CallContext<*, Param1>
  val param2: CallContext<*, Param2>
}

/** Starting point for defining symbolic member calls to [Type]. */
class CallContextBase<Type: Any>(val kClass: KClass<Type>) : CallContext<Nothing, Type> {

  var debugInfo: String? = null
  override val before: CallContext<*, Nothing>? = null
  lateinit var dslBuilder: DSLBuilder

  override operator fun <Return> times(func: KFunction1<Type, Return>): CallContext<Type, Return> =
      Callable1CallContextImpl(func, null)

  override operator fun <Return, Param> times(
      func: KFunction2<Type, Param, Return>
  ): IntermediateCallable2CallContext<Type, Param, Return> =
      IntermediateCallable2CallContextImpl(func, null, base())

  override operator fun <Return, Param1, Param2> times(
      func: KFunction3<Type, Param1, Param2, Return>
  ): IntermediateCallable3CallContext<Type, Param1, Param2, Return> =
      IntermediateCallable3CallContextImpl(func, null, base())

  override fun toString() = debugInfo ?: super.toString()
}

// Operator for representing a property call on CallContext.
// To perform the property legality check, this function must be an extension function with reified
// Return.
inline operator fun <Caller, reified Return : Any, W> CallContext<Caller, Return>.times(
    prop: KProperty1<Return, W>
): CallContext<Return, W> = this.callProperty(prop, Return::class)

// Operator for representing a property call on CallContextBase.
// To perform the property legality check, this function must be an extension function with reified
// Return.
inline operator fun <reified Return : Any, W> CallContextBase<Return>.times(
    prop: KProperty1<Return, W>
): CallContext<Return, W> = this.callProperty(prop, Return::class)

/**
 * Realises a symbolic call to a property [prop] and checks if the property is legal (Not intended
 * for direct use).
 */
fun <Caller : Any, Return> CallContext<*, Caller>.callProperty(
    prop: KProperty1<Caller, Return>,
    callerClass: KClass<Caller>
): PropertyCallContext<Caller, Return> {
  // Exception for List::size
  if (callerClass != List::class || prop.name != "size") {
    smtTranslationClassInfo(callerClass).requireTranslatableProperty(prop.name)
  }
  // Notice: Previously, the base of PropertyCallContextImpl was set to zero when called on CCB
  return PropertyCallContextImpl(prop, this)
}

/** Returns a string formatted as "name (...)". */
fun CallContext<*, *>.toFormattedString(): String {
  val name =
      when (this) {
        is Callable1CallContext<*, *> -> func.name
        is Callable2CallContext<*, *, *> -> func.name
        is Callable3CallContext<*, *, *, *> -> func.name
        is PropertyCallContext<*, *> -> prop.name
        else -> "?"
      }
  return "\"$name\" ($this)"
}

private class PropertyCallContextImpl<Caller, Return>(
    override val prop: KProperty1<Caller, Return>,
    override val before: CallContext<*, Caller>?
) : PropertyCallContext<Caller, Return>

private class Callable1CallContextImpl<Caller, Return>(
    override val func: KFunction1<Caller, Return>,
    override val before: CallContext<*, Caller>?
) : Callable1CallContext<Caller, Return>

private class IntermediateCallable2CallContextImpl<Caller, Param, Return>(
    private val func: KFunction2<Caller, Param, Return>,
    private val before: CallContext<*, Caller>?,
    private val base: CallContextBase<*>
) : IntermediateCallable2CallContext<Caller, Param, Return> {

  override fun withParam(cc: CallContext<*, Param>): Callable2CallContext<Caller, Param, Return> =
      base.dslBuilder.assertCallContextAllowed(cc).let {
        Callable2CallContextImpl(func, before, cc)
      }
}

private class Callable2CallContextImpl<Caller, Param, Return>(
    override val func: KFunction2<Caller, Param, Return>,
    override val before: CallContext<*, Caller>?,
    override val param: CallContext<*, Param>
) : Callable2CallContext<Caller, Param, Return>

private class IntermediateCallable3CallContextImpl<Caller, Param1, Param2, Return>(
    private val func: KFunction3<Caller, Param1, Param2, Return>,
    private val before: CallContext<*, Caller>?,
    private val base: CallContextBase<*>
) : IntermediateCallable3CallContext<Caller, Param1, Param2, Return> {

  override fun withParams(
      cc1: CallContext<*, Param1>,
      cc2: CallContext<*, Param2>
  ): Callable3CallContext<Caller, Param1, Param2, Return> =
      base.dslBuilder
          .assertCallContextAllowed(cc1)
          .also { base.dslBuilder.assertCallContextAllowed(cc2) }
          .let { Callable3CallContextImpl(func, before, cc1, cc2) }
}

private class Callable3CallContextImpl<Caller, Param1, Param2, Return>(
    override val func: KFunction3<Caller, Param1, Param2, Return>,
    override val before: CallContext<*, Caller>?,
    override val param1: CallContext<*, Param1>,
    override val param2: CallContext<*, Param2>
) : Callable3CallContext<Caller, Param1, Param2, Return>
