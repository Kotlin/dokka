package org.jetbrains.dokka.renderers

import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.plugability.DokkaContext

interface Renderer {
    fun render(root: PageNode)
}

//class Renderers{
//    abstract class Renderer(resolvers: List[Resolver])
//    class HtmlRenderer(list)
//        fun toHtml() = {
//    new HtmlRenderer(a, b,c)
//}