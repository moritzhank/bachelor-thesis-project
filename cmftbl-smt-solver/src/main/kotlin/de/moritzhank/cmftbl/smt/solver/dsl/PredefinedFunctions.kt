@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.dsl

import de.moritzhank.cmftbl.smt.solver.dsl.TFunctionBuilder.Companion.function

/** Contains commonly used predefined functions. */
object PredefinedFunctions {

  val IntSign = function { int: CCB<Int> ->
    branch {
          lt {
            wrap(int)
            const(0)
          }
        }
        .satisfied { const(-1) }
        .otherwise { const(1) }
  }
}
