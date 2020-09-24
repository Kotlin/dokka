package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query

abstract class DefaultLocationProvider(
    protected val pageGraphRoot: RootPageNode,
    protected val dokkaContext: DokkaContext,
    protected val extension: String
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
        .filterNotNull()
        .map { extDocInfo ->
            val externalLocationProvider = (externalLocationProviderFactories.asSequence()
                .mapNotNull { it.getExternalLocationProvider(extDocInfo) }.firstOrNull()
                ?: run { dokkaContext.logger.error("No ExternalLocationProvider for '${extDocInfo.packageList.url}' found"); null })
            extDocInfo to externalLocationProvider
        }
        .toMap()

    protected val packagesIndex: Map<String, ExternalLocationProvider?> = externalLocationProviders
        .flatMap { (extDocInfo, externalLocationProvider) ->
            extDocInfo.packageList.packages.map { packageName -> packageName to externalLocationProvider }
        }
        .toMap()
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

}
