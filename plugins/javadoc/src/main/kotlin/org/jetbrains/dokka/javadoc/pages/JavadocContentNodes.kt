package org.jetbrains.dokka.javadoc.pages

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*

enum class JavadocContentKind : Kind {
    AllClasses, OverviewSummary, PackageSummary, Class, OverviewTree, PackageTree, IndexPage
}

abstract class JavadocContentNode(
    dri: Set<DRI>,
    kind: Kind,
    override val sourceSets: Set<DokkaSourceSet>
) : ContentNode {
    override val dci: DCI = DCI(dri, kind)
    override val style: Set<Style> = emptySet()
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = this
}

interface JavadocList {
    val tabTitle: String
    val colTitle: String
    val children: List<JavadocListEntry>
}

interface JavadocListEntry {
    val stringTag: String
}

class EmptyNode(
    dri: DRI,
    kind: Kind,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentNode {
    override val dci: DCI = DCI(setOf(dri), kind)
    override val style: Set<Style> = emptySet()

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode =
        EmptyNode(dci.dri.first(), dci.kind, sourceSets, newExtras)

    override fun hasAnyContent(): Boolean = false
}

class JavadocContentGroup(
    val dri: Set<DRI>,
    val kind: Kind,
    sourceSets: Set<DokkaSourceSet>,
    override val children: List<JavadocContentNode>
) : JavadocContentNode(dri, kind, sourceSets) {

    companion object {
        operator fun invoke(
            dri: Set<DRI>,
            kind: Kind,
            sourceSets: Set<DokkaSourceSet>,
            block: JavaContentGroupBuilder.() -> Unit
        ): JavadocContentGroup =
            JavadocContentGroup(dri, kind, sourceSets, JavaContentGroupBuilder(sourceSets).apply(block).list)
    }

    override fun hasAnyContent(): Boolean = children.isNotEmpty()
}

class JavaContentGroupBuilder(val sourceSets: Set<DokkaSourceSet>) {
    val list = mutableListOf<JavadocContentNode>()
}

class TitleNode(
    val title: String,
    val subtitle: List<ContentNode>,
    val version: String,
    val parent: String?,
    val dri: Set<DRI>,
    val kind: Kind,
    sourceSets: Set<DokkaSourceSet>
) : JavadocContentNode(dri, kind, sourceSets) {
    override fun hasAnyContent(): Boolean = !title.isBlank() || !version.isBlank() || subtitle.isNotEmpty()
}

fun JavaContentGroupBuilder.title(
    title: String,
    subtitle: List<ContentNode>,
    version: String,
    parent: String? = null,
    dri: Set<DRI>,
    kind: Kind
) {
    list.add(TitleNode(title, subtitle, version, parent, dri, kind, sourceSets))
}

class RootListNode(
    val entries: List<LeafListNode>,
    val dri: Set<DRI>,
    val kind: Kind,
    sourceSets: Set<DokkaSourceSet>,
) : JavadocContentNode(dri, kind, sourceSets) {
    override fun hasAnyContent(): Boolean = children.isNotEmpty()
}

class LeafListNode(
    val tabTitle: String,
    val colTitle: String,
    val entries: List<JavadocListEntry>,
    val dri: Set<DRI>,
    val kind: Kind,
    sourceSets: Set<DokkaSourceSet>
) : JavadocContentNode(dri, kind, sourceSets) {
    override fun hasAnyContent(): Boolean = children.isNotEmpty()
}


fun JavaContentGroupBuilder.rootList(
    dri: Set<DRI>,
    kind: Kind,
    rootList: List<JavadocList>
) {
    val children = rootList.map {
        LeafListNode(it.tabTitle, it.colTitle, it.children, dri, kind, sourceSets)
    }
    list.add(RootListNode(children, dri, kind, sourceSets))
}

fun JavaContentGroupBuilder.leafList(
    dri: Set<DRI>,
    kind: Kind,
    leafList: JavadocList
) {
    list.add(LeafListNode(leafList.tabTitle, leafList.colTitle, leafList.children, dri, kind, sourceSets))
}

fun JavadocList(tabTitle: String, colTitle: String, children: List<JavadocListEntry>) = object : JavadocList {
    override val tabTitle = tabTitle
    override val colTitle = colTitle
    override val children = children
}

class LinkJavadocListEntry(
    val name: String,
    val dri: Set<DRI>,
    val kind: Kind = ContentKind.Symbol,
    val sourceSets: Set<DokkaSourceSet>
) :
    JavadocListEntry {
    override val stringTag: String
        get() = if (builtString == null)
            throw IllegalStateException("stringTag for LinkJavadocListEntry accessed before build() call")
        else builtString!!

    private var builtString: String? = null

    fun build(body: (String, Set<DRI>, Kind, List<DokkaSourceSet>) -> String) {
        builtString = body(name, dri, kind, sourceSets.toList())
    }
}

data class RowJavadocListEntry(val link: LinkJavadocListEntry, val doc: List<ContentNode>) : JavadocListEntry {
    override val stringTag: String = ""
}

data class JavadocSignatureContentNode(
    val dri: DRI,
    val kind: Kind = ContentKind.Symbol,
    val annotations: ContentNode?,
    val modifiers: ContentNode?,
    val signatureWithoutModifiers: ContentNode,
    val supertypes: ContentNode?
): JavadocContentNode(setOf(dri), kind, signatureWithoutModifiers.sourceSets) {
    override fun hasAnyContent(): Boolean = true
}
