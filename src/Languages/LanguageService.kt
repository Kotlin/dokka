package org.jetbrains.dokka

trait LanguageService {
    fun render(node: DocumentationNode): String
    fun renderName(node: DocumentationNode): String
    fun renderFunction(node: DocumentationNode): String
    fun renderClass(node: DocumentationNode): String
    fun renderTypeParametersForNode(node: DocumentationNode): String
    fun renderTypeParameter(node: DocumentationNode): String
    fun renderParameter(node: DocumentationNode): String
    fun renderType(node: DocumentationNode): String
    fun renderPackage(node: DocumentationNode): String
    fun renderProperty(node: DocumentationNode): String
    fun renderModifier(node: DocumentationNode): String
    fun renderModifiersForNode(node: DocumentationNode): String
}

