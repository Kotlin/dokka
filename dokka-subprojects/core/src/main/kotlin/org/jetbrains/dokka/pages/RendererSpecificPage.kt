/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.pages

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.renderers.Renderer
import kotlin.reflect.KClass

public fun interface DriResolver: (DRI, Set<DisplaySourceSet>) -> String?
public fun interface PageResolver: (PageNode, PageNode?) -> String?

public interface RendererSpecificPage : PageNode {
    public val strategy: RenderingStrategy
}

public class RendererSpecificRootPage(
    override val name: String,
    override val children: List<PageNode>,
    override val strategy: RenderingStrategy
) : RootPageNode(), RendererSpecificPage {
    override fun modified(name: String, children: List<PageNode>): RendererSpecificRootPage =
        RendererSpecificRootPage(name, children, strategy)
}

public class RendererSpecificResourcePage(
    override val name: String,
    override val children: List<PageNode>,
    override val strategy: RenderingStrategy
): RendererSpecificPage {
    override fun modified(name: String, children: List<PageNode>): RendererSpecificResourcePage =
        RendererSpecificResourcePage(name, children, strategy)
}

public sealed class RenderingStrategy {
    public class Callback(public val instructions: Renderer.(PageNode) -> String): RenderingStrategy()
    public data class Copy(val from: String) : RenderingStrategy()
    public data class Write(val text: String) : RenderingStrategy()
    public data class DriLocationResolvableWrite(val contentToResolve: (DriResolver) -> String) : RenderingStrategy()
    public data class PageLocationResolvableWrite(val contentToResolve: (PageResolver) -> String) : RenderingStrategy()
    public object DoNothing : RenderingStrategy()

    public companion object {
        public inline operator fun <reified T: Renderer> invoke(crossinline instructions: T.(PageNode) -> String): RenderingStrategy {
            return Callback { if (this is T) instructions(it) else throw WrongRendererTypeException(T::class) }
        }
    }
}

public data class WrongRendererTypeException(val expectedType: KClass<*>): Exception()
