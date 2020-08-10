package org.jetbrains.dokka.javadoc.renderer

import org.jetbrains.dokka.javadoc.pages.*
import org.jetbrains.dokka.javadoc.renderer.SearchRecord.Companion.allTypes
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.resolvers.local.resolveOrThrow
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.utilities.formatToEndWithHtml
import java.lang.StringBuilder

class SearchScriptsCreator(private val locationProvider: LocationProvider) {

    fun invoke(input: RootPageNode): List<RendererSpecificPage> {
        val data = when (input) {
            is JavadocModulePageNode -> processModules(listOf(input))
            else -> processModules(input.children.filterIsInstance<JavadocModulePageNode>())
        }
        val serializer = SearchRecordJsonSerializer()

        val modules = RendererSpecificResourcePage(
            name = "module-search-index.js",
            children = emptyList(),
            strategy = RenderingStrategy.Write(serializer.serialize(data.moduleRecords, "moduleSearchIndex"))
        )

        val packages = RendererSpecificResourcePage(
            name = "package-search-index.js",
            children = emptyList(),
            strategy = RenderingStrategy.Write(serializer.serialize(data.packageRecords, "packageSearchIndex"))
        )

        val types = RendererSpecificResourcePage(
            name = "type-search-index.js",
            children = emptyList(),
            strategy = RenderingStrategy.Write(serializer.serialize(data.typeRecords, "typeSearchIndex"))
        )

        val members = RendererSpecificResourcePage(
            name = "member-search-index.js",
            children = emptyList(),
            strategy = RenderingStrategy.Write(serializer.serialize(data.memberRecords, "memberSearchIndex"))
        )

        val indexes = RendererSpecificResourcePage(
            name = "tag-search-index.js",
            children = emptyList(),
            strategy = RenderingStrategy.Write(serializer.serialize(data.searchIndexes, "tagSearchIndex"))
        )

        return listOf(modules, packages, types, members, indexes)
    }

    private fun processModules(input: List<JavadocModulePageNode>): SearchData {
        val modules = SearchData(moduleRecords = input.map {
            SearchRecord(
                l = it.name,
                url = locationProvider.resolveOrThrow(it).formatToEndWithHtml()
            )
        })
        val processablePackages = input.flatMap { it.children.filterIsInstance<JavadocPackagePageNode>() }
        return processPackages(processablePackages, modules)
    }

    private fun processPackages(input: List<JavadocPackagePageNode>, accumulator: SearchData): SearchData {
        val packages = input.map {
            SearchRecord(
                l = it.name,
                url = locationProvider.resolveOrThrow(it).formatToEndWithHtml()
            )
        } + SearchRecord.allPackages
        fun allClasses(c: JavadocClasslikePageNode): List<JavadocClasslikePageNode> =
            c.children.filterIsInstance<JavadocClasslikePageNode>().flatMap { allClasses(it) } + c
        val types = input.flatMap {
            it.children.filterIsInstance<JavadocClasslikePageNode>().flatMap { allClasses(it) }.map { classlike -> it to classlike }
        }
        val updated = accumulator.copy(packageRecords = packages)
        return processTypes(types, updated)
    }

    private fun processTypes(
        input: List<Pair<JavadocPackagePageNode, JavadocClasslikePageNode>>,
        accumulator: SearchData
    ): SearchData {
        val types = input.map {
            SearchRecord(
                p = it.first.name,
                l = it.second.name,
                url = locationProvider.resolveOrThrow(it.second).formatToEndWithHtml()
            )
        } + allTypes
        val updated = accumulator.copy(typeRecords = types)
        return processMembers(input, updated).copy(searchIndexes = indexSearchForClasslike(input))
    }

    private fun processMembers(
        input: List<Pair<JavadocPackagePageNode, JavadocClasslikePageNode>>,
        accumulator: SearchData
    ): SearchData {
        val functions = input.flatMap {
            (it.second.constructors + it.second.methods).withoutInherited().map { function ->
                SearchRecordCreator.function(
                    packageName = it.first.name,
                    classlikeName = it.second.name,
                    input = function,
                    url = locationProvider.resolveOrThrow(function.dri, it.first.sourceSets())
                )
            }
        }

        val properties = input.flatMap {
            it.second.properties.map { property ->
                SearchRecordCreator.property(
                    packageName = it.first.name,
                    classlikeName = it.second.name,
                    property,
                    locationProvider.resolveOrThrow(property.dri, it.first.sourceSets())
                )
            }
        }

        val entries = input.flatMap {
            it.second.entries.map { entry ->
                SearchRecordCreator.entry(
                    packageName = it.first.name,
                    classlikeName = it.second.name,
                    entry,
                    locationProvider.resolveOrThrow(entry.dri, it.first.sourceSets())
                )
            }
        }

        return accumulator.copy(memberRecords = functions + properties + entries)
    }

