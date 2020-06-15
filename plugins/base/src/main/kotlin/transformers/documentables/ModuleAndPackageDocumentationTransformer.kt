package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.sourceSet
import org.jetbrains.dokka.parsers.MarkdownParser
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.PreMergeDocumentableTransformer
import org.jetbrains.kotlin.name.FqName
import java.nio.file.Files
import java.nio.file.Paths


internal class ModuleAndPackageDocumentationTransformer(val context: DokkaContext) : PreMergeDocumentableTransformer {

    override fun invoke(original: List<DModule>): List<DModule> {

        val modulesAndPackagesDocumentation =
            context.configuration.passesConfigurations
                .map {
                    Pair(it.moduleName, context.sourceSet(it)) to
                            it.includes.map { Paths.get(it) }
                                .also {
                                    it.forEach {
                                        if (Files.notExists(it))
                                            context.logger.warn("Not found file under this path ${it.toAbsolutePath()}")
                                    }
                                }
                                .filter { Files.exists(it) }
                                .flatMap {
                                    it.toFile()
                                        .readText()
                                        .split(Regex("(\n|^)# (?=(Module|Package))")) // Matches heading with Module/Package to split by
                                        .filter { it.isNotEmpty() }
                                        .map {
                                            it.split(
                                                Regex(" "),
                                                2
                                            )
                                        } // Matches space between Module/Package and fully qualified name
                                }.groupBy({ it[0] }, {
                                    it[1].split(Regex("\n"), 2) // Matches new line after fully qualified name
                                        .let { it[0].trim() to it[1].trim() }
                                }).mapValues {
                                    it.value.toMap()
                                }
                }.toMap()

        return original.map { module ->

            val moduleDocumentation =
                module.sourceSets.mapNotNull { pd ->
                    val doc = modulesAndPackagesDocumentation[Pair(module.name, pd)]
                    val facade = context.platforms[pd]?.facade
                        ?: return@mapNotNull null.also { context.logger.warn("Could not find platform data for ${pd.moduleName}/${pd.sourceSetName}") }
                    try {
                        doc?.get("Module")?.get(module.name)?.run {
                            pd to MarkdownParser(
                                facade,
                                facade.moduleDescriptor,
                                context.logger
                            ).parse(this)
                        }
                    } catch (e: IllegalArgumentException) {
                        context.logger.error(e.message.orEmpty())
                        null
                    }
                }.toMap()

            val packagesDocumentation = module.packages.map {
                it.name to it.sourceSets.mapNotNull { pd ->
                    val doc = modulesAndPackagesDocumentation[Pair(module.name, pd)]
                    val facade = context.platforms[pd]?.facade
                        ?: return@mapNotNull null.also { context.logger.warn("Could not find platform data for ${pd.moduleName}/${pd.sourceSetName}") }
                    val descriptor = facade.moduleDescriptor.getPackage(FqName(it.name.let { if(it == "[JS root]") "" else it }))
                    doc?.get("Package")?.get(it.name)?.run {
                        pd to MarkdownParser(
                            facade,
                            descriptor,
                            context.logger
                        ).parse(this)
                    }
                }.toMap()
            }.toMap()

            module.copy(
                documentation = mergeDocumentation(module.documentation, moduleDocumentation),
                packages = module.packages.map {
                    val packageDocumentation = packagesDocumentation[it.name]
                    if (packageDocumentation != null && packageDocumentation.isNotEmpty())
                        it.copy(documentation = mergeDocumentation(it.documentation, packageDocumentation))
                    else
                        it
                }
            )
        }
    }

    private fun mergeDocumentation(origin: Map<SourceSetData, DocumentationNode>, new: Map<SourceSetData, DocumentationNode>) =
        (origin.asSequence() + new.asSequence())
            .distinct()
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, values) -> DocumentationNode(values.flatMap { it.children }) }

}
