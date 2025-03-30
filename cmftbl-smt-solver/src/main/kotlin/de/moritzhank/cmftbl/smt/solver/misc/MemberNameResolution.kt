package de.moritzhank.cmftbl.smt.solver.misc

import de.moritzhank.cmftbl.smt.solver.dsl.CCB
import de.moritzhank.cmftbl.smt.solver.dsl.CallContext
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

internal fun CallContext<*, *>.toSmtRepresentation(elem: String): String {
  var elem = elem
  val ccs = toOrderedList().toMutableList()
  val base = ccs.removeFirst() as CCB<*>
  var kClass = base.kClass
  for (cc in ccs) {
    val callerName = kClass.simpleName!!.firstCharLower()
    val memberName = cc.memberName()
    elem = "(${callerName}_$memberName $elem)"
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