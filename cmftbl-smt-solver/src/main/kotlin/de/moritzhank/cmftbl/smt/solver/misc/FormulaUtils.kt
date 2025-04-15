package de.moritzhank.cmftbl.smt.solver.misc

import de.moritzhank.cmftbl.smt.solver.dsl.And
import de.moritzhank.cmftbl.smt.solver.dsl.Formula
import de.moritzhank.cmftbl.smt.solver.dsl.Iff
import de.moritzhank.cmftbl.smt.solver.dsl.Implication
import de.moritzhank.cmftbl.smt.solver.dsl.LogicalConnectiveFormula
import de.moritzhank.cmftbl.smt.solver.dsl.Neg
import de.moritzhank.cmftbl.smt.solver.dsl.Or

fun LogicalConnectiveFormula.lhs() : Formula {
  return when (this) {
    is And -> lhs
    is Iff -> lhs
    is Implication -> lhs
    is Neg -> error("Neg has no left child node.")
    is Or -> lhs
  }
}

fun LogicalConnectiveFormula.rhs() : Formula {
  return when (this) {
    is And -> rhs
    is Iff -> rhs
    is Implication -> rhs
    is Neg -> error("Neg has no right child node.")
    is Or -> rhs
  }
}