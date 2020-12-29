package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider.Companion.identifierToFilename
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.base.resolvers.shared.RecognizedLinkFormat
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import java.io.File
import java.net.URL

interface ExternalModuleLinkResolver {
    fun resolve(dri: DRI, fileContext: File): String?
    fun resolveLinkToModuleIndex(moduleName: String): String?
}

class DefaultExternalModuleLinkResolver(val context: DokkaContext) : ExternalModuleLinkResolver {
    private val elpFactory = context.plugin<DokkaBase>().query { externalLocationProviderFactory }
    private val externalDocumentations by lazy(::setupExternalDocumentations)
    private val elps by lazy {
        elpFactory.flatMap { externalDocumentations.map { ed -> it.getExternalLocationProvider(ed) } }.filterNotNull()
    }

    private fun setupExternalDocumentations(): List<ExternalDocumentation> {
        val packageLists =
            context.configuration.modules.map(::loadPackageListForModule).toMap()
        return packageLists.mapNotNull { (module, packageList) ->
            packageList?.let {
                context.configuration.modules.find { it.name == module.name }?.let { m ->
                    ExternalDocumentation(
                        URL("file:/${m.relativePathToOutputDirectory.toRelativeOutputDir()}"),
                        packageList
                    )
                }
            }
        }
    }

    private fun File.toRelativeOutputDir(): File = if(isAbsolute) {
        relativeToOrSelf(context.configuration.outputDir)
    } else {
        this
    }

    private fun loadPackageListForModule(module: DokkaConfiguration.DokkaModuleDescription) =
        module.sourceOutputDirectory.resolve(File(identifierToFilename(module.name))).let {
            it to PackageList.load(
                URL("file:" + it.resolve("package-list").path),
                8,
                true
            )
        }

    override fun resolve(dri: DRI, fileContext: File): String? {
        val absoluteLink = elps.mapNotNull { it.resolve(dri) }.firstOrNull() ?: return null
        val modulePath = context.configuration.outputDir.absolutePath.split(File.separator)
        val contextPath = fileContext.absolutePath.split(File.separator)
        val commonPathElements = modulePath.zip(contextPath)
            .takeWhile { (a, b) -> a == b }.count()

        return (List(contextPath.size - commonPathElements - 1) { ".." } + modulePath.drop(commonPathElements)).joinToString(
            "/"
        ) + absoluteLink.removePrefix("file:")
    }

    override fun resolveLinkToModuleIndex(moduleName: String): String? =
        context.configuration.modules.firstOrNull { it.name == moduleName }
            ?.let { module ->
                val (_, packageList) = loadPackageListForModule(module)
                val extension = when (packageList?.linkFormat) {
                    RecognizedLinkFormat.KotlinWebsiteHtml,
                    RecognizedLinkFormat.DokkaOldHtml,
                    RecognizedLinkFormat.DokkaHtml -> ".html"
                    RecognizedLinkFormat.DokkaGFM,
                    RecognizedLinkFormat.DokkaJekyll -> ".md"
                    else -> ""
                }
                "${module.relativePathToOutputDirectory}/index$extension"
            }

}
