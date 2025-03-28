package de.moritzhank.cmftbl.smt.solver.dsl

sealed interface TFunction<Return>

data class TFConstantNumber<T : Number>(val content: T) : TFunction<T>

data class TFConstantBoolean(val content: Boolean) : TFunction<Boolean>

data class TFCallContextWrapper<Return>(
    val callContext: CallContext<*, Return>,
) : TFunction<Return>

data class TFAdd<T : Number>(val lhs: TFunction<T>, val rhs: TFunction<T>) : TFunction<T>

data class TFComparison<T>(val lhs: TFunction<T>, val rhs: TFunction<T>, val relation: Relation) :
    TFunction<Boolean>

data class TFFilter<C, T : Collection<C>>(
    val collection: CallContext<*, T>,
    val phi: TFunction<Boolean>
) : TFunction<Collection<C>>

data class TFBranch<T>(
    val cond: TFunction<Boolean>,
    val thenFunction: TFunction<T>,
    val elseFunction: TFunction<T>
) : TFunction<T>
