package de.moritzhank.cmftbl.smt.solver.misc

import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.dsl.CallContext
import de.moritzhank.cmftbl.smt.solver.dsl.Callable1CallContext
import de.moritzhank.cmftbl.smt.solver.dsl.Callable2CallContext
import de.moritzhank.cmftbl.smt.solver.dsl.Callable3CallContext
import de.moritzhank.cmftbl.smt.solver.dsl.PropertyCallContext
import de.moritzhank.cmftbl.smt.solver.dsl.memberName
import de.moritzhank.cmftbl.smt.solver.translation.data.smtTranslationClassInfo
import kotlin.reflect.KClass

internal fun CCB<*>.tickMemberName(): String {
  val translationClassInfo = smtTranslationClassInfo(kClass)
  require(translationClassInfo.isEntityType)
  return "${kClass.simpleName!!.firstCharLower()}_tickData"
}

internal fun CCB<*>.idMemberName(): String {
  val translationClassInfo = smtTranslationClassInfo(kClass)
  require(translationClassInfo.isEntityType)
  return "${kClass.simpleName!!.firstCharLower()}_id"
}

internal fun CallContext<*, *>.toSmtRepresentation(baseElem: (CCB<*>) -> String): String {
  val ccs = toOrderedList().toMutableList()
  val base = ccs.removeFirst() as CCB<*>
  var elem = baseElem(base)
  var kClass = base.kClass
  for (cc in ccs) {
    val callerName = kClass.simpleName!!.firstCharLower()
    val memberName = cc.memberName()
    when (cc) {
      is CCB<*> -> error("This path should not be reachable.")
      is PropertyCallContext,  is Callable1CallContext<*, *> -> {
        elem = "(${callerName}_$memberName $elem)"
      }
      is Callable2CallContext<*, *, *> -> {
        val paramStr = cc.param.toSmtRepresentation(baseElem)
        elem = "(${callerName}_$memberName $elem $paramStr)"
      }
      is Callable3CallContext<*, *, *, *> -> {
        val param1Str = cc.param1.toSmtRepresentation(baseElem)
        val param2Str = cc.param2.toSmtRepresentation(baseElem)
        elem = "(${callerName}_$memberName $elem $param1Str $param2Str)"
      }
    }
    kClass = kClass.members.find { it.name == memberName }!!.returnType.classifier as KClass<*>
  }
  return elem
}

private fun CallContext<*, *>.toOrderedList(): List<CallContext<*, *>> {
  val result = mutableListOf<CallContext<*, *>>()
  var current = this
  while (current.before != null) {
    result.add(current)
    current = current.before!!
  }
  result.add(current)
  result.reverse()
  require(result.first() is CCB<*>)
  return result
}