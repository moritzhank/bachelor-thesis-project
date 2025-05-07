package de.moritzhank.cmftbl.smt.solver.misc

import de.moritzhank.cmftbl.smt.solver.SmtSolver

fun diagramColor(solver: SmtSolver): String = when(solver) {
    SmtSolver.CVC5 -> "#E84D8A"
    SmtSolver.Z3 -> "#FEB326"
    SmtSolver.YICES -> "#7F58AF"
    SmtSolver.MATHSAT -> "#7CC3E6"
}