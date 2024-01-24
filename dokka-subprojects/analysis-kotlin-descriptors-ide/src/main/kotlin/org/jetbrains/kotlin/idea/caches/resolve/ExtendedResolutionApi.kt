/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.NoDescriptorForDeclarationException


internal fun KtElement.getResolutionFacade(): ResolutionFacade = KotlinCacheService.getInstance(project).getResolutionFacade(this)

/**
 * This function first uses declaration resolvers to resolve this declaration and/or additional declarations (e.g. its parent),
 * and then takes the relevant descriptor from binding context.
 * The exact set of declarations to resolve depends on bodyResolveMode
 */
internal fun KtDeclaration.resolveToDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): DeclarationDescriptor? {
    //TODO: BodyResolveMode.PARTIAL is not quite safe!
    val context = safeAnalyze(resolutionFacade, bodyResolveMode)
    return if (this is KtParameter && hasValOrVar()) {
        context.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, this)
        // It is incorrect to have `val/var` parameters outside the primary constructor (e.g., `fun foo(val x: Int)`)
        // but we still want to try to resolve in such cases.
            ?: context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)
    } else {
        context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, this)
    }
}

internal fun KtParameter.resolveToParameterDescriptorIfAny(bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL) =
    resolveToParameterDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

internal fun KtParameter.resolveToParameterDescriptorIfAny(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): ValueParameterDescriptor? {
    val context = safeAnalyze(resolutionFacade, bodyResolveMode)
    return context.get(BindingContext.VALUE_PARAMETER, this) as? ValueParameterDescriptor
}

internal fun KtDeclaration.resolveToDescriptorIfAny(
    bodyResolveMode: BodyResolveMode = BodyResolveMode.PARTIAL
): DeclarationDescriptor? =
    resolveToDescriptorIfAny(getResolutionFacade(), bodyResolveMode)

internal fun KtElement.analyze(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext = resolutionFacade.analyze(this, bodyResolveMode)

internal fun KtElement.safeAnalyze(
    resolutionFacade: ResolutionFacade,
    bodyResolveMode: BodyResolveMode = BodyResolveMode.FULL
): BindingContext = try {
    analyze(resolutionFacade, bodyResolveMode)
} catch (e: Exception) {
    e.returnIfNoDescriptorForDeclarationException { BindingContext.EMPTY }
}

internal inline fun <T> Exception.returnIfNoDescriptorForDeclarationException(
    crossinline condition: (Boolean) -> Boolean = { v -> v },
    crossinline computable: () -> T
): T =
    if (condition(this.isItNoDescriptorForDeclarationException)) {
        computable()
    } else {
        throw this
    }

internal val Exception.isItNoDescriptorForDeclarationException: Boolean
    get() = this is NoDescriptorForDeclarationException || (cause as? Exception)?.isItNoDescriptorForDeclarationException == true
