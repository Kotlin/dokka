package org.jetbrains.dokka.allModulesPage.templates

import org.jetbrains.dokka.DokkaConfiguration.DokkaModuleDescription
import org.jetbrains.dokka.base.renderers.PackageListService
import org.jetbrains.dokka.base.resolvers.shared.PackageList
import org.jetbrains.dokka.base.resolvers.shared.PackageList.Companion.PACKAGE_LIST_NAME
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.templates.TemplateProcessingStrategy
import java.io.File

class PackageListProcessingStrategy(val context: DokkaContext) : TemplateProcessingStrategy {
    private val fragments = mutableSetOf<PackageList>()

    private fun canProcess(file: File, moduleContext: DokkaModuleDescription?): Boolean =
            file.extension.isBlank() && file.nameWithoutExtension == PACKAGE_LIST_NAME && moduleContext != null

    override fun process(input: File, output: File, moduleContext: DokkaModuleDescription?): Boolean {
        val canProcess = canProcess(input, moduleContext)
        if (canProcess) {
            val packageList = PackageList.load(input.toURI().toURL(), 8, true)
            val moduleFilename = moduleContext?.name?.let { "$it/" }
            packageList?.copy(
                    modules = mapOf(moduleContext?.name.orEmpty() to packageList.modules.getOrDefault(PackageList.SINGLE_MODULE_NAME, emptySet())),
                    locations = packageList.locations.entries.associate { it.key to "$moduleFilename${it.value}" }
            )?.let { fragments.add(it) } ?: fallbackToCopy(input, output)
        }
        return canProcess
    }

    override fun finish(output: File) {
        if (fragments.isNotEmpty()) {
            val linkFormat = fragments.first().linkFormat

            if (!fragments.all { it.linkFormat == linkFormat }) {
                context.logger.error("Link format is inconsistent between modules: " + fragments.joinToString { it.linkFormat.formatName } )
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
}
