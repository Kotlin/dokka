package org.jetbrains.dokka.uitest.types

/**
 * Documentation for an annotation class
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
@MustBeDocumented
annotation class SimpleKotlinAnnotationClass
