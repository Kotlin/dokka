package org.jetbrains.dokka.base.transformers.documentables.utils

import com.intellij.psi.PsiClass
import kotlinx.coroutines.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.DescriptorDocumentableSource
import org.jetbrains.dokka.analysis.PsiDocumentableSource
import org.jetbrains.dokka.analysis.from
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.utilities.parallelForEach
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import java.util.concurrent.ConcurrentHashMap


/**
 * The class allows lo build a full class hierarchy via descriptors
 */
class ClassGraphBuilder {
    suspend operator fun invoke(original: DModule): SourceSetDependent<Map<DRI, List<DRI>>> = coroutineScope {
        val map = original.sourceSets.associateWith { ConcurrentHashMap<DRI, List<DRI>>() }
        original.packages.parallelForEach { visitDocumentable(it, map) }
        map
    }

    private suspend fun collectSupertypesFromKotlinType(
        DRIWithKType: Pair<DRI, KotlinType>,
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        supersMap: SourceSetDependent<MutableMap<DRI, List<DRI>>>
    ): Unit = coroutineScope {
        val supertypes = DRIWithKType.second.immediateSupertypes().filterNot { it.isAnyOrNullableAny() }
        val supertypesDRIWithKType = supertypes.mapNotNull { supertype ->
            supertype.constructor.declarationDescriptor?.let {
                DRI.from(it) to supertype
            }
        }

        supersMap[sourceSet]?.let { map ->
            if (map[DRIWithKType.first] == null) {
                // another thread can rewrite the same value, but it isn't a problem
                map[DRIWithKType.first] = supertypesDRIWithKType.map { it.first }
                supertypesDRIWithKType.parallelForEach { collectSupertypesFromKotlinType(it, sourceSet, supersMap) }
            }
        }
    }

    private suspend fun collectSupertypesFromPsiClass(
        DRIWithPsiClass: Pair<DRI, PsiClass>,
        sourceSet: DokkaConfiguration.DokkaSourceSet,
        supersMap: SourceSetDependent<MutableMap<DRI, List<DRI>>>
    ): Unit = coroutineScope {
        val supertypes = DRIWithPsiClass.second.superTypes.mapNotNull { it.resolve() }
            .filterNot { it.qualifiedName == "java.lang.Object" }
        val supertypesDRIWithPsiClass = supertypes.map { DRI.from(it) to it }

        supersMap[sourceSet]?.let { map ->
            if (map[DRIWithPsiClass.first] == null) {
                // another thread can rewrite the same value, but it isn't a problem
                map[DRIWithPsiClass.first] = supertypesDRIWithPsiClass.map { it.first }
                supertypesDRIWithPsiClass.parallelForEach { collectSupertypesFromPsiClass(it, sourceSet, supersMap) }
            }
        }
    }

    private suspend fun visitDocumentable(
        documentable: Documentable,
        supersMap: SourceSetDependent<MutableMap<DRI, List<DRI>>>
    ): Unit = coroutineScope {
        if (documentable is WithScope) {
            documentable.classlikes.parallelForEach { visitDocumentable(it, supersMap) }
        }
        if (documentable is DClasslike) {
            // to build a full class graph, using supertypes from Documentable
            // is not enough since it keeps only one level of hierarchy
            documentable.sources.forEach { (sourceSet, source) ->
                if (source is DescriptorDocumentableSource) {
                    val descriptor = source.descriptor as ClassDescriptor
                    val type = descriptor.defaultType
                    collectSupertypesFromKotlinType(documentable.dri to type, sourceSet, supersMap)
                } else if (source is PsiDocumentableSource) {
                    val psi = source.psi as PsiClass
                    collectSupertypesFromPsiClass(documentable.dri to psi, sourceSet, supersMap)
                }
            }
        }
    }
}