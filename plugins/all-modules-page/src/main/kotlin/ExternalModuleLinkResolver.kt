package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.base.resolvers.shared.PackageList.Companion.PACKAGE_LIST_NAME
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

    private fun setupExternalDocumentations(): List<ExternalDocumentation> =
        context.configuration.modules.mapNotNull { module ->
            loadPackageListForModule(module)?.let { packageList ->
                ExternalDocumentation(
                    URL("file:/${module.relativePathToOutputDirectory.toRelativeOutputDir()}"),
                    packageList
                )
            }
        }


    private fun File.toRelativeOutputDir(): File = if (isAbsolute) {
        relativeToOrSelf(context.configuration.outputDir)
    } else {
        this
    }

    private fun loadPackageListForModule(module: DokkaModuleDescription) =
        module.sourceOutputDirectory.walkTopDown().maxDepth(3).firstOrNull { it.name == PACKAGE_LIST_NAME }?.let {
            PackageList.load(
                URL("file:" + it.path),
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
                val packageList = loadPackageListForModule(module)
                val extension = packageList?.linkFormat?.linkExtension?.let { ".$it" }.orEmpty()
                "${module.relativePathToOutputDirectory}/index$extension"
            }

}
