package org.jetbrains.dokka

trait LanguageService {
    fun render(node: DocumentationNode): ContentNode
    fun renderName(node: DocumentationNode) : String
}

