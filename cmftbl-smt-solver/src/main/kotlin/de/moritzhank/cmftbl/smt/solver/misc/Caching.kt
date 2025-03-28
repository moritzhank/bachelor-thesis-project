package de.moritzhank.cmftbl.smt.solver.misc

import java.lang.ref.SoftReference
import kotlin.reflect.KClass

/*
 * The code below is a modified version of the caching implementation
 * of kotlinx.serialization, which is licensed under Apache 2.0.
 * The unmodified version can be found at:
 * https://github.com/Kotlin/kotlinx.serialization/blob/d4d066d72a9f92f06c640be5a36a22f75d0d7659/core/jvmMain/src/kotlinx/serialization/internal/Caching.kt
 */

/** Maps classes to values without classloader leak. */
class ClassValueCache<T> {

  private val classValue = ClassValueReferences<CacheEntry<T>>()

  /** If [key] is not in the cache, [factory] is called. */
  fun getOrSet(key: KClass<*>, factory: () -> T): T =
      classValue.getOrSet(key.java) { CacheEntry(factory()) }.content
}

private class ClassValueReferences<T> : ClassValue<MutableSoftReference<T>>() {

  override fun computeValue(type: Class<*>): MutableSoftReference<T> = MutableSoftReference()

  inline fun getOrSet(key: Class<*>, crossinline factory: () -> T): T {
    val ref: MutableSoftReference<T> = get(key)
    ref.reference.get()?.let {
      return it
    }
    return ref.getOrSetWithLock { factory() }
  }
}

private class CacheEntry<T>(@JvmField val content: T)

private class MutableSoftReference<T> {

  @JvmField @Volatile var reference: SoftReference<T> = SoftReference(null)

  @Synchronized
  fun getOrSetWithLock(factory: () -> T): T {
    reference.get()?.let {
      return it
    }
    val value = factory()
    reference = SoftReference(value)
    return value
  }
}
