package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer

class InheritorsExtractorTransformer : DocumentableTransformer {
    override fun invoke(original: DModule, context: DokkaContext): DModule =
        original.generateInheritanceMap().let { inheritanceMap -> original.appendInheritors(inheritanceMap) as DModule }

    private fun <T : Documentable> T.appendInheritors(inheritanceMap: Map<PlatformData, Map<DRI, List<DRI>>>): Documentable =
        InheritorsInfo(PlatformDependent(inheritanceMap.getForDRI(dri))).let { info ->
            when (this) {
                is DModule -> copy(packages = packages.map { it.appendInheritors(inheritanceMap) as DPackage })
                is DPackage -> copy(classlikes = classlikes.map { it.appendInheritors(inheritanceMap) as DClasslike })
                is DClass -> copy(
                    extra = extra + info,
                    classlikes = classlikes.map { it.appendInheritors(inheritanceMap) as DClasslike })
                is DEnum -> copy(
                    extra = extra + info,
                    classlikes = classlikes.map { it.appendInheritors(inheritanceMap) as DClasslike })
                is DInterface -> copy(
                    extra = extra + info,
                    classlikes = classlikes.map { it.appendInheritors(inheritanceMap) as DClasslike })
                is DObject -> copy(classlikes = classlikes.map { it.appendInheritors(inheritanceMap) as DClasslike })
                is DAnnotation -> copy(classlikes = classlikes.map { it.appendInheritors(inheritanceMap) as DClasslike })
                else -> this
            }
        }

    private fun Map<PlatformData, Map<DRI, List<DRI>>>.getForDRI(dri: DRI) =
        PlatformDependent(map { (v, k) ->
            v to k[dri]
        }.map { (k, v) -> k to v.orEmpty() }.toMap())

    private fun DModule.generateInheritanceMap() =
        getInheritanceEntriesRec().filterNot { it.second.isEmpty() }.groupBy({ it.first }) { it.second }
            .map { (k, v) ->
                k to v.flatMap { p -> p.groupBy({ it.first }) { it.second }.toList() }
                    .groupBy({ it.first }) { it.second }.map { (k2, v2) -> k2 to v2.flatten() }.toMap()
            }.toMap()

    private fun <T : Documentable> T.getInheritanceEntriesRec(): List<Pair<PlatformData, List<Pair<DRI, DRI>>>> =
        this.toInheritanceEntries() + children.flatMap { it.getInheritanceEntriesRec() }

    private fun <T : Documentable> T.toInheritanceEntries() =
        (this as? WithSupertypes)?.let {
            it.supertypes.map.map { (k, v) -> k to v.map { it to dri } }
        }.orEmpty()

}

class InheritorsInfo(val value: PlatformDependent<List<DRI>>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, InheritorsInfo> {
        override fun mergeStrategyFor(left: InheritorsInfo, right: InheritorsInfo): MergeStrategy<Documentable> =
            MergeStrategy.Replace(
                InheritorsInfo(
                    PlatformDependent(
                        (left.value.map.entries.toList() + right.value.map.entries.toList())
                            .groupBy({ it.key }) { it.value }
                            .map { (k, v) -> k to v.flatten() }.toMap()
                    )
                )
            )
    }

    override val key: ExtraProperty.Key<Documentable, *> = InheritorsInfo
}

