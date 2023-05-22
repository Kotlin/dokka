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
import org.jetbrains.dokka.transformers.pages.PageCreator
import java.io.File


class AuxiliaryDocPageCreator(
    private val context: DokkaContext,
) : PageCreator<AuxiliaryDocPageContext> {
    private val commentsConverter by lazy { context.plugin<DokkaBase>().querySingle { commentsToContentConverter } }
    private val signatureProvider by lazy { context.plugin<DokkaBase>().querySingle { signatureProvider } }


    override fun invoke(creationContext: AuxiliaryDocPageContext): RootPageNode {
        return AuxiliaryRootPageNode(
            name = creationContext.rootPage.pureFileName(),
            dri = setOf(AUX_ROOT_DRI),
            content = renderMarkdownContent(creationContext.rootPage),
            children = childPages(creationContext)
        )
    }

    private fun File.pureFileName() = name.split(".").first()

    private fun childPages(creationContext: AuxiliaryDocPageContext): List<PageNode> =
        creationContext.contentPages.map { contentPage(it) }


    private fun contentPage(contentFile: File, children: List<PageNode> = emptyList()): PageNode {
        val displayName = contentFile.pureFileName()
        return AuxiliaryPageNode(
            name = displayName,
            dri = setOf(DRI(packageName = AUX_PACKAGE_PLACEHOLDER, classNames = displayName)),
            content = renderMarkdownContent(contentFile),
            children = children
        )
    }

    private fun renderMarkdownContent(contentFile: File): ContentGroup {
        val sourceSetData = emptySet<DokkaSourceSet>()
        val builder = PageContentBuilder(commentsConverter, signatureProvider, context.logger)
        return builder.contentFor(
            dri = DRI(packageName = AUX_PACKAGE_PLACEHOLDER, classNames = contentFile.pureFileName()),
            kind = ContentKind.Cover,
            sourceSets = sourceSetData
        ) {
            getMarkdownContent(contentFile).let { node ->
                group(kind = ContentKind.Cover) {
                    group {
                        node.children.forEach { comment(it.root) }
                    }
                }
            }
        }
    }

    private fun getMarkdownContent(file: File): DocumentationNode =
        MarkdownParser(
            { DRI(packageName = AUX_PACKAGE_PLACEHOLDER, classNames = it) },
            file.absolutePath
        ).parse(file.readText())


    companion object {
        const val AUX_PACKAGE_PLACEHOLDER = ".aux_doc"
        val AUX_ROOT_DRI = DRI.topLevel
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
    override val name: String,
    override val dri: Set<DRI>,
    override val content: ContentNode,
    override val embeddedResources: List<String> = emptyList(),
    override val children: List<PageNode> = emptyList(),
) : RootPageNode(forceTopLevelName = false), CustomRootPage {


    override fun modified(name: String, children: List<PageNode>): RootPageNode {
        return AuxiliaryRootPageNode(name, dri, content, embeddedResources, children)
    }

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>,
    ) = AuxiliaryRootPageNode(name, dri, content, embeddedResources, children)
}

data class AuxiliaryDocPageContext(val rootPage: File, val contentPages: Set<File>) : CreationContext