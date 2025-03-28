@file:Suppress("unused", "unnecessary_not_null_assertion")

package de.moritzhank.cmftbl.smt.solver.dsl

import kotlin.reflect.*

/** Base for [FormulaBuilder] and [TFunctionBuilder]. */
open class DSLBuilder(
    protected val allowedCCBs: List<CCB<*>>,
    protected val registeredFunctions: MutableMap<KCallable<*>, TNFunction<*>>
) {

  /** Assert that a CallContext [callContext] is legal in the current context. */
  fun assertCallContextAllowed(callContext: CallContext<*, *>) {
    // Is callContext's CallContextBase allowed?
    if (!allowedCCBs.contains(callContext.base())) {
      throw AssertionError("${callContext.toFormattedString()}} is not allowed in this context.")
    }
    // Are all function calls registered?
    var elemBefore: CallContext<*, *>? = callContext
    while (elemBefore != null) {
      val currentElem = elemBefore.also { elemBefore = elemBefore!!.before }
      val isRegistered =
          when (currentElem) {
            is Callable1CallContext -> registeredFunctions[currentElem.func] != null
            is Callable2CallContext<*, *, *> -> registeredFunctions[currentElem.func] != null
            is Callable3CallContext<*, *, *, *> -> registeredFunctions[currentElem.func] != null
            else -> true
          }
      if (!isRegistered) {
        throw AssertionError("${currentElem.toFormattedString()} is not a registered function.")
      }
    }
  }

  /** Register the symbolic representation [func] of the function [callable]. */
  fun <Caller, Return> registerFunction(
      callable: KFunction1<Caller, Return>,
      func: T2Function<Caller, Return>
  ) {
    registeredFunctions[callable] = func
  }

  /** Register the symbolic representation [func] of the function [callable]. */
  fun <Caller, Param, Return> registerFunction(
      callable: KFunction2<Caller, Param, Return>,
      func: T3Function<Caller, Param, Return>
  ) {
    registeredFunctions[callable] = func
  }

  /** Register the symbolic representation [func] of the function [callable]. */
  fun <Caller, Param1, Param2, Return> registerFunction(
      callable: KFunction3<Caller, Param1, Param2, Return>,
      func: T4Function<Caller, Param1, Param2, Return>
  ) {
    registeredFunctions[callable] = func
  }
}
