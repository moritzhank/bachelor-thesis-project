package de.moritzhank.cmftbl.smt.solver.translation.data.encoding

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import de.moritzhank.cmftbl.smt.solver.translation.data.*

/**
 * @param memberNames Used to be able to correctly set the serialised member in
 *   [SmtIntermediateMember]
 * @param currentMemberIndex Used to be able to correctly set the serialised member in
 *   [SmtIntermediateMember]
 * @param listIntermediateMember Needed to be able to collect list elements during serialization
 */
@OptIn(ExperimentalSerializationApi::class)
internal class SmtDataEncoder(
    result: MutableList<SmtIntermediateRepresentation>,
    visitedSmtIDs: MutableMap<Int, Boolean>,
    serializersModule: SerializersModule,
    // Changing parameters:
    private val current: SmtIntermediateRepresentation?,
    private var currentMemberIndex: Int,
    private val memberNames: Array<String>,
    private var listIntermediateMember: SmtIntermediateMember.List?,
    private val serializationMode: SmtDataSerializationMode = SmtDataSerializationMode.DEFAULT,
    nextSerializable: Any? = null,
) : AbstractSmtDataEncoder(result, visitedSmtIDs, serializersModule, nextSerializable) {

  private val defaultErrorMessage = {
    "An unexpected error occurred during the serialization of an object."
  }

  /** Sets the currently serialized Member to [member]. */
  private fun setMember(member: SmtIntermediateMember) {
    requireNotNull(current, defaultErrorMessage)
    current.members[memberNames[currentMemberIndex++]] = member
  }

  /**
   * Encode [obj] dependent on [serializationMode] (Used in [beginStructure]).
   *
   * @param memberNames Needed for translation process (See [SmtDataEncoder])
   */
  private fun encodeSmtTranslatableBase(
      obj: SmtTranslatableBase,
      memberNames: Array<String>
  ): CompositeEncoder {
    val smtID = obj.getSmtID()
    visitedSmtIDs[smtID] = true
    val intermediateRepresentation = SmtIntermediateRepresentation(obj)
    if (serializationMode == SmtDataSerializationMode.DEFAULT) {
      // currentMemberIndex is zero for the root object
      if (currentMemberIndex >= 0) {
        setMember(SmtIntermediateMember.Reference(smtID))
      }
    } else {
      val listMembers = this.listIntermediateMember
      require(listMembers is SmtIntermediateMember.List.ReferenceList) {
        "An unexpected error occurred during the serialization of a list of non-primitive values."
      }
      listMembers.list.add(intermediateRepresentation.ref.getSmtID())
    }
    result.add(intermediateRepresentation)
    return SmtDataEncoder(
        result, visitedSmtIDs, serializersModule, intermediateRepresentation, 0, memberNames, null)
  }

  /**
   * Encodes [nextSerializable] as a list (Used in [beginStructure]).
   * - Retrieve generic argument class of list
   * - Call [setMember] with [SmtIntermediateMember.List.ValueList] or
   *   [SmtIntermediateMember.List.ReferenceList]
   * - Setup new [SmtDataEncoder] for encoding of list elements
   */
  private fun encodeNextSerializableAsList(): CompositeEncoder {
    val nextSerializable = this.nextSerializable
    require(nextSerializable is List<*>) {
      "An unexpected error occurred during the serialization of a list."
    }
    val newSmtID = SmtTranslatableBase.uniqueSmtID()
    // Nested lists are not allowed up to this point (so current is not null)
    // TODO: Performance hit of about 300ms
    val typeArgument =
        current!!
            .ref
            .getSmtTranslationClassInfo()
            .getTranslatableProperties()
            .getOrNull(currentMemberIndex)
            ?.listTypeArgumentClass
    // TODO: Change error message:
    requireNotNull(typeArgument, defaultErrorMessage)
    val primitive = typeArgument.smtPrimitive()
    val intermediateListMember =
        if (primitive != null) {
          SmtIntermediateMember.List.ValueList(newSmtID, primitive, mutableListOf())
        } else {
          SmtIntermediateMember.List.ReferenceList(newSmtID, mutableListOf())
        }
    if (serializationMode == SmtDataSerializationMode.DEFAULT) {
      setMember(intermediateListMember)
    } else {
      // This branch should not be taken, because nested lists are not allowed up to this point
      require(false, defaultErrorMessage)
      // val listMembers = this.listIntermediateMember
      // require(listMembers is SmtIntermediateMember.List.ReferenceList) {
      //   "An unexpected error occurred during the serialization of a nested list."
      // }
      // listMembers.list.add(intermediateListMember.refID)
    }
    return SmtDataEncoder(
        result,
        visitedSmtIDs,
        serializersModule,
        null,
        -1,
        arrayOf(),
        intermediateListMember,
        SmtDataSerializationMode.LIST)
  }

  /** Already visited members are encoded with their refID dependent on [serializationMode]. */
  override fun encodeAlreadyVisitedMember(member: SmtIntermediateMember.Reference) {
    if (serializationMode == SmtDataSerializationMode.DEFAULT) {
      setMember(member)
    } else {
      val listMembers = this.listIntermediateMember
      require(listMembers is SmtIntermediateMember.List.ReferenceList) {
        "An unexpected error occurred during the serialization of a list of non-primitive values."
      }
      listMembers.list.add(member.refID)
    }
  }

  /** This function is called for the serialization of all primitive elements. */
  override fun encodePrimitiveValue(value: Any, primitive: SmtPrimitive?) {
    // TODO: Enums are currently encoded as ints
    if (serializationMode == SmtDataSerializationMode.DEFAULT) {
      requireNotNull(primitive) { "An unexpected non-primitive value has occurred." }
      setMember(SmtIntermediateMember.Value(value, primitive))
    } else {
      val listMembers = this.listIntermediateMember
      require(listMembers is SmtIntermediateMember.List.ValueList) {
        "An unexpected error occurred during the serialization of a list of primitive values."
      }
      require(value.smtPrimitive() == listMembers.primitive) {
        "An unexpected primitive type was encountered when serialising a list of primitive values."
      }
      listMembers.list.add(value)
    }
  }

  /** This function is called for the serialization of all non-primitive elements. */
  override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
    val nextSerializable = this.nextSerializable
    requireNotNull(nextSerializable, defaultErrorMessage)
    // Encode list
    if (descriptor.kind == StructureKind.LIST) {
      return encodeNextSerializableAsList()
    }
    // Throw error when encoding elements other than lists and classes
    val kind = descriptor.kind
    require(kind == StructureKind.CLASS, defaultErrorMessage)
    // Encode class
    require(nextSerializable is SmtTranslatableBase, defaultErrorMessage)
    return encodeSmtTranslatableBase(
        nextSerializable, descriptor.elementNames.toList().toTypedArray())
  }
}
