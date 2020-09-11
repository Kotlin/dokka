package org.jetbrains.dokka.pagesSerialization.renderers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.base.renderers.OutputWriter
import org.jetbrains.dokka.base.resolvers.local.LocationProviderFactory
import org.jetbrains.dokka.base.transformers.pages.serialization.ContentSerializationTransformer
import org.jetbrains.dokka.base.transformers.pages.serialization.ModulePageView
import org.jetbrains.dokka.base.transformers.pages.serialization.PagesSerializationTransformer
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.renderers.Renderer

class PagesSerializationRenderer(
    private val outputWriter: OutputWriter,
    private val locationProviderFactory: LocationProviderFactory
) : Renderer {
    override fun render(root: RootPageNode) {
        val mapper = ObjectMapper().registerModule(KotlinModule())
        val newRoot = PagesSerializationTransformer(ContentSerializationTransformer()).invoke(root)
//        val withLocationResolution = newRoot.transformContentPagesTree { page -> page.modified(page.name, page.content.map {  }) }
        val serialized = when (newRoot) {
            is ModulePageView -> mapper.writeValueAsString(newRoot)
            else -> ""
        }
        runBlocking(Dispatchers.Default) {
            launch {
                outputWriter.write(root.name, serialized, ".json")
            }
        }
    }
}