package org.jetbrains.dokka

import java.net.URL
import java.nio.file.Paths

interface DocumentationRoot {
    val resolver: InboundExternalLinkResolutionService
    val locations: Map<String, String>
    fun resolve(path: String): String
    val isLocal: Boolean
}

class ExternalDocumentationRoot(
    private val rootUrl: URL,
    override val resolver: InboundExternalLinkResolutionService,
    override val locations: Map<String, String>
): DocumentationRoot {
    override val isLocal = false

    override fun resolve(path: String): String =
        URL(rootUrl, path).toExternalForm()

    override fun toString(): String = rootUrl.toString()
}

class LocalDocumentationRoot(
    private val rootPath: String,
    override val resolver: InboundExternalLinkResolutionService,
    override val locations: Map<String, String>
): DocumentationRoot {
    override val isLocal = true

    override fun resolve(path: String): String =
            Paths.get(rootPath).resolve(path).toString()

    override fun toString(): String = rootPath
}