package org.jetbrains.dokka.pagesSerialization.renderers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.resolvers.local.resolveOrThrow
import org.jetbrains.dokka.base.transformers.pages.serialization.*
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.renderers.Renderer

class PagesSerializationRenderer(
    private val outputWriter: OutputWriter,
    private val locationProviderFactory: LocationProviderFactory
) : Renderer {
    override fun render(root: RootPageNode) {
        val mapper = ObjectMapper().registerModule(KotlinModule())
        val newRoot = PagesSerializationTransformer(ContentSerializationTransformer()).invoke(root)
        val locationProvider = locationProviderFactory.getLocationProvider(newRoot)
        fun resolveContent(node: PagesSerializationContentView): PagesSerializationContentView =
            when(node){
                is UnresolvedLinkView -> ResolvedLinkView(
                    address = locationProvider.resolve(node.dci.dri.first(), node.sourceSets) ?: "",
                    dci = node.dci,
                    style = node.style,
                    children = node.children.map { resolveContent(it as PagesSerializationContentView) },
                    sourceSets = node.sourceSets,
                    extra = node.extra
                )
                is DivergentInstanceView -> node.copy(
                    before = node.before?.let { resolveContent(it as PagesSerializationContentView) },
                    divergent = resolveContent(node.divergent as PagesSerializationContentView),
                    after = node.after?.let { resolveContent(it as PagesSerializationContentView) }
                )
                else -> node.withNewChildren(node.children.map { resolveContent(it as PagesSerializationContentView) })
            }

        val withLocationResolution = newRoot.transformPageNodeTree { page -> (page as PagesSerializationView).withNewContent(resolveContent(page.content)) }
        val serialized = mapper.writeValueAsString(withLocationResolution)
        runBlocking(Dispatchers.Default) {
            launch {
                outputWriter.write(root.name, serialized, ".json")
            }
        }
    }
}