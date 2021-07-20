package org.jetbrains.dokka.sitemap.transformers.pages

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.sitemap.SitemapConfiguration
import org.jetbrains.dokka.sitemap.SitemapPlugin
import org.jetbrains.dokka.templates.TemplateProcessingStrategy
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class SitemapTemplateProcessingStrategy(val context: DokkaContext): TemplateProcessingStrategy {
    private val fragments = ConcurrentHashMap<String, List<String>>()
    private val maybeConfiguration = configuration<SitemapPlugin, SitemapConfiguration>(context)

    override fun process(
        input: File,
        output: File,
        moduleContext: DokkaConfiguration.DokkaModuleDescription?
    ): Boolean {
        if(canProcess(input)){
            val (moduleName, content) = input.readLines().headAndTail()
            moduleName ?: run {
                context.logger.debug("SitemapTemplateProcessingStrategy failed to process file $input even though it should be able to do so. Module name in first line was not found.")
                return false
            }
            fragments[moduleName] = content
            return true
        }
        return false
    }

    override fun finish(output: File) {
        if (fragments.isNotEmpty()) {
            val content = fragments.flatMap { (key, values) -> values.map { it.replace(SitemapPageTransformer.templateSymbol, key) }}
                .sorted()
                .joinToString(separator = "\n")
            output.resolve(maybeConfiguration?.relativeOutputLocation ?: SitemapConfiguration.defaultRelativeOutputLocation).appendText(content)
        }
    }

    /**
     * There is an implicit assumption that sitemaps for multimodule will be in default location
     *
     * That is the case when using [SitemapPageTransformer]
     */
    private fun canProcess(input: File): Boolean =
        input.name == SitemapConfiguration.defaultRelativeOutputLocation

    private fun List<String>.headAndTail(): Pair<String?, List<String>> =
        when(size){
            0 -> null to emptyList()
            1 -> first() to emptyList()
            else -> first() to drop(1)
        }
}