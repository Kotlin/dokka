package org.jetbrains.dokka.allModulesPage.templates

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import java.net.URL
import java.io.File

class ExternalModuleLinkResolver(val context: DokkaContext) {
    private val elpFactory = context.plugin<DokkaBase>().query { externalLocationProviderFactory }
    private val externalDocumentations by lazy(::setupExternalDocumentations)
    private val elps by lazy {
        elpFactory.flatMap { externalDocumentations.map { ed -> it.getExternalLocationProvider(ed) } }.filterNotNull()
    }

    private fun setupExternalDocumentations(): List<ExternalDocumentation> {
        val packageLists =
            context.configuration.modules.map { it.sourceOutputDirectory.resolve(it.relativePathToOutputDirectory) }
                .map { module ->
                    module to PackageList.load(
                        URL("file:" + module.resolve("package-list").path),
                        8,
                        true
                    )
                }.toMap()
        return packageLists.map { (module, pckgList) ->
            ExternalDocumentation(
                URL("file:/${module.name}/${module.name}"),
                pckgList!!
            )
        }
    }

    fun resolve(dri: DRI, fileContext: File): String? {
        val absoluteLink = elps.mapNotNull { it.resolve(dri) }.firstOrNull() ?: return null
        val modulePath = context.configuration.outputDir.absolutePath.split(File.separator)
        val contextPath = fileContext.absolutePath.split(File.separator)
        val commonPathElements = modulePath.zip(contextPath)
            .takeWhile { (a, b) -> a == b }.count()

        return (List(contextPath.size - commonPathElements - 1) { ".." } + modulePath.drop(commonPathElements)).joinToString(
            "/"
        ) + absoluteLink.removePrefix("file:")
    }
}