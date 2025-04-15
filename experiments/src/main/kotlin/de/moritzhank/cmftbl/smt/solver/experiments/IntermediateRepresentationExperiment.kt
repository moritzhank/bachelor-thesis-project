package de.moritzhank.cmftbl.smt.solver.experiments

import de.moritzhank.cmftbl.smt.solver.ExperimentLoader
import de.moritzhank.cmftbl.smt.solver.SmtSolver
import de.moritzhank.cmftbl.smt.solver.runSmtSolver
import de.moritzhank.cmftbl.smt.solver.translation.data.SmtDataTranslationWrapper
import de.moritzhank.cmftbl.smt.solver.translation.data.SmtIntermediateRepresentation
import de.moritzhank.cmftbl.smt.solver.translation.data.generateSmtLib
import de.moritzhank.cmftbl.smt.solver.translation.data.getSmtIntermediateRepresentation
import kotlinx.serialization.modules.EmptySerializersModule
import tools.aqua.stars.data.av.dataclasses.Segment
import kotlin.time.measureTime

fun main() {
  val solver = SmtSolver.YICES
  val removeSmt2File = false
  val logic = "QF_LIRA"

  val t: Segment = ExperimentLoader.loadTestSegment()
  println("Finished reading.")
  val serializersModule = EmptySerializersModule()
  var intermediateRepresentation: List<SmtIntermediateRepresentation>
  val intermediateRepresentationTime = measureTime {
    intermediateRepresentation = getSmtIntermediateRepresentation(serializersModule, t)
  }
  println("Duration of generation of intermediate representation: $intermediateRepresentationTime")
  println("Size of intermediate representation: ${intermediateRepresentation.size}")
  var translationWrapper: SmtDataTranslationWrapper
  val translationWrapperTime = measureTime {
    translationWrapper =
        SmtDataTranslationWrapper(intermediateRepresentation, t.tickData.toTypedArray())
  }
  println("Duration of generation of SmtDataTranslationWrapper: $translationWrapperTime")
  var smtLib: String
  val smtLibTime = measureTime { smtLib = generateSmtLib(translationWrapper, solver, logic) }
  smtLib += "(check-sat)"
  smtLib = ";Town_01, seed 2, segment 1" + System.lineSeparator() + smtLib
  println("Duration of generation of SMT-LIB: $smtLibTime")
  println("Generated SmtLib lines: ${smtLib.lines().size}")

  println("Running solver ...")
  println("========[ Result of the solver ]========")
  println(runSmtSolver(smtLib, solver, null, removeSmt2File, 120, memoryProfilerCallback = null))
  println("========================================")
}
