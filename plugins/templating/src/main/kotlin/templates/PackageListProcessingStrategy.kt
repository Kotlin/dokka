package org.jetbrains.dokka.allModulesPage.templates

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.renderers.PackageListService
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.templates.TemplateProcessingStrategy
import java.io.File

class PackageListProcessingStrategy(val context: DokkaContext) : TemplateProcessingStrategy {
    private val fragments = mutableSetOf<PackageList>()

    private fun canProcess(file: File): Boolean =
            file.extension.isBlank() && file.nameWithoutExtension == PACKAGE_LIST_NAME

    override fun process(input: File, output: File, moduleContext: DokkaConfiguration.DokkaModuleDescription?): Boolean {
        if (canProcess(input)) {
            val packageList = PackageList.load(input.toURI().toURL(), 8, true)
            packageList?.copy(modules = mapOf(moduleContext?.name.orEmpty() to packageList.modules.getOrDefault("", emptySet())))
                    ?.let { fragments.add(it) } ?: fallbackToCopy(input, output)
        }
        return canProcess(input)
    }

    override fun finish(output: File) {
        if (fragments.isNotEmpty()) {
            val linkFormat = fragments.first().linkFormat

            if (!fragments.all { it.linkFormat == linkFormat }) {
                context.logger.error("Link format is inconsistent between modules")
            }

            val locations: Map<String, String> = fragments.map { it.locations }.fold(emptyMap()) { acc, el -> acc + el }
            val modules: Map<String, Set<String>> = fragments.map { it.modules }.fold(emptyMap()) { acc, el -> acc + el }
            val mergedPackageList = PackageListService.renderPackageList(locations, modules, linkFormat.formatName, linkFormat.linkExtension)
            output.mkdirs()
            output.resolve(PACKAGE_LIST_NAME).writeText(mergedPackageList)
        }
    }

    private fun fallbackToCopy(input: File, output: File) {
        context.logger.warn("Falling back to just copying ${input.name} file even though it should have been processed")
        input.copyTo(output)
    }

    companion object {
        const val PACKAGE_LIST_NAME = "package-list"
    }
}
