/**
 * Pretend to be from `com.intellij.psi` in order to access package-private declarations
 */
@file:Suppress("PackageDirectoryMismatch")
package com.intellij.psi

import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue

/*
 * For some reason `PsiAnnotationConstantValue` is package private,
 * even though it's returned in public API calls.
 */
internal fun JvmAnnotationAttributeValue.isPsiAnnotationConstantValue() = this is PsiAnnotationConstantValue
internal fun JvmAnnotationAttributeValue.getPsiAnnotationConstantValue(): Any? =
    (this as PsiAnnotationConstantValue).constantValue
