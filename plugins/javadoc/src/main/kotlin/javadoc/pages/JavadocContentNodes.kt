package javadoc.pages

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*

abstract class JavadocContentNode(dri: Set<DRI>, kind: ContentKind) : ContentNode {
    abstract val contentMap: Map<String, Any?>
    override val dci: DCI = DCI(dri, kind)
    override val platforms: Set<PlatformData> = setOf(PlatformData("jvm", Platform.jvm, listOf("jvm")))
    override val style: Set<Style> = emptySet()
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = this
}

interface JavadocListEntry {
    val stringTag: String
}

class EmptyNode(
    dri: DRI,
    kind: ContentKind,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentNode {
    override val dci: DCI = DCI(setOf(dri), kind)
    override val platforms: Set<PlatformData> = setOf(PlatformData("jvm", Platform.jvm, listOf("jvm")))
    override val style: Set<Style> = emptySet()

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode =
        EmptyNode(dci.dri.first(), dci.kind as ContentKind, newExtras)
}

data class JavadocContentGroup(val dri: Set<DRI>, val kind: ContentKind, val children: List<JavadocContentNode>) :
    JavadocContentNode(dri, kind) {
    override val contentMap: Map<String, Any?> by lazy { children.fold(emptyMap<String, Any?>()) { m, cv -> m + cv.contentMap } }

    companion object {
        operator fun invoke(
            dri: Set<DRI>,
            kind: ContentKind,
            block: MutableList<JavadocContentNode>.() -> Unit
        ): JavadocContentGroup =
            JavadocContentGroup(dri, kind, mutableListOf<JavadocContentNode>().apply(block).toList())
    }
}

data class TitleNode(
    val title: String,
    val version: String,
    val dri: Set<DRI>,
    val kind: ContentKind
) : JavadocContentNode(dri, kind) {

    override val contentMap: Map<String, Any?> by lazy {
        mapOf(
            "title" to title,
            "version" to version
        )
    }

//    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = TODO()
}

fun MutableList<JavadocContentNode>.title(
    title: String, version: String, dri: Set<DRI>, kind: ContentKind
) {
    add(TitleNode(title, version, dri, kind))
}

data class ListNode(
    val tabTitle: String,
    val colTitle: String,
    val children: List<JavadocListEntry>,
    val dri: Set<DRI>,
    val kind: ContentKind
) : JavadocContentNode(dri, kind) {
    override val contentMap: Map<String, Any?> by lazy {
        mapOf(
            "tabTitle" to tabTitle,
            "colTitle" to colTitle,
            "list" to children
        )
    }
}

fun MutableList<JavadocContentNode>.list(
    tabTitle: String,
    colTitle: String,
    dri: Set<DRI>,
    kind: ContentKind,
    children: List<JavadocListEntry>
) {
    add(ListNode(tabTitle, colTitle, children, dri, kind))
}

data class SimpleJavadocListEntry(val content: String) :
    JavadocListEntry {
    override val stringTag: String = content
}

data class LinkJavadocListEntry(val name: String, val dri: Set<DRI>, val kind: ContentKind = ContentKind.Symbol, val platformData: List<PlatformData>) :
    JavadocListEntry {
    override val stringTag: String
        get() = if (builtString == null)
            throw IllegalStateException("stringTag for LinkJavadocListEntry accessed before build() call")
        else builtString!!

    private var builtString: String? = null

    fun build(body: (String, Set<DRI>, ContentKind, List<PlatformData>) -> String) {
        builtString = body(name, dri, kind, platformData)
    }
}

data class RowJavadocListEntry(val link: LinkJavadocListEntry, val doc: String): JavadocListEntry {
    override val stringTag: String = ""
}

data class CompoundJavadocListEntry(
    val name: String,
    val content: List<JavadocListEntry>
) : JavadocListEntry {
    override val stringTag: String
        get() = if (builtString == null)
            throw IllegalStateException("stringTag for CompoundJavadocListEntry accessed before build() call")
        else builtString!!

    private var builtString: String? = null

    fun build(body: (List<JavadocListEntry>) -> String) {
        builtString = body(content)
    }
}