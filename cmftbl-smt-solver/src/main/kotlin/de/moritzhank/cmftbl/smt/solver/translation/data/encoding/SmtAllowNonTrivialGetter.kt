package de.moritzhank.cmftbl.smt.solver.translation.data.encoding

/**
 * If you want to allow non-trivial getters, you will need to write a custom serializer that
 * includes the property with the custom getter. Important: The Smt-translation process requires the
 * members to be in the order of their declaration.
 */
@Target(AnnotationTarget.PROPERTY) annotation class SmtAllowNonTrivialGetter
