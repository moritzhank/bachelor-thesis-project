@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.translation.data

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import de.moritzhank.cmftbl.smt.solver.translation.data.encoding.SmtDataEncoder
import de.moritzhank.cmftbl.smt.solver.translation.data.encoding.SmtDataSerializationMode

/** Represents the serialization of [ref]. */
class SmtIntermediateRepresentation(
    val ref: SmtTranslatableBase,
    val members: MutableMap<String, SmtIntermediateMember> = mutableMapOf()
)

/** Represents a serialized member of [SmtIntermediateRepresentation]. */
sealed class SmtIntermediateMember {

  class Reference(val refID: Int) : SmtIntermediateMember()

  class Value(val value: Any, val primitive: SmtPrimitive) : SmtIntermediateMember()

  sealed class List(val refID: Int) : SmtIntermediateMember() {

    class ValueList(refID: Int, val primitive: SmtPrimitive, val list: MutableCollection<Any>) :
        List(refID)

    class ReferenceList(refID: Int, val list: MutableCollection<Int>) : List(refID)
  }

  fun type(): SmtIntermediateMemberType =
      when (this) {
        is Reference -> SmtIntermediateMemberType.REFERENCE
        is Value -> SmtIntermediateMemberType.VALUE
        is List.ValueList -> SmtIntermediateMemberType.VALUE_LIST
        is List.ReferenceList -> SmtIntermediateMemberType.REFERENCE_LIST
      }
}

/**
 * Enum to streamline the handling of [SmtIntermediateMember] in the following translation process.
 */
enum class SmtIntermediateMemberType {

  REFERENCE,
  VALUE,
  VALUE_LIST,
  REFERENCE_LIST
}

/** Generate the intermediate representation of [ref]. */
inline fun <reified T : SmtTranslatableBase> getSmtIntermediateRepresentation(
    serializersModule: SerializersModule,
    ref: T
): List<SmtIntermediateRepresentation> {
  val serializer = serializersModule.serializer<T>()
  return getSmtIntermediateRepresentation(serializer, serializersModule, ref)
}

/** Generate the intermediate representation of [ref]. */
fun <T : SmtTranslatableBase> getSmtIntermediateRepresentation(
    serializer: SerializationStrategy<T>,
    serializersModule: SerializersModule,
    ref: T
): List<SmtIntermediateRepresentation> {
  val result = mutableListOf<SmtIntermediateRepresentation>()
  val encoder =
      SmtDataEncoder(
          result,
          mutableMapOf(),
          serializersModule,
          SmtIntermediateRepresentation(ref),
          -1,
          arrayOf(),
          null,
          SmtDataSerializationMode.DEFAULT,
          ref)
  serializer.serialize(encoder, ref)
  return result
}
