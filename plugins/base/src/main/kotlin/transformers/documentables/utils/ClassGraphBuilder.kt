package org.jetbrains.dokka.base.transformers.documentables.utils

import kotlinx.coroutines.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.analysis.DescriptorDocumentableSource
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
    suspend operator fun invoke(original: DModule,): SourceSetDependent<Map<DRI, List<DRI>>> = coroutineScope{
        val map = original.sourceSets.associateWith { ConcurrentHashMap<DRI, List<DRI>>() }
        original.packages.parallelForEach{ visitDocumentable(it, map) }
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

        supersMap[sourceSet]?.let {
            if (it[DRIWithKType.first] == null) {
                // another thread can rewrite the same value, but it isn't a problem
                it[DRIWithKType.first] = supertypesDRIWithKType.map { it.first }
                supertypesDRIWithKType.parallelForEach { collectSupertypesFromKotlinType(it, sourceSet, supersMap) }
            }
        }
    }

    private suspend fun visitDocumentable(
        documentable: Documentable,
        supersMap: SourceSetDependent<MutableMap<DRI, List<DRI>>>
    ): Unit = coroutineScope {
        if (documentable is WithScope) {
            documentable.classlikes.
            parallelForEach{ visitDocumentable(it, supersMap) }
        }
        if(documentable is DClasslike) {
            documentable.sources.forEach { (sourceSet, source) ->
                if (source is DescriptorDocumentableSource) {
                    val descriptor = source.descriptor as ClassDescriptor
                    val type = descriptor.defaultType
                    collectSupertypesFromKotlinType( documentable.dri to type, sourceSet, supersMap)
                }
            }
        }
    }
}