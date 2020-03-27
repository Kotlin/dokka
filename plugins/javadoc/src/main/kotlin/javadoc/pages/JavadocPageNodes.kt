package javadoc.pages

import org.jetbrains.dokka.base.renderers.platforms
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.pages.*

interface JavadocPageNode : ContentPage {
    val contentMap: Map<String, Any?>
}

class JavadocModulePageNode(
    override val name: String, override val content: JavadocContentNode, override val children: List<PageNode>,
    override val dri: Set<DRI>
) :
    RootPageNode(),
    JavadocPageNode {
    override val contentMap: Map<String, Any?> by lazy { mapOf("kind" to "main") + content.contentMap }

    val version: String = "0.0.1"
    val pathToRoot: String = ""

    override val documentable: Documentable? = null
    override val embeddedResources: List<String> = emptyList()
    override fun modified(name: String, ch: List<PageNode>): RootPageNode =
        JavadocModulePageNode(name, content, ch, dri)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        ch: List<PageNode>
    ): ContentPage = JavadocModulePageNode(name, content as JavadocContentNode, ch, dri)
}

class JavadocPackagePageNode(
    override val name: String,
    override val content: JavadocContentNode,
    override val dri: Set<DRI>,

    override val documentable: Documentable? = null,
    override val children: List<JavadocClasslikePageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf()
) : JavadocPageNode {
    override val contentMap: Map<String, Any?> by lazy { mapOf("kind" to "package") + content.contentMap }
    override fun modified(
        name: String,
        ch: List<PageNode>
    ): PageNode = JavadocPackagePageNode(
        name,
        content,
        dri,
        documentable,
        ch.map { it as JavadocClasslikePageNode },
        embeddedResources
    )

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        ch: List<PageNode>
    ): ContentPage =
        JavadocPackagePageNode(
            name,
            content as JavadocContentNode,
            dri,
            documentable,
            ch.map { it as JavadocClasslikePageNode },
            embeddedResources
        )
}

class JavadocClasslikePageNode(
    override val name: String,
    override val content: JavadocContentNode,
    override val dri: Set<DRI>,

    override val documentable: Documentable? = null,
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf()
) : JavadocPageNode {
    override val contentMap: Map<String, Any?> by lazy { mapOf("kind" to "class") + content.contentMap }
    override fun modified(
        name: String,
        children: List<PageNode>
    ): PageNode = JavadocClasslikePageNode(name, content, dri, documentable, children, embeddedResources)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage =
        JavadocClasslikePageNode(name, content as JavadocContentNode, dri, documentable, children, embeddedResources)
}

class AllClassesPage(val classes: List<JavadocClasslikePageNode>) :
    JavadocPageNode {
    val classEntries = classes.map { LinkJavadocListEntry(it.name, it.dri, ContentKind.Classlikes, it.platforms()) }

    override val contentMap: Map<String, Any?> = mapOf(
        "title" to "All Classes",
        "list" to classEntries
    )

    override val name: String = "All Classes"
    override val dri: Set<DRI> = setOf(DRI.topLevel)

    override val documentable: Documentable? = null
    override val embeddedResources: List<String> = emptyList()

    override val content: ContentNode =
        EmptyNode(
            DRI.topLevel,
            ContentKind.Classlikes
        )

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage = TODO()

    override fun modified(name: String, children: List<PageNode>): PageNode =
        TODO()

    override val children: List<PageNode> = emptyList()


}

interface ContentValue
data class StringValue(val text: String) : ContentValue
data class ListValue(val list: List<String>) : ContentValue