    private fun indexSearchForClasslike(
        input: List<Pair<JavadocPackagePageNode, JavadocClasslikePageNode>>,
    ): List<SearchRecord> {
        val indexesForClasslike = input.flatMap {
            val indexes = it.second.indexes()
            indexes.map { index ->
                val label = renderNode(index)
                SearchRecord(
                    p = it.first.name,
                    c = it.second.name,
                    l = label,
                    url = resolveUrlForSearchIndex(it.second.dri.first(), it.second.sourceSets(), label)
                )
            }
        }

        val indexesForMemberNodes = input.flatMap { packageWithClasslike ->
            (packageWithClasslike.second.constructors +
                    packageWithClasslike.second.methods.withoutInherited() +
                    packageWithClasslike.second.properties +
                    packageWithClasslike.second.entries
                    ).map { it to it.indexes() }
                .flatMap { entryWithIndex ->
                    entryWithIndex.second.map {
                        val label = renderNode(it)
                        SearchRecord(
                            p = packageWithClasslike.first.name,
                            c = packageWithClasslike.second.name,
                            l = label,
                            url = resolveUrlForSearchIndex(
                                entryWithIndex.first.dri,
                                packageWithClasslike.second.sourceSets(),
                                label
                            )
                        )
                    }
                }
        }

        return indexesForClasslike + indexesForMemberNodes
    }

    private fun <T : Documentable> WithJavadocExtra<T>.indexes(): List<ContentNode> =
        extra[JavadocIndexExtra]?.index.orEmpty()

    private fun List<JavadocFunctionNode>.withoutInherited(): List<JavadocFunctionNode> = filter { !it.isInherited }

    private fun resolveUrlForSearchIndex(
        dri: DRI,
        sourceSets: Set<DisplaySourceSet>,
        label: String
    ): String =
        locationProvider.resolveOrThrow(dri, sourceSets).formatToEndWithHtml() + "#" + label
}

private data class SearchRecord(
    val p: String? = null,
    val c: String? = null,
    val l: String,
    val url: String? = null
) {
    companion object {
        val allPackages = SearchRecord(l = "All packages", url = "index.html")
        val allTypes = SearchRecord(l = "All classes", url = "allclasses.html")
    }
}

private object SearchRecordCreator {
    fun function(
        packageName: String,
        classlikeName: String,
        input: JavadocFunctionNode,
        url: String
    ): SearchRecord =
        SearchRecord(
            p = packageName,
            c = classlikeName,
            l = input.name + input.parameters.joinToString(
                prefix = "(",
                postfix = ")"
            ) { renderNode(it.type) },
            url = url.formatToEndWithHtml()
        )

    fun property(
        packageName: String,
        classlikeName: String,
        input: JavadocPropertyNode,
        url: String
    ): SearchRecord =
        SearchRecord(
            p = packageName,
            c = classlikeName,
            l = input.name,
            url = url.formatToEndWithHtml()
        )

    fun entry(packageName: String, classlikeName: String, input: JavadocEntryNode, url: String): SearchRecord =
        SearchRecord(
            p = packageName,
            c = classlikeName,
            l = input.name,
            url = url.formatToEndWithHtml()
        )
}

private data class SearchData(
    val moduleRecords: List<SearchRecord> = emptyList(),
    val packageRecords: List<SearchRecord> = emptyList(),
    val typeRecords: List<SearchRecord> = emptyList(),
    val memberRecords: List<SearchRecord> = emptyList(),
    val searchIndexes: List<SearchRecord> = emptyList()
)

private class SearchRecordJsonSerializer {
    fun serialize(record: SearchRecord): String {
        val serialized = StringBuilder()
        serialized.append("{")
        with(record) {
            if (p != null) serialized.append("\"p\":\"$p\",")
            if (c != null) serialized.append("\"c\":\"$c\",")
            serialized.append("\"l\":\"$l\"")
            if (url != null) serialized.append(",\"url\":\"$url\"")
        }
        serialized.append("}")
        return serialized.toString()
    }

    fun serialize(records: List<SearchRecord>, variable: String): String =
        "var " + variable + " = " + records.joinToString(prefix = "[", postfix = "]") { serialize(it) }
}

private fun renderNode(node: ContentNode): String =
    when (node) {
        is ContentText -> node.text
        else -> node.children.joinToString(separator = "") { renderNode(it) }
    }
