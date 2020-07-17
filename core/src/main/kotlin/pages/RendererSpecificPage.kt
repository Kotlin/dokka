package org.jetbrains.dokka.pages

import org.jetbrains.dokka.renderers.Renderer
import kotlin.reflect.KClass

interface RendererSpecificPage : PageNode {
    val strategy: RenderingStrategy
}

class RendererSpecificRootPage(
    override val name: String,
    override val children: List<PageNode>,
    override val strategy: RenderingStrategy
) : RootPageNode(), RendererSpecificPage {
    override fun modified(name: String, children: List<PageNode>): RendererSpecificRootPage =
        RendererSpecificRootPage(name, children, strategy)
}

class RendererSpecificResourcePage(
    override val name: String,
    override val children: List<PageNode>,
    override val strategy: RenderingStrategy
): RendererSpecificPage {
    override fun modified(name: String, children: List<PageNode>): RendererSpecificResourcePage =
        RendererSpecificResourcePage(name, children, strategy)
}

sealed class RenderingStrategy {
    class Callback(val instructions: Renderer.(PageNode) -> String): RenderingStrategy()
    data class Copy(val from: String) : RenderingStrategy()
    data class Write(val text: String) : RenderingStrategy()
    object DoNothing : RenderingStrategy()

    companion object {
        inline operator fun <reified T: Renderer> invoke(crossinline instructions: T.(PageNode) -> String) =
            Callback { if (this is T) instructions(it) else throw WrongRendererTypeException(T::class) }
    }
}

data class WrongRendererTypeException(val expectedType: KClass<*>): Exception()