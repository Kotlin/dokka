/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl

import com.intellij.psi.PsiClass
import kotlinx.coroutines.coroutineScope
import org.jetbrains.dokka.analysis.java.util.PsiDocumentableSource
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.DescriptorDocumentableSource
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.from
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.utilities.parallelForEach
import org.jetbrains.dokka.analysis.kotlin.internal.ClassHierarchy
import org.jetbrains.dokka.analysis.kotlin.internal.FullClassHierarchyBuilder
import org.jetbrains.dokka.analysis.kotlin.internal.Supertypes
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import java.util.concurrent.ConcurrentHashMap

internal class DescriptorFullClassHierarchyBuilder : FullClassHierarchyBuilder {

    override suspend fun build(module: DModule): ClassHierarchy = coroutineScope {
        val map = module.sourceSets.associateWith { ConcurrentHashMap<DRI, List<DRI>>() }
        module.packages.parallelForEach { visitDocumentable(it, map) }
        map
    }

    private suspend fun collectSupertypesFromKotlinType(
        driWithKType: Pair<DRI, KotlinType>,
        supersMap: MutableMap<DRI, Supertypes>
    ): Unit = coroutineScope {
        val (dri, kotlinType) = driWithKType
        val supertypes = kotlinType.immediateSupertypes().filterNot { it.isAnyOrNullableAny() }
        val supertypesDriWithKType = supertypes.mapNotNull { supertype ->
            supertype.constructor.declarationDescriptor?.let {
                DRI.from(it) to supertype
            }
        }

        if (supersMap[dri] == null) {
            // another thread can rewrite the same value, but it isn't a problem
            supersMap[dri] = supertypesDriWithKType.map { it.first }
            supertypesDriWithKType.parallelForEach { collectSupertypesFromKotlinType(it, supersMap) }
        }
    }

    private suspend fun collectSupertypesFromPsiClass(
        driWithPsiClass: Pair<DRI, PsiClass>,
        supersMap: MutableMap<DRI, Supertypes>
    ): Unit = coroutineScope {
        val (dri, psiClass) = driWithPsiClass
        val supertypes = psiClass.superTypes.mapNotNull { it.resolve() }
            .filterNot { it.qualifiedName == "java.lang.Object" }
        val supertypesDriWithPsiClass = supertypes.map { DRI.from(it) to it }

        if (supersMap[dri] == null) {
            // another thread can rewrite the same value, but it isn't a problem
            supersMap[dri] = supertypesDriWithPsiClass.map { it.first }
            supertypesDriWithPsiClass.parallelForEach { collectSupertypesFromPsiClass(it, supersMap) }
        }
    }

    private suspend fun visitDocumentable(
        documentable: Documentable,
        hierarchy: SourceSetDependent<MutableMap<DRI, List<DRI>>>
    ): Unit = coroutineScope {
        if (documentable is WithScope) {
            documentable.classlikes.parallelForEach { visitDocumentable(it, hierarchy) }
        }
        if (documentable is DClasslike) {
            // to build a full class graph, using supertypes from Documentable
            // is not enough since it keeps only one level of hierarchy
            documentable.sources.forEach { (sourceSet, source) ->
                if (source is DescriptorDocumentableSource) {
                    val descriptor = source.descriptor as? ClassifierDescriptorWithTypeParameters ?: return@forEach // it can be typealias as well
                    val type = descriptor.defaultType
                    hierarchy[sourceSet]?.let { collectSupertypesFromKotlinType(documentable.dri to type, it) }
                } else if (source is PsiDocumentableSource) {
                    val psi = source.psi as PsiClass
                    hierarchy[sourceSet]?.let { collectSupertypesFromPsiClass(documentable.dri to psi, it) }
                }
            }
        }
    }
}
