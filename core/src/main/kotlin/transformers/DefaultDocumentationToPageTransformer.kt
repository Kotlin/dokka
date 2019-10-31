package org.jetbrains.dokka.transformers

import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentSymbol
import org.jetbrains.dokka.pages.ModulePageNode
import org.jetbrains.dokka.pages.PageNode

//class DefaultDocumentationToPageTransformer: DocumentationToPageTransformer {
//    override fun transform(d: DocumentationNode): PageNode {
//        assert(d is DocumentationNodes.Module) // TODO: remove this if it's true. Fix this it it's not
//        val rootPage = ModulePageNode(d.name, contentFor(d), null)
//
//        // TODO
//
//        return rootPage
//    }
//
//    private fun contentFor(d: DocumentationNode): List<ContentNode> {
//        val symbol = ContentSymbol()
//    }
//    private fun moduleContent(d: DocumentationNodes.Module)
//
//    private fun symbolFor(d: DocumentationNode): List<ContentNode> {
//
//    }
//
//}