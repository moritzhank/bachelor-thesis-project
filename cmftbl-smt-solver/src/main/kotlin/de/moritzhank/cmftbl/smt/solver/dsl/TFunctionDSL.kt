@file:Suppress("unused", "unchecked_cast")

package de.moritzhank.cmftbl.smt.solver.dsl

import kotlin.reflect.KCallable
import kotlin.reflect.KClass

// Important: The TFunction-DSL cannot contain infix operators, because this would conflict with the
// correctness of the return type
class TFunctionBuilder<Return>(
    allowedCCBs: List<CallContextBase<*>>,
    registeredFunctions: MutableMap<KCallable<*>, TNFunction<*>>,
    private val funs: MutableList<TFunction<*>> = mutableListOf()
) : DSLBuilder(allowedCCBs, registeredFunctions) {

  companion object {

    /** Define function with no parameters. */
    fun <Return> function(init: TFunctionBuilder<Return>.() -> Unit): T1Function<Return> {
      val builder = TFunctionBuilder<Return>(listOf(), mutableMapOf())
      init.invoke(builder)
      return T1FunctionImpl(builder.funs[0] as TFunction<Return>)
    }

    /** Define function with one parameter. */
    inline fun <reified Param : Any, Return> function(
        noinline init: TFunctionBuilder<Return>.(CallContextBase<Param>) -> Unit
    ): T2Function<Param, Return> {
      return function(Param::class, init)
    }

    /** Define function with one parameter. */
    fun <Param : Any, Return> function(
      kClass: KClass<Param>,
      init: TFunctionBuilder<Return>.(CallContextBase<Param>) -> Unit
    ): T2Function<Param, Return> {
      val params = listOf(CallContextBase<Param>(kClass))
      val builder = TFunctionBuilder<Return>(params, mutableMapOf())
      params[0].dslBuilder = builder
      init.invoke(builder, params[0])
      return T2FunctionImpl(builder.funs[0] as TFunction<Return>)
    }

    /** Define function with two parameters. */
    inline fun <reified Param1 : Any, reified Param2 : Any, Return> function(
        noinline init: TFunctionBuilder<Return>.(CallContextBase<Param1>, CallContextBase<Param2>) -> Unit
    ): T3Function<Param1, Param2, Return> {
      return function(Param1::class, Param2::class, init)
    }

    /** Define function with two parameters. */
    fun <Param1 : Any, Param2 : Any, Return> function(
      kClass1: KClass<Param1>,
      kClass2: KClass<Param2>,
      init: TFunctionBuilder<Return>.(CallContextBase<Param1>, CallContextBase<Param2>) -> Unit
    ): T3Function<Param1, Param2, Return> {
      val param1 = CallContextBase<Param1>(kClass1)
      val param2 = CallContextBase<Param2>(kClass2)
      val builder = TFunctionBuilder<Return>(listOf(param1, param2), mutableMapOf())
      param1.dslBuilder = builder
      param2.dslBuilder = builder
      init.invoke(builder, param1, param2)
      return T3FunctionImpl(builder.funs[0] as TFunction<Return>)
    }

    /** Define function with three parameters. */
    inline fun <reified Param1 : Any, reified Param2 : Any, reified Param3 : Any, Return> function(
      noinline init: TFunctionBuilder<Return>.(CCB<Param1>, CCB<Param2>, CCB<Param3>) -> Unit
    ): T4Function<Param1, Param2, Param3, Return> {
      return function(Param1::class, Param2::class, Param3::class, init)
    }

    /** Define function with three parameters. */
    fun <Param1 : Any, Param2 : Any, Param3 : Any, Return> function(
      kClass1 : KClass<Param1>,
      kClass2 : KClass<Param2>,
      kClass3 : KClass<Param3>,
      init: TFunctionBuilder<Return>.(CallContextBase<Param1>, CallContextBase<Param2>, CallContextBase<Param3>) -> Unit
    ): T4Function<Param1, Param2, Param3, Return> {
      val param1 = CallContextBase<Param1>(kClass1)
      val param2 = CallContextBase<Param2>(kClass2)
      val param3 = CallContextBase<Param3>(kClass3)
      val builder = TFunctionBuilder<Return>(listOf(param1, param2, param3), mutableMapOf())
      param1.dslBuilder = builder
      param2.dslBuilder = builder
      param3.dslBuilder = builder
      init.invoke(builder, param1, param2, param3)
      return T4FunctionImpl(builder.funs[0] as TFunction<Return>)
    }
  }

  private fun <P, R> buildCallContextWrapper(cc: CallContext<P, R>): TFCallContextWrapper<R> =
      assertCallContextAllowed(cc).let { TFCallContextWrapper(cc) }

  private fun <T> buildComparison(relation: Relation): TFComparison<T> =
      assert(funs.size == 2).let {
        TFComparison(funs[0] as TFunction<T>, funs[1] as TFunction<T>, relation)
      }

  private fun <T : Number> buildAdd(): TFAdd<T> =
      assert(funs.size == 2).let { TFAdd(funs[0] as TFunction<T>, funs[1] as TFunction<T>) }

  private fun <C, T : Collection<C>> buildFilter(cc: CallContext<*, T>): TFFilter<C, T> =
      assert(funs.size == 1)
          .also { assertCallContextAllowed(cc) }
          .let { TFFilter(cc, funs[0] as TFunction<Boolean>) }

  private fun <T> buildBranch(funCond: TFunction<Boolean>): TFBranch<T> =
      assert(funs.size == 2).let {
        TFBranch(funCond, funs[0] as TFunction<T>, funs[1] as TFunction<T>)
      }

  fun TFunctionBuilder<Return>.wrap(cc: CallContext<*, Return>): TFunction<Return> =
      buildCallContextWrapper(cc).also { funs.add(it) }

  fun <T : Number> TFunctionBuilder<T>.const(content: T) =
      TFConstantNumber(content).also { funs.add(it) }

  fun TFunctionBuilder<Boolean>.const(content: Boolean) =
      TFConstantBoolean(content).also { funs.add(it) }

  private fun <T> comparison(
      init: TFunctionBuilder<T>.() -> Unit,
      relation: Relation
  ): TFComparison<T> {
    return TFunctionBuilder<T>(allowedCCBs, registeredFunctions.toMutableMap())
        .apply(init)
        .buildComparison<T>(relation)
        .also { funs.add(it) }
  }

  fun <T> TFunctionBuilder<Boolean>.leq(init: TFunctionBuilder<T>.() -> Unit): TFComparison<T> =
      comparison(init, Relation.Leq)

  fun <T> TFunctionBuilder<Boolean>.geq(init: TFunctionBuilder<T>.() -> Unit): TFComparison<T> =
      comparison(init, Relation.Geq)

  fun <T> TFunctionBuilder<Boolean>.lt(init: TFunctionBuilder<T>.() -> Unit): TFComparison<T> =
      comparison(init, Relation.Lt)

  fun <T> TFunctionBuilder<Boolean>.gt(init: TFunctionBuilder<T>.() -> Unit): TFComparison<T> =
      comparison(init, Relation.Gt)

  fun <T> TFunctionBuilder<Boolean>.eq(init: TFunctionBuilder<T>.() -> Unit): TFComparison<T> =
      comparison(init, Relation.Eq)

  fun <T> TFunctionBuilder<Boolean>.ne(init: TFunctionBuilder<T>.() -> Unit): TFComparison<T> =
      comparison(init, Relation.Ne)

  fun <T : Number> TFunctionBuilder<T>.add(init: TFunctionBuilder<T>.() -> Unit): TFAdd<T> {
    return TFunctionBuilder<T>(allowedCCBs, registeredFunctions.toMutableMap())
        .apply(init)
        .buildAdd<T>()
        .also { funs.add(it) }
  }

  inline fun <reified C : Any, T : Collection<C>> TFunctionBuilder<T>.filter(
    collection: CallContext<*, T>,
    noinline init: TFunctionBuilder<Boolean>.(CallContextBase<C>) -> Unit
  ): TFFilter<C, T> {
    return filter(C::class, collection, init)
  }

  fun <C: Any, T : Collection<C>> TFunctionBuilder<T>.filter(
      kClass: KClass<C>,
      collection: CallContext<*, T>,
      init: TFunctionBuilder<Boolean>.(CallContextBase<C>) -> Unit
  ): TFFilter<C, T> {
    val ccb = CallContextBase<C>(kClass)
    val fb = TFunctionBuilder<Boolean>(allowedCCBs.plus(ccb), registeredFunctions.toMutableMap())
    ccb.dslBuilder = fb
    init.invoke(fb, ccb)
    return fb.buildFilter(collection).also { funs.add(it) }
  }

  fun <T> TFunctionBuilder<T>.branch(
      init: TFunctionBuilder<Boolean>.() -> Unit
  ): TFBranchCondition<T> {
    val funBuilder =
        TFunctionBuilder<Boolean>(allowedCCBs, registeredFunctions.toMutableMap()).apply(init)
    assert(funBuilder.funs.size == 1)
    return TFBranchCondition(
        funBuilder.funs[0] as TFunction<Boolean>, this, this.allowedCCBs, this.registeredFunctions)
  }

  class TFBranchCondition<T>(
      private val condition: TFunction<Boolean>,
      private val initialBuilder: TFunctionBuilder<T>,
      private val allowedCCBs: List<CallContextBase<*>>,
      private val registeredFunctions: MutableMap<KCallable<*>, TNFunction<*>>
  ) {

    fun satisfied(init: TFunctionBuilder<T>.() -> Unit): TFBranchSatisfied<T> {
      val funBuilder =
          TFunctionBuilder<T>(allowedCCBs, registeredFunctions.toMutableMap()).apply(init)
      assert(funBuilder.funs.size == 1)
      return TFBranchSatisfied(
          condition,
          funBuilder.funs[0] as TFunction<T>,
          initialBuilder,
          allowedCCBs,
          registeredFunctions.toMutableMap())
    }
  }

  class TFBranchSatisfied<T>(
      private val condition: TFunction<Boolean>,
      private val satisfied: TFunction<T>,
      private val initialBuilder: TFunctionBuilder<T>,
      private val allowedCCBs: List<CallContextBase<*>>,
      private val registeredFunctions: MutableMap<KCallable<*>, TNFunction<*>>
  ) {

    fun otherwise(init: TFunctionBuilder<T>.() -> Unit): TFBranch<T> {
      val funBuilder =
          TFunctionBuilder<T>(allowedCCBs, registeredFunctions.toMutableMap()).apply(init)
      assert(funBuilder.funs.size == 1)
      return TFBranch(condition, satisfied, funBuilder.funs[0] as TFunction<T>).also {
        initialBuilder.funs.add(it)
      }
    }
  }
}

private class T1FunctionImpl<Return>(override val func: TFunction<Return>) : T1Function<Return>

private class T2FunctionImpl<Param, Return>(override val func: TFunction<Return>) :
    T2Function<Param, Return>

private class T3FunctionImpl<Param1, Param2, Return>(override val func: TFunction<Return>) :
    T3Function<Param1, Param2, Return>

private class T4FunctionImpl<Param1, Param2, Param3, Return>(override val func: TFunction<Return>) :
    T4Function<Param1, Param2, Param3, Return>
