/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation.kdp

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.kotlin.documentation.KdActuality
import org.jetbrains.kotlin.documentation.KdModality
import org.jetbrains.kotlin.documentation.KdVisibility

internal fun WithVisibility.kdVisibility(
    sourceSet: DokkaConfiguration.DokkaSourceSet
): KdVisibility = when (visibility[sourceSet]) {
    JavaVisibility.Default -> KdVisibility.PACKAGE_PRIVATE
    JavaVisibility.Private -> KdVisibility.PRIVATE
    JavaVisibility.Protected -> KdVisibility.PACKAGE_PROTECTED
    JavaVisibility.Public -> KdVisibility.PUBLIC

    KotlinVisibility.Internal -> KdVisibility.INTERNAL
    KotlinVisibility.Private -> KdVisibility.PRIVATE
    KotlinVisibility.Protected -> KdVisibility.PROTECTED
    KotlinVisibility.Public -> KdVisibility.PUBLIC
    null -> KdVisibility.PUBLIC // safe default?
}

internal fun WithAbstraction.kdModality(
    sourceSet: DokkaConfiguration.DokkaSourceSet
): KdModality = when (modifier[sourceSet]) {
    JavaModifier.Abstract -> KdModality.ABSTRACT
    JavaModifier.Empty -> KdModality.OPEN
    JavaModifier.Final -> KdModality.FINAL
    KotlinModifier.Abstract -> KdModality.ABSTRACT
    KotlinModifier.Empty -> KdModality.FINAL
    KotlinModifier.Final -> KdModality.FINAL
    KotlinModifier.Open -> KdModality.OPEN
    KotlinModifier.Sealed -> KdModality.SEALED
    null -> KdModality.FINAL // safe default?
}

internal fun <T> T.kdActuality(
    sourceSet: DokkaConfiguration.DokkaSourceSet
): KdActuality? where T : Documentable, T : WithIsExpectActual = when {
    !isExpectActual -> null
    expectPresentInSet == sourceSet -> KdActuality.EXPECT
    else -> KdActuality.ACTUAL
}


internal fun <T : Any> List<T>.singleOrNullIfEmpty(): T? = when (size) {
    0 -> null
    else -> single()
}

internal fun Documentable.extraModifiers(
    sourceSet: DokkaConfiguration.DokkaSourceSet
): Set<ExtraModifiers> {
    @Suppress("UNCHECKED_CAST")
    this as WithExtraProperties<out Documentable>
    return extra[AdditionalModifiers]?.content?.get(sourceSet).orEmpty()
}

// TODO: ignore file level annotations?
internal fun Documentable.directAnnotations(
    sourceSet: DokkaConfiguration.DokkaSourceSet
): List<Annotations.Annotation> {
    @Suppress("UNCHECKED_CAST")
    this as WithExtraProperties<out Documentable>
    return extra[Annotations]?.directAnnotations?.get(sourceSet).orEmpty()
}
