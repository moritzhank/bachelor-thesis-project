@file:Suppress("unnecessary_not_null_assertion")

package de.moritzhank.cmftbl.smt.solver.translation.data.encoding

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.SerializersModule
import de.moritzhank.cmftbl.smt.solver.translation.data.*

/**
 * Abstracts the verification and handling of the next serializable value ([nextSerializable]).
 * - Serialized elements must inherit from [SmtTranslatableBase]
 * - Check that the list of members from the serializer matches the [SmtTranslationClassInfo]
 * - Delegate already serialized elements to [encodeAlreadyVisitedMember]
 * - Maintenance of [nextSerializable] for further processing
 * - Delegate encoding of primitive values to [encodePrimitiveValue]
 *
 * @param nextSerializable Without it, it would not be possible to get the actual object down the
 *   serialization process
 */
@ExperimentalSerializationApi
internal abstract class AbstractSmtDataEncoder(
    protected val result: MutableList<SmtIntermediateRepresentation>,
    protected val visitedSmtIDs: MutableMap<Int, Boolean>,
    override val serializersModule: SerializersModule,
    protected var nextSerializable: Any? = null
) : AbstractEncoder() {

  /** This function is called for each member encountered that has already been serialized. */
  abstract fun encodeAlreadyVisitedMember(member: SmtIntermediateMember.Reference)

  /** This function is called for each primitive value that has to be serialized. */
  abstract fun encodePrimitiveValue(value: Any, primitive: SmtPrimitive?)

  /**
   * Verifies all requirements for serialized elements and handles already visited elements.
   * IMPORTANT: [compareMembersFromSerializerAgainstClassInfo] is NOT performed for the root object!
   */
  final override fun <T> encodeSerializableElement(
      descriptor: SerialDescriptor,
      index: Int,
      serializer: SerializationStrategy<T>,
      value: T
  ) {
    nextSerializable = null
    requireNotNull(value) {
      "The current serialized value should not be null. This is probably a bug in kotlinx.serialization."
    }
    val elemDescriptor = descriptor.getElementDescriptor(index)
    if (elemDescriptor.kind == StructureKind.CLASS) {
      require(value is SmtTranslatableBase) {
        val className = value!!::class.simpleName ?: "<unknown_class>"
        val memberName = "${descriptor.serialName}.${descriptor.getElementName(index)}"
        "The class \"$className\" (Accessed via \"$memberName\") has to inherit from SmtTranslatableBase in order to be serialized."
      }
      // TODO: This is not verified for root object
      // TODO: Performance hit of about 250ms
      compareMembersFromSerializerAgainstClassInfo(
          elemDescriptor, value.getSmtTranslationClassInfo()) { l1, l2 ->
            val className = value!!::class.simpleName ?: "<unknown_class>"
            val memberName = "${descriptor.serialName}.${descriptor.getElementName(index)}"
            "The list of expected members does not match the serialised members for \"$className\" (Accessed via \"$memberName\"). Expected: $l1. Found: $l2. This may be due to incorrect configuration of a custom serializer or missing @Transient annotations."
          }
      val smtID = value.getSmtID()
      if (visitedSmtIDs[smtID] == true) {
        encodeAlreadyVisitedMember(SmtIntermediateMember.Reference(smtID))
        return
      }
      // TODO: Investigate origin and move or remove
      // require(elemDescriptor.kind != StructureKind.LIST) {
      //  val memberName = "${descriptor.serialName}.${descriptor.getElementName(index)}"
      //  "Generic (except lists), anonymous and polymorphic classes can not be translated. You can
      // solve this by annotating \"$memberName}\" with @Transient."
      // }
    }
    nextSerializable = value
    super.encodeSerializableElement(descriptor, index, serializer, value)
  }

  final override fun <T : Any> encodeNullableSerializableElement(
      descriptor: SerialDescriptor,
      index: Int,
      serializer: SerializationStrategy<T>,
      value: T?
  ) {
    error("Nullable elements are not supported yet by the serialization.")
  }

  final override fun encodeValue(value: Any) {
    nextSerializable = null
    encodePrimitiveValue(value, value.smtPrimitive())
  }

  private fun compareMembersFromSerializerAgainstClassInfo(
      descriptor: SerialDescriptor,
      classInfo: SmtTranslationClassInfo,
      lazyMessage: (List<String>, List<String>) -> Any
  ) {
    val descriptorElementNames = descriptor.elementNames.toList()
    val classInfoElementNames = mutableListOf<String>()
    classInfo.getTranslatableProperties().fold(classInfoElementNames) { list, elem ->
      list.apply { add(elem.name) }
    }
    require(descriptorElementNames == classInfoElementNames) {
      lazyMessage(classInfoElementNames, descriptorElementNames)
    }
  }
}
