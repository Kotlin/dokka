package org.jetbrains.dokka

public trait ResolutionService {
    fun resolve(text: String): DocumentationNode
}