@file:Suppress("unused")

package de.moritzhank.cmftbl.smt.solver.misc

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlinx.metadata.KmProperty
import kotlinx.metadata.jvm.KotlinClassMetadata

/** Get simple name of [kClass] with guarantee. */
internal fun getSimpleName(kClass: KClass<*>): String {
  val name = kClass.simpleName
  requireNotNull(name) { "No simple name could be found for \"$kClass\"." }
  return name
}

/** Get a list of KmProperties for [kClass]. */
internal fun getKmProperties(kClass: KClass<*>): List<KmProperty> {
  // Adapted from
  // https://discuss.kotlinlang.org/t/reflection-and-properties-checking-for-custom-getters-setters/22457/2
  val kotlinClassMetadata =
      try {
        KotlinClassMetadata.readStrict(kClass.java.getAnnotation(Metadata::class.java))
      } catch (e: NullPointerException) {
        // Error message rewritten to be more meaningful
        error("Failed to retrieve Kotlin metadata for '$kClass'.")
      }
  return (kotlinClassMetadata as KotlinClassMetadata.Class).kmClass.properties
}

/**
 * @return A pair of the resolved class and the resolved class of a possible generic argument (in
 *   this order)
 * @throws IllegalArgumentException if the generic type is itself generic or not a class
 * @throws IllegalStateException if the given [type] is no class
 */
internal fun resolveClassAndGenericArgumentClass(type: Type): Pair<Class<*>, Class<*>?> {
  if (type is Class<*>) {
    return Pair(type, null)
  } else if (type is ParameterizedType) {
    val genericType = type.actualTypeArguments.firstOrNull()
    require(genericType is Class<*>) {
      "A generic type which is itself a generic type or not a class is not currently supported."
    }
    return Pair(type.rawType as Class<*>, genericType)
  }
  error("The given type \"${type.typeName}\" is no class.")
}
