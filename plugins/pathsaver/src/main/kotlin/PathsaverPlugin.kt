package  org.jetbrains.dokka.pathsaver

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.RendererSpecificResourcePage
import org.jetbrains.dokka.pages.RenderingStrategy
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.transformers.documentation.DocumentableTransformer
import org.jetbrains.dokka.transformers.pages.PageTransformer

class PathsaverPlugin : DokkaPlugin() {
    val dokkaBasePlugin by lazy { plugin<DokkaBase>() }

    val transformer by extending {
        CoreExtensions.documentableTransformer with DriExtractor order { after(dokkaBasePlugin.emptyPackagesFilter) }
    }

    val preprocessor by extending {
        dokkaBasePlugin.htmlPreprocessors providing { context -> PathSaver(context, dokkaBasePlugin) }
    }
}

object DriExtractor : DocumentableTransformer {
    private val _drisList: MutableList<DRI> = mutableListOf()
    val drisList: List<DRI>
        get() = _drisList

    override fun invoke(original: DModule, context: DokkaContext) =
        original.also { module -> module.withDescendants().forEach { _drisList.add(it.dri) } }
}

class PathSaver(val context: DokkaContext, val dokkaBasePlugin: DokkaBase) :
    PageTransformer {
    val mapper = jacksonObjectMapper()

    override fun invoke(input: RootPageNode): RootPageNode {
        val locationProviderFactories = dokkaBasePlugin.query { locationProviderFactory }
        val locationProvider: LocationProvider =
            locationProviderFactories.map {
                it.getLocationProvider(input)
            }.first()

        val displaySourceSets = context.configuration.sourceSets.map { DisplaySourceSet(it) }.toSet()
        return input.modified(
            children = input.children + createDriPathPage(displaySourceSets, locationProvider)
        )
    }

    private fun createDriPathPage(
        sourceSets: Set<DisplaySourceSet>,
        locationProvider: LocationProvider
    ): RendererSpecificResourcePage {
        val dris = DriExtractor.drisList.flatMap { dri -> sourceSets.map { sourceSet -> dri to sourceSet } }
            .map { (dri, sourceSet) ->
                (dri to sourceSet) to locationProvider.resolve(dri, sourceSet)
            }.toMap()

        return RendererSpecificResourcePage(
            "pathsIndex.json",
            emptyList(),
            RenderingStrategy.Write(mapper.writeValueAsString(dris))
        )
    }

}
