package org.jetbrains.dokka.newFrontend.renderer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.newFrontend.transformers.ModulePageNodeView
import org.jetbrains.dokka.newFrontend.transformers.PackagePageNodeView
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.renderers.Renderer

@ExperimentalSerializationApi
class NewFrontendRenderer(private val context: DokkaContext): Renderer {
    private val outputWriter = context.plugin<DokkaBase>().querySingle { outputWriter }

    override fun render(root: RootPageNode) {
        val page = root as ModulePageNodeView
        val bytes = Json.encodeToString(page)
        runBlocking(Dispatchers.Default) {
            launch {
                outputWriter.write("/module-page", bytes, ".json")
                page.children.forEach{
                    render(it)
                }
            }
        }
    }

    suspend fun render(page: PackagePageNodeView) {
        val serialized = Json.encodeToString(page)
        outputWriter.write("/package-page-${page.name}", serialized, ".json")
    }
}