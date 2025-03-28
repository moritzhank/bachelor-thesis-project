package de.moritzhank.cmftbl.smt.solver.translation.data.encoding

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.serializer
import de.moritzhank.cmftbl.smt.solver.translation.data.smtTranslationClassInfo
import kotlin.reflect.KClass


@ExperimentalSerializationApi
fun buildSafeSerialDescriptor(
  kClass: KClass<*>,
  name: String = kClass.simpleName!!,
  exceptionalSerializer: (Class<*>) -> KSerializer<*>?
): SerialDescriptor {
  val smtTransProps = smtTranslationClassInfo(kClass).getTranslatableProperties()
  val smtTransPropsNames = smtTransProps.map { it.name }.toTypedArray()
  val elemToDescriptor = Array<Lazy<SerialDescriptor>>(smtTransPropsNames.size) { i ->
    val propClazz = smtTransProps[i].clazz
    val serializer = exceptionalSerializer(propClazz)
    lazy {
      serializer?.descriptor ?: serializer(propClazz).descriptor
    }
  }
  return object: SerialDescriptor {
    override val serialName: String = name
    override val kind: SerialKind = StructureKind.CLASS
    override val elementsCount: Int = smtTransPropsNames.size
    override fun getElementName(index: Int) = smtTransPropsNames[index]
    // Can maybe be precomputed
    override fun getElementIndex(name: String) : Int {
      val i = smtTransPropsNames.indexOf(name)
      return if (i == -1) CompositeDecoder.UNKNOWN_NAME else i
    }
    override fun getElementAnnotations(index: Int): List<Annotation> = listOf()
    override fun getElementDescriptor(index: Int): SerialDescriptor = elemToDescriptor[index].value
    override fun isElementOptional(index: Int) = false
  }
}