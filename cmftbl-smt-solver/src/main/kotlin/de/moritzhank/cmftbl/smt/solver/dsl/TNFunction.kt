@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.dsl

/** Symbolic representation of n-ary functions (Wraps [TFunction]). */
sealed interface TNFunction<Return> {
  val func: TFunction<Return>
}

sealed interface T1Function<Return> : TNFunction<Return>

sealed interface T2Function<Param, Return> : TNFunction<Return>

sealed interface T3Function<Param1, Param2, Return> : TNFunction<Return>

sealed interface T4Function<Param1, Param2, Param3, Return> : TNFunction<Return>
