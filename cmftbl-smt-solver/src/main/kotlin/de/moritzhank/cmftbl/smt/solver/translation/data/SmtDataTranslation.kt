@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.translation.data

import de.moritzhank.cmftbl.smt.solver.SmtSolver
import de.moritzhank.cmftbl.smt.solver.misc._toSmtLibPrimitiveFormat
import de.moritzhank.cmftbl.smt.solver.misc.firstCharLower
import de.moritzhank.cmftbl.smt.solver.misc.generateEqualsITEStructure
import de.moritzhank.cmftbl.smt.solver.misc.negate
import de.moritzhank.cmftbl.smt.solver.misc.toSmtLibPrimitiveFormat

/** Generate SmtLib. */
fun generateSmtLib(
  wrapper: SmtDataTranslationWrapper,
  solver: SmtSolver = SmtSolver.CVC5,
  logic: String = "ALL"
): String {
  val result = StringBuilder()
  val termForNegativeNumber =
      if (solver == SmtSolver.YICES) {
        { x: Number -> "(- 0 ${x.negate()._toSmtLibPrimitiveFormat()})" }
      } else {
        { x: Number -> x._toSmtLibPrimitiveFormat() }
      }
  val termForMinusOne = termForNegativeNumber(-1)

  // Prelude
  result.appendLine("(set-logic $logic)")
  result.appendLine()

  // Generate sort intervals
  result.appendLine("; Sort intervals")
  wrapper.capturedKClassToExternalIDInterval.forEach { (kClass, interval) ->
    // Should be in cache to this point
    val name =
        if (kClass != List::class) {
          smtTranslationClassInfo(kClass).getTranslationName()
        } else {
          "List"
        }
    result.appendLine(
        "(define-fun is_$name ((id Int)) Bool (and (>= id ${interval.first}) (<= id ${interval.second})))")
  }
  result.appendLine()

  // Generate sort members
  result.appendLine("; Sort members")
  for ((name, smtIntermediateMember) in wrapper.memberNameToSmtIntermediateMember) {
    val memberInfo = wrapper.memberNameToMemberInfo[name]!!
    when (memberInfo.memberType) {
      // Generate member definition for references
      SmtIntermediateMemberType.REFERENCE -> {
        val iteStructure =
            generateEqualsITEStructure(
                smtIntermediateMember.entries,
                "id",
                { ifEntry -> "${wrapper.smtIDToExternalID[ifEntry.component1()]!!}" },
                { thenEntry ->
                  "${wrapper.smtIDToExternalID[(thenEntry.component2() as SmtIntermediateMember.Reference).refID]!!}"
                },
                termForMinusOne)
        result.appendLine("(define-fun ${name.firstCharLower()} ((id Int)) Int $iteStructure)")
      }
      // Generate member definition for values
      SmtIntermediateMemberType.VALUE -> {
        val smtPrimitive = memberInfo.memberClass.smtPrimitive()!!
        // All Strings are ignored because of solver performance.
        if (smtPrimitive == SmtPrimitive.STRING) {
          // Yices does not support strings
          continue
        }
        val iteStructure =
            generateEqualsITEStructure(
                smtIntermediateMember.entries,
                "id",
                { ifPair -> "${wrapper.smtIDToExternalID[ifPair.component1()]!!}" },
                { thenPair ->
                  (thenPair.component2() as SmtIntermediateMember.Value)
                      .value
                      .toSmtLibPrimitiveFormat(termForNegativeNumber)
                },
                smtPrimitive.defaultValue.toSmtLibPrimitiveFormat(termForNegativeNumber))
        val returnSort = smtPrimitive.smtPrimitiveSortName
        result.appendLine(
            "(define-fun ${name.firstCharLower()} ((id Int)) $returnSort $iteStructure)")
      }
      // Generate member definition for lists
      SmtIntermediateMemberType.REFERENCE_LIST -> {
        if (solver == SmtSolver.YICES && memberInfo.listArgumentClass == String::class.java) {
          // Yices does not support strings
          continue
        }
        // Generate member to list mapping
        val iteStructure =
            generateEqualsITEStructure(
                smtIntermediateMember.entries,
                "id",
                { ifEntry -> "${wrapper.smtIDToExternalID[ifEntry.component1()]!!}" },
                { thenEntry ->
                  "${wrapper.smtIDToExternalID[(thenEntry.component2() as SmtIntermediateMember.List).refID]}"
                },
                termForMinusOne)
        result.appendLine("(define-fun ${name.firstCharLower()} ((id Int)) Int $iteStructure)")
        // Generate list membership function
        val iteStructure2 =
            generateEqualsITEStructure(
                smtIntermediateMember.entries,
                "listId",
                { ifEntry ->
                  "${wrapper.smtIDToExternalID[(ifEntry.component2() as SmtIntermediateMember.List.ReferenceList).refID]!!}"
                },
                { thenEntry ->
                  val list =
                      (thenEntry.component2() as SmtIntermediateMember.List.ReferenceList).list
                  if (list.isNotEmpty()) {
                    generateEqualsITEStructure(
                        list,
                        "elemId",
                        { ifEntry -> "${wrapper.smtIDToExternalID[ifEntry]}" },
                        { _ -> "true" },
                        "false")
                  } else {
                    // TODO: Maybe cut out these entry, because default value is always false
                    "false"
                  }
                },
                "false")
        result.appendLine(
            "(define-fun in_${name.firstCharLower()} ((listId Int) (elemId Int)) Bool $iteStructure2)")
        // Generate list size function
        val iteStructure3 =
            generateEqualsITEStructure(
                smtIntermediateMember.entries,
                "listId",
                { ifEntry -> "${wrapper.smtIDToExternalID[ifEntry.component1()]!!}" },
                { thenEntry ->
                  "${(thenEntry.component2() as SmtIntermediateMember.List.ReferenceList).list.size}"
                },
                termForMinusOne)
        result.appendLine(
            "(define-fun size_${name.firstCharLower()} ((listId Int)) Int $iteStructure3)")
      }
      SmtIntermediateMemberType.VALUE_LIST -> {
        TODO()
      }
    }
  }
  result.appendLine()

  result.appendLine("; Information about the ticks")
  val indexToTick = wrapper.listOfChronologicalTicks.mapIndexed { index, tick -> index to tick }
  val iteStructure4 =
      generateEqualsITEStructure(
          indexToTick,
          "tickIndex",
          { ifEntry -> "${ifEntry.first}" },
          { thenEntry ->
            "${wrapper.smtIDToExternalID[thenEntry.second.getSmtID()]!!}"
          },
          termForMinusOne)
  result.appendLine("(define-fun indexToTick ((tickIndex Int)) Int $iteStructure4)")
  result.appendLine()

  return result.toString()
}
