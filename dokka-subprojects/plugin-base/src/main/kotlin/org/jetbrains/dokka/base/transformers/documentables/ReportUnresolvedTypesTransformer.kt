/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer

/**
 * Reports unresolved types found in the documentable model:
 *
 * ```
 * `Foo` is unresolved in file:///path/to/File.kt:1:1
 * ```
 *
 * Unresolved types are detected by [UnresolvedTypeExtra] attached by the analysis backend
 * (or by the [UnresolvedBound] itself for backends that do not attach the extra, e.g. Java).
 *
 * Being a [PreMergeDocumentableTransformer] that runs after the filtering transformers
 * (visibility, suppression, etc.), it does not report unresolved types of declarations
 * that are not documented (e.g. private members) -
 * https://github.com/Kotlin/dokka/issues/4620
 */
internal class ReportUnresolvedTypesTransformer(
    private val context: DokkaContext
) : PreMergeDocumentableTransformer {

    override fun invoke(modules: List<DModule>): List<DModule> = modules.map { module ->
        module.also {
            val reported = mutableSetOf<String>()

            module.withDescendants().forEach { documentable ->
                typePositions(documentable).forEach { bound ->
                    bound.forEachUnresolved { unresolved ->
                        val warning = unresolved.warningFor(documentable)
                        if (reported.add(warning)) {
                            context.logger.warn(warning)
                        }
                    }
                }
            }
        }
    }

    private fun Bound.warningFor(documentable: Documentable): String {
        val extra = when (this) {
            is UnresolvedBound -> extra[UnresolvedTypeExtra]
            is GenericTypeConstructor -> extra[UnresolvedTypeExtra]
            else -> null
        }
        return if (extra != null) {
            "${extra.message} in ${extra.location}"
        } else { // Java
            val name = (this as? UnresolvedBound)?.name ?: toString()
            "`$name` is unresolved in ${documentable.sourceSetAwareDescription(documentable.sourceSets)}"
        }
    }

    private fun typePositions(documentable: Documentable): Sequence<Bound> = sequence {
        if (documentable is WithSupertypes) {
            yieldAll(documentable.supertypes.values.flatten().map { it.typeConstructor })
        }
        if (documentable is WithType) {
            yield(documentable.type)
        }
        if (documentable is DTypeAlias) {
            yieldAll(documentable.underlyingType.values)
        }
        if (documentable is Callable) {
            documentable.receiver?.let { yield(it.type) }
        }
        if (documentable is DProperty) {
            // Accessors are not included in [DProperty.children]
            documentable.getter?.let { yieldAll(typePositions(it)) }
            documentable.setter?.let { yieldAll(typePositions(it)) }
        }
        if (documentable is WithGenerics) {
            documentable.generics.forEach { yieldAll(it.bounds) }
        }
    }

    private fun Bound.forEachUnresolved(action: (Bound) -> Unit) {
        when (this) {
            is UnresolvedBound -> action(this)
            is GenericTypeConstructor -> {
                if (extra[UnresolvedTypeExtra] != null) action(this)
                projections.forEach { it.forEachUnresolved(action) }
            }
            is FunctionalTypeConstructor -> projections.forEach { it.forEachUnresolved(action) }
            is Nullable -> inner.forEachUnresolved(action)
            is DefinitelyNonNullable -> inner.forEachUnresolved(action)
            is TypeAliased -> {
                typeAlias.forEachUnresolved(action)
                inner.forEachUnresolved(action)
            }

            Dynamic -> Unit
            is JavaObject -> Unit
            is PrimitiveJavaType -> Unit
            is TypeParameter -> Unit
            Void -> Unit
        }
    }

    private fun Projection.forEachUnresolved(action: (Bound) -> Unit) = when (this) {
        is Bound -> forEachUnresolved(action)
        is Variance<*> -> inner.forEachUnresolved(action)
        is Star -> {}
    }
}
