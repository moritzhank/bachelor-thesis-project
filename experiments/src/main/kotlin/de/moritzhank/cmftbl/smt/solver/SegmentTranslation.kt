package de.moritzhank.cmftbl.smt.solver

import de.moritzhank.cmftbl.smt.solver.translation.data.SmtDataTranslationWrapper
import de.moritzhank.cmftbl.smt.solver.translation.data.generateSmtLib
import de.moritzhank.cmftbl.smt.solver.translation.data.getSmtIntermediateRepresentation
import kotlinx.serialization.modules.EmptySerializersModule
import tools.aqua.stars.data.av.dataclasses.Segment

/** Generate SMTLib from a segment. */
fun generateSmtLibForSegment(segment: Segment, solver: SmtSolver = SmtSolver.CVC5, logic: String = "ALL"): String {
  val serializersModule = EmptySerializersModule()
  val intermediateRepresentation = getSmtIntermediateRepresentation<Segment>(serializersModule, segment)
  val translationWrapper = SmtDataTranslationWrapper(intermediateRepresentation, segment.tickData.toTypedArray())
  return generateSmtLib(translationWrapper, solver, logic)
}
