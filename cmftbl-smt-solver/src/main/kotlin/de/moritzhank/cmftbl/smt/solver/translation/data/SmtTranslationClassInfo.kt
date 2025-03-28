@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.translation.data

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.javaType
import kotlinx.metadata.isNotDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Transient
import de.moritzhank.cmftbl.smt.solver.translation.data.encoding.SmtAllowNonTrivialGetter
import de.moritzhank.cmftbl.smt.solver.misc.ClassValueCache
import de.moritzhank.cmftbl.smt.solver.misc.getKmProperties
import de.moritzhank.cmftbl.smt.solver.misc.getSimpleName
import de.moritzhank.cmftbl.smt.solver.misc.resolveClassAndGenericArgumentClass

/** Stores a list of translatable properties of a class. */
internal class SmtTranslationClassInfo(
    /** Name of the translated class. */
    private val translationName: String,
    private val properties: Array<Property>
) {

  fun isTranslatableProperty(name: String): Boolean = properties.any { it.name == name }

  fun requireTranslatableProperty(propName: String) {
    require(isTranslatableProperty(propName)) {
      "The property \"$translationName.$propName\" can not be translated. This can happen, for example, due to non-trivial getters."
    }
  }

  fun getTranslatableProperties() = properties

  fun getTranslationName() = translationName

  class Property(
      val name: String,
      val nonTrivialGetter: Boolean,
      val clazz: Class<*>,
      val listTypeArgumentClass: Class<*>? = null
  )
}

internal val SMT_TRANSLATION_CACHE = ClassValueCache<SmtTranslationClassInfo>()

/**
 * Generate SmtTranslationClassInfo for [kClass] or get it from [SMT_TRANSLATION_CACHE]. IMPORTANT:
 * This will throw an error if called on non-Kotlin classes such as lists.
 */
@OptIn(ExperimentalStdlibApi::class)
internal fun <T : Any> smtTranslationClassInfo(kClass: KClass<T>): SmtTranslationClassInfo {
  // Lambda expression to calculate the SmtTranslationClassInfo for kClass
  val smtTranslationClassInfoFactory: () -> SmtTranslationClassInfo = {
    val translationName: String =
        kClass.findAnnotation<SerialName>()?.value ?: getSimpleName(kClass)
    val translatableProperties = mutableListOf<SmtTranslationClassInfo.Property>()
    for (kmProperty in getKmProperties(kClass)) {
      // Get the kProperty associated with kmProperty
      val kProperty = kClass.memberProperties.find { it.name == kmProperty.name }!!
      // Skip properties with Transient annotation
      if (kProperty.hasAnnotation<Transient>()) {
        continue
      }
      // Skip non-trivial getter if they are not annotated with SmtAllowNonTrivialGetter
      var isNonTrivialGetter = false
      if (kmProperty.getter.isNotDefault) {
        if (!kProperty.hasAnnotation<SmtAllowNonTrivialGetter>()) {
          continue
        }
        isNonTrivialGetter = true
      }
      // Resolve the class of the property and, if possible, the generic type
      val resolvedClassPair =
          try {
            resolveClassAndGenericArgumentClass(kProperty.returnType.javaType)
          } catch (err: IllegalStateException) {
            // Ignoring any type that is no class
            continue
          } catch (err: IllegalArgumentException) {
            // Ignoring any generic type whose generic argument is itself generic or no class
            continue
          }
      var clazz = resolvedClassPair.first
      val genericArgumentClass = resolvedClassPair.second
      // Lambda expression to check if class is translatable
      val isTranslatable: (Class<*>) -> Boolean = { !it.isSealed && !it.isInterface }
      // Ignore non-generic classes that cannot be translated
      if (genericArgumentClass == null && !isTranslatable(clazz)) {
        continue
      }
      if (genericArgumentClass != null) {
        // Ignore all generic types that are not lists
        if (clazz.typeName != List::class.java.name) {
          continue
        }
        // Ignore all generic types where the generic argument cannot be translated
        if (!isTranslatable(genericArgumentClass)) {
          continue
        }
      }
      // Override enums to be integers
      if (clazz.isEnum) {
        clazz = Int::class.java
      }
      val newProperty =
          SmtTranslationClassInfo.Property(
              kProperty.name, isNonTrivialGetter, clazz, genericArgumentClass)
      translatableProperties.add(newProperty)
    }
    SmtTranslationClassInfo(translationName, translatableProperties.toTypedArray())
  }
  return SMT_TRANSLATION_CACHE.getOrSet(kClass, smtTranslationClassInfoFactory)
}
