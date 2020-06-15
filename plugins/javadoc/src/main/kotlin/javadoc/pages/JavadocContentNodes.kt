package javadoc.pages

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.SourceSetData
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*

enum class JavadocContentKind : Kind {
    AllClasses, OverviewSummary, PackageSummary, Class, OverviewTree, PackageTree
}

abstract class JavadocContentNode(
    dri: Set<DRI>,
    kind: Kind,
    override val sourceSets: Set<SourceSetData>
) : ContentNode {
    abstract val contentMap: Map<String, Any?>
    override val dci: DCI = DCI(dri, kind)
    override val style: Set<Style> = emptySet()
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = this
}

interface JavadocListEntry {
    val stringTag: String
}

class EmptyNode(
    dri: DRI,
    kind: Kind,
    override val sourceSets: Set<SourceSetData>,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentNode {
    override val dci: DCI = DCI(setOf(dri), kind)
    override val style: Set<Style> = emptySet()

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode =
        EmptyNode(dci.dri.first(), dci.kind, sourceSets, newExtras)
}

class JavadocContentGroup(
    val dri: Set<DRI>,
    val kind: Kind,
    sourceSets: Set<SourceSetData>,
    val children: List<JavadocContentNode>
) : JavadocContentNode(dri, kind, sourceSets) {
    override val contentMap: Map<String, Any?> by lazy { children.fold(emptyMap<String, Any?>()) { m, cv -> m + cv.contentMap } }

    companion object {
        operator fun invoke(
            dri: Set<DRI>,
            kind: Kind,
            sourceSets: Set<SourceSetData>,
            block: JavaContentGroupBuilder.() -> Unit
        ): JavadocContentGroup =
            JavadocContentGroup(dri, kind, sourceSets, JavaContentGroupBuilder(sourceSets).apply(block).list)
    }
}

class JavaContentGroupBuilder(val sourceSets: Set<SourceSetData>) {
    val list = mutableListOf<JavadocContentNode>()
}

class TitleNode(
    val title: String,
    val version: String,
    val parent: String?,
    val dri: Set<DRI>,
    val kind: Kind,
    sourceSets: Set<SourceSetData>
) : JavadocContentNode(dri, kind, sourceSets) {

    override val contentMap: Map<String, Any?> by lazy {
        mapOf(
            "title" to title,
            "version" to version,
            "packageName" to parent
        )
    }

//    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = TODO()
}

fun JavaContentGroupBuilder.title(
    title: String,
    version: String,
    parent: String? = null,
    dri: Set<DRI>,
    kind: Kind
) {
    list.add(TitleNode(title, version, parent, dri, kind, sourceSets))
}

class ListNode(
    val tabTitle: String,
    val colTitle: String,
    val children: List<JavadocListEntry>,
    val dri: Set<DRI>,
    val kind: Kind,
    sourceSets: Set<SourceSetData>
) : JavadocContentNode(dri, kind, sourceSets) {
    override val contentMap: Map<String, Any?> by lazy {
        mapOf(
            "tabTitle" to tabTitle,
            "colTitle" to colTitle,
            "list" to children
        )
    }
}

fun JavaContentGroupBuilder.list(
    tabTitle: String,
    colTitle: String,
    dri: Set<DRI>,
    kind: Kind,
    children: List<JavadocListEntry>
) {
    list.add(ListNode(tabTitle, colTitle, children, dri, kind, sourceSets))
}

data class SimpleJavadocListEntry(val content: String) : JavadocListEntry {
    override val stringTag: String = content
}

class LinkJavadocListEntry(
    val name: String,
    val dri: Set<DRI>,
    val kind: Kind = ContentKind.Symbol,
    val sourceSets: Set<SourceSetData>
) :
    JavadocListEntry {
    override val stringTag: String
        get() = if (builtString == null)
            throw IllegalStateException("stringTag for LinkJavadocListEntry accessed before build() call")
        else builtString!!

    private var builtString: String? = null

    fun build(body: (String, Set<DRI>, Kind, List<SourceSetData>) -> String) {
        builtString = body(name, dri, kind, sourceSets.toList())
    }
}

data class RowJavadocListEntry(val link: LinkJavadocListEntry, val doc: List<ContentNode>) : JavadocListEntry {
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