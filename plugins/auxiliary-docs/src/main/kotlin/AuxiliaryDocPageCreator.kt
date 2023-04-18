package org.jetbrains.dokka.auxiliaryDocs

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.parsers.MarkdownParser
import org.jetbrains.dokka.base.translators.documentables.PageContentBuilder
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.CreationContext
import java.io.File


class AuxiliaryDocPageCreator(
    private val context: DokkaContext,
) {
    private val commentsConverter by lazy { context.plugin<DokkaBase>().querySingle { commentsToContentConverter } }
    private val signatureProvider by lazy { context.plugin<DokkaBase>().querySingle { signatureProvider } }

    fun root(creationContext: AuxiliaryDocPageContext): RootPageNode {
        return AuxiliaryRootPageNode(
            name = creationContext.page,
            dri = setOf(DRI(packageName = AUX_PACKAGE_PLACEHOLDER, classNames = creationContext.page)),
            content = renderContent(creationContext)
        )
    }

    fun page(creationContext: AuxiliaryDocPageContext): PageNode {
        return AuxiliaryPageNode(
            name = creationContext.page,
            dri = setOf(DRI(packageName = AUX_PACKAGE_PLACEHOLDER, classNames = creationContext.page)),
            content = renderContent(creationContext)
        )
    }

    private fun renderContent(creationContext: AuxiliaryDocPageContext): ContentGroup {
        val sourceSetData = emptySet<DokkaSourceSet>()
        val builder = PageContentBuilder(commentsConverter, signatureProvider, context.logger)
        return builder.contentFor(
            dri = DRI(packageName = AUX_PACKAGE_PLACEHOLDER, classNames = creationContext.page),
            kind = ContentKind.Cover,
            sourceSets = sourceSetData
        ) {
            getMarkdownContent(creationContext.configuration.docs).takeIf { it.isNotEmpty() }?.let { nodes ->
                group(kind = ContentKind.Cover) {
                    nodes.forEach { node ->
                        group {
                            node.children.forEach { comment(it.root) }
                        }
                    }
                }
            }
        }
    }

    private fun getMarkdownContent(files: Set<File>): List<DocumentationNode> =
        files.map { MarkdownParser({ null }, it.absolutePath).parse(it.readText()) }


    companion object {
        const val AUX_PACKAGE_PLACEHOLDER = ".ext"
    }
}


class AuxiliaryPageNode(
    override val name: String,
    override val dri: Set<DRI>,
    override val content: ContentNode,
    override val embeddedResources: List<String> = emptyList(),
    override val children: List<PageNode> = emptyList(),
) : ContentPage {

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>,
    ): ContentPage {
        return AuxiliaryPageNode(name, dri, content, embeddedResources, children)
    }

    override fun modified(name: String, children: List<PageNode>): PageNode {
        return AuxiliaryPageNode(name, dri, content, embeddedResources, children)
    }
}

class AuxiliaryRootPageNode(
    override val name: String = "Home",
    override val dri: Set<DRI>,
    override val content: ContentNode,
    override val embeddedResources: List<String> = emptyList(),
    override val children: List<PageNode> = emptyList(),
) : RootPageNode(forceTopLevelName = true), CustomRootPage {


    override fun modified(name: String, children: List<PageNode>): RootPageNode {
        return AuxiliaryRootPageNode(name, dri, content, embeddedResources, children)
    }

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>,
    ) =
        if (name == this.name && content === this.content && embeddedResources === this.embeddedResources && children shallowEq this.children) this
        else AuxiliaryRootPageNode(name, dri, content, embeddedResources, children)
}

private infix fun <T> List<T>.shallowEq(other: List<T>) =
    this === other || (this.size == other.size && (this zip other).all { (a, b) -> a === b })

data class AuxiliaryDocPageContext(val page: String, val configuration: AuxiliaryConfiguration) : CreationContext