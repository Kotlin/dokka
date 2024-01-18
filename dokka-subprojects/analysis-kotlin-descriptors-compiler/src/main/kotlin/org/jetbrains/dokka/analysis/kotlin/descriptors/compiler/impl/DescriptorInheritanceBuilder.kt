/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl

import com.intellij.psi.PsiClass
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.java.util.PsiDocumentableSource
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.DescriptorDocumentableSource
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.from
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.WithSources
import org.jetbrains.dokka.analysis.kotlin.internal.InheritanceBuilder
import org.jetbrains.dokka.analysis.kotlin.internal.InheritanceNode
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.resolve.DescriptorUtils.getClassDescriptorForType

internal class DescriptorInheritanceBuilder : InheritanceBuilder {

    override fun build(documentables: Map<DRI, Documentable>): List<InheritanceNode> {
        val descriptorMap = getDescriptorMap(documentables)

        val psiInheritanceTree =
            documentables.flatMap { (_, v) -> (v as? WithSources)?.sources?.values.orEmpty() }
                .filterIsInstance<PsiDocumentableSource>().mapNotNull { it.psi as? PsiClass }
                .flatMap(::gatherPsiClasses)
                .flatMap { entry -> entry.second.map { it to entry.first } }
                .let {
                    it + it.map { it.second to null }
                }
                .groupBy({ it.first }) { it.second }
                .map { it.key to it.value.filterNotNull().distinct() }
                .map { (k, v) ->
                    InheritanceNode(
                        DRI.from(k),
                        v.map { InheritanceNode(DRI.from(it)) },
                        k.supers.filter { it.isInterface }.map { DRI.from(it) },
                        k.isInterface
                    )

                }

        val descriptorInheritanceTree = descriptorMap.flatMap { (_, v) ->
            v.typeConstructor.supertypes
                .map { getClassDescriptorForType(it) to v }
        }
            .let {
                it + it.map { it.second to null }
            }
            .groupBy({ it.first }) { it.second }
            .map { it.key to it.value.filterNotNull().distinct() }
            .map { (k, v) ->
                InheritanceNode(
                    DRI.from(k),
                    v.map { InheritanceNode(DRI.from(it)) },
                    k.typeConstructor.supertypes.map { getClassDescriptorForType(it) }
                        .mapNotNull { cd -> cd.takeIf { it.kind == ClassKind.INTERFACE }?.let { DRI.from(it) } },
                    isInterface = k.kind == ClassKind.INTERFACE
                )
            }

        return psiInheritanceTree + descriptorInheritanceTree
    }

    private fun gatherPsiClasses(psi: PsiClass): List<Pair<PsiClass, List<PsiClass>>> = psi.supers.toList().let { l ->
        listOf(psi to l) + l.flatMap { gatherPsiClasses(it) }
    }

    private fun getDescriptorMap(documentables: Map<DRI, Documentable>): Map<DRI, ClassDescriptor> {
        val map: MutableMap<DRI, ClassDescriptor> = mutableMapOf()
        documentables
            .mapNotNull { (k, v) ->
                v.descriptorForPlatform()?.let { k to it }?.also { (k, v) -> map[k] = v }
            }.map { it.second }.forEach { gatherSupertypes(it, map) }

        return map.toMap()
    }

    private fun gatherSupertypes(descriptor: ClassDescriptor, map: MutableMap<DRI, ClassDescriptor>) {
        map.putIfAbsent(DRI.from(descriptor), descriptor)
        descriptor.typeConstructor.supertypes.map { getClassDescriptorForType(it) }
            .forEach { gatherSupertypes(it, map) }
    }


    private fun Documentable?.descriptorForPlatform(platform: Platform = Platform.jvm) =
        (this as? WithSources).descriptorForPlatform(platform)

    private fun WithSources?.descriptorForPlatform(platform: Platform = Platform.jvm) = this?.let {
        it.sources.entries.find { it.key.analysisPlatform == platform }?.value?.let { it as? DescriptorDocumentableSource }?.descriptor as? ClassDescriptor
    }
}
