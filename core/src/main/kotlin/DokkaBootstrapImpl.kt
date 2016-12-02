package org.jetbrains.dokka

import java.io.File

fun parseSourceLinkDefinition(srcLink: String): SourceLinkDefinition {
    val (path, urlAndLine) = srcLink.split('=')
    return SourceLinkDefinition(File(path).absolutePath,
            urlAndLine.substringBefore("#"),
            urlAndLine.substringAfter("#", "").let { if (it.isEmpty()) null else "#" + it })
}


class DokkaBootstrapImpl : DokkaBootstrap {

    lateinit var generator: DokkaGenerator

    override fun configure(logger: DokkaLogger,
                           moduleName: String,
                           classpath: List<String>,
                           sources: List<String>,
                           samples: List<String>,
                           includes: List<String>,
                           outputDir: String,
                           format: String,
                           includeNonPublic: Boolean,
                           reportUndocumented: Boolean,
                           skipEmptyPackages: Boolean,
                           skipDeprecated: Boolean,
                           jdkVersion: Int,
                           generateIndexPages: Boolean,
                           sourceLinks: List<String>) {
        generator = DokkaGenerator(
                logger,
                classpath,
                sources,
                samples,
                includes,
                moduleName,
                DocumentationOptions(
                        outputDir,
                        format,
                        includeNonPublic,
                        reportUndocumented,
                        skipEmptyPackages,
                        skipDeprecated,
                        jdkVersion,
                        generateIndexPages,
                        sourceLinks.map(::parseSourceLinkDefinition)
                )
        )

    }

    override fun generate() = generator.generate()
}