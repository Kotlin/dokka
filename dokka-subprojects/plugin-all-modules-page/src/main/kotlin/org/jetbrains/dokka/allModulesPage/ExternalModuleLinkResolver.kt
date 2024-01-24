/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.allModulesPage

import org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.resolvers.external.ExternalLocationProvider
import org.jetbrains.dokka.base.resolvers.shared.ExternalDocumentation
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.base.resolvers.shared.PackageList.Companion.PACKAGE_LIST_NAME
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.query
import org.jetbrains.dokka.utilities.associateWithNotNull
import java.io.File
import java.net.URL

public interface ExternalModuleLinkResolver {
    public fun resolve(dri: DRI, fileContext: File): String?
    public fun resolveLinkToModuleIndex(moduleName: String): String?
}

public class DefaultExternalModuleLinkResolver(
    public val context: DokkaContext
) : ExternalModuleLinkResolver {

    private class ExternalLocationProviderWithModuleDescription(
        val locationProvider: ExternalLocationProvider,
        val moduleDescription: DokkaModuleDescription
    )

    private val elpFactory = context.plugin<DokkaBase>().query { externalLocationProviderFactory }
    private val elps by lazy {
        val externalDocumentations = setupExternalDocumentations()
        elpFactory.flatMap {
            externalDocumentations.mapNotNull { (mdl, ed) ->
                it.getExternalLocationProvider(ed)?.let { ExternalLocationProviderWithModuleDescription(it, mdl) }
            }
        }
    }

    private fun setupExternalDocumentations(): Map<DokkaModuleDescription, ExternalDocumentation> =
        context.configuration.modules.associateWithNotNull { module ->
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
        val resolvedLinks = elps.mapNotNull { locProviderWithMdl ->
            locProviderWithMdl.locationProvider.resolve(dri)?.let { it to locProviderWithMdl.moduleDescription }
        }
        val validLink = resolvedLinks.firstOrNull {
            // this is a temporary hack for the first case (short-term solution) of #3368:
            // a situation where 2 (or more) local modules have the same package name.
            // TODO #3368 currently, it does not work for external modules with the same package name
            if (resolvedLinks.size > 1) {
                val moduleDescription = it.second
                val resolvedLinkWithoutRelativePath = it.first.removePrefix(
                   "file:/" + moduleDescription.relativePathToOutputDirectory.toRelativeOutputDir()
                        .toString()
                )

                val partialModuleOutput = moduleDescription.sourceOutputDirectory
                val absolutePath = File(partialModuleOutput.absolutePath + resolvedLinkWithoutRelativePath)
                absolutePath.isFile
            } else true
        }?.first ?: return null

        // relativization [fileContext] path over output path (or `fileContext.relativeTo(outputPath)`)
        //  e.g. outputPath = `/a/b/`
        //  fileContext =  `/a/c/d/index.html`
        //  will be transformed to `../../b`+ validLink
        val outputPath = context.configuration.outputDir.absolutePath


        val outputPathParts = outputPath.split(File.separator)
        val contextPathParts = fileContext.absolutePath.split(File.separator)
        val commonPathElements = outputPathParts.zip(contextPathParts)
            .takeWhile { (a, b) -> a == b }.count()

        return (List(contextPathParts.size - commonPathElements - 1) { ".." } + outputPathParts.drop(commonPathElements))
            .joinToString("/") + validLink.removePrefix("file:")
    }

    override fun resolveLinkToModuleIndex(moduleName: String): String? =
        context.configuration.modules.firstOrNull { it.name == moduleName }
            ?.let { module ->
                val packageList = loadPackageListForModule(module)
                val extension = packageList?.linkFormat?.linkExtension?.let { ".$it" }.orEmpty()
                "${module.relativePathToOutputDirectory}/index$extension"
            }

}
