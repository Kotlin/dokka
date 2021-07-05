package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.external.Dokka010ExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.external.javadoc.AndroidExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.external.javadoc.JavadocExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query

abstract class DefaultLocationProvider(
    protected val pageGraphRoot: RootPageNode,
    protected val dokkaContext: DokkaContext
) : LocationProvider {
    protected val externalLocationProviderFactories =
        dokkaContext.plugin<DokkaBase>().query { externalLocationProviderFactory }

    protected val externalLocationProviders: Map<ExternalDocumentation, ExternalLocationProvider?> = dokkaContext
            .configuration
            .sourceSets
            .flatMap { sourceSet ->
                sourceSet.externalDocumentationLinks.map {
                    PackageList.load(it.packageListUrl, sourceSet.jdkVersion, dokkaContext.configuration.offlineMode)
                            ?.let { packageList -> ExternalDocumentation(it.url, packageList) }
                }
            }
            .filterNotNull().associateWith { extDocInfo ->
                externalLocationProviderFactories
                    .mapNotNull { it.getExternalLocationProvider(extDocInfo) }
                    .firstOrNull()
                    ?: run { dokkaContext.logger.error("No ExternalLocationProvider for '${extDocInfo.packageList.url}' found"); null }
            }

    protected val packagesIndex: Map<String, ExternalLocationProvider?> =
        externalLocationProviders
            .flatMap { (extDocInfo, externalLocationProvider) ->
                extDocInfo.packageList.packages.map { packageName -> packageName to externalLocationProvider }
            }.groupBy { it.first }.mapValues { (_, lst) ->
                lst.map { it.second }
                    .sortedWith(compareBy(nullsLast(ExternalLocationProviderOrdering)) { it })
                    .firstOrNull()
            }
            .filterKeys(String::isNotBlank)


    protected val locationsIndex: Map<String, ExternalLocationProvider?> = externalLocationProviders
        .flatMap { (extDocInfo, externalLocationProvider) ->
            extDocInfo.packageList.locations.keys.map { relocatedDri -> relocatedDri to externalLocationProvider }
        }
        .toMap()
        .filterKeys(String::isNotBlank)

    protected open fun getExternalLocation(dri: DRI, sourceSets: Set<DisplaySourceSet>): String? =
        packagesIndex[dri.packageName]?.resolve(dri)
            ?: locationsIndex[dri.toString()]?.resolve(dri)
            ?: externalLocationProviders.values.mapNotNull { it?.resolve(dri) }.firstOrNull()

    private object ExternalLocationProviderOrdering : Comparator<ExternalLocationProvider> {
        private val desiredOrdering = listOf(
            DefaultExternalLocationProvider::class,
            Dokka010ExternalLocationProvider::class,
            AndroidExternalLocationProvider::class,
            JavadocExternalLocationProvider::class
        )

        override fun compare(o1: ExternalLocationProvider, o2: ExternalLocationProvider): Int =
            desiredOrdering.indexOf(o1::class).compareTo(desiredOrdering.indexOf(o2::class))
    }

}
