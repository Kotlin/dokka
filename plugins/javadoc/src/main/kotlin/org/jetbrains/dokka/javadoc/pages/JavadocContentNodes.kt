/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc.pages

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*

public enum class JavadocContentKind : Kind {
    AllClasses, OverviewSummary, PackageSummary, Class, OverviewTree, PackageTree, IndexPage
}

public abstract class JavadocContentNode(
    dri: Set<DRI>,
    kind: Kind,
    override val sourceSets: Set<DisplaySourceSet>
) : ContentNode {
    override val dci: DCI = DCI(dri, kind)
    override val style: Set<Style> = emptySet()
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = this
}

public interface JavadocList {
    public val tabTitle: String
    public val colTitle: String
    public val children: List<JavadocListEntry>
}

public interface JavadocListEntry {
    public val stringTag: String
}

public data class EmptyNode(
    val dri: DRI,
    val kind: Kind,
    override val sourceSets: Set<DisplaySourceSet>,
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentNode {
    override val dci: DCI = DCI(setOf(dri), kind)
    override val style: Set<Style> = emptySet()

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): EmptyNode = copy(extra = newExtras)

    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): EmptyNode =
        copy(sourceSets = sourceSets)

    override fun hasAnyContent(): Boolean = false
}

public data class JavadocContentGroup(
    val dri: Set<DRI>,
    val kind: Kind,
    override val sourceSets: Set<DisplaySourceSet>,
    override val children: List<JavadocContentNode>
) : JavadocContentNode(dri, kind, sourceSets) {

    public companion object {
        public operator fun invoke(
            dri: Set<DRI>,
            kind: Kind,
            sourceSets: Set<DisplaySourceSet>,
            block: JavaContentGroupBuilder.() -> Unit
        ): JavadocContentGroup =
            JavadocContentGroup(dri, kind, sourceSets, JavaContentGroupBuilder(sourceSets).apply(block).list)
    }

    override fun hasAnyContent(): Boolean = children.isNotEmpty()

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): JavadocContentGroup = this

    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): JavadocContentGroup =
        copy(sourceSets = sourceSets)
}

public class JavaContentGroupBuilder(
    public val sourceSets: Set<DisplaySourceSet>
) {
    public val list: MutableList<JavadocContentNode> = mutableListOf<JavadocContentNode>()
}

public data class TitleNode(
    val title: String,
    val subtitle: List<ContentNode>,
    val version: String?,
    val parent: String?,
    val dri: Set<DRI>,
    val kind: Kind,
    override val sourceSets: Set<DisplaySourceSet>
) : JavadocContentNode(dri, kind, sourceSets) {
    override fun hasAnyContent(): Boolean = title.isNotBlank() || !version.isNullOrBlank() || subtitle.isNotEmpty()

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): TitleNode = this

    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): TitleNode =
        copy(sourceSets = sourceSets)
}

public fun JavaContentGroupBuilder.title(
    title: String,
    subtitle: List<ContentNode>,
    version: String? = null,
    parent: String? = null,
    dri: Set<DRI>,
    kind: Kind
) {
    list.add(TitleNode(title, subtitle, version, parent, dri, kind, sourceSets))
}

public data class RootListNode(
    val entries: List<LeafListNode>,
    val dri: Set<DRI>,
    val kind: Kind,
    override val sourceSets: Set<DisplaySourceSet>,
) : JavadocContentNode(dri, kind, sourceSets) {
    override fun hasAnyContent(): Boolean = children.isNotEmpty()


    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): RootListNode =
        copy(sourceSets = sourceSets)
}

public data class LeafListNode(
    val tabTitle: String,
    val colTitle: String,
    val entries: List<JavadocListEntry>,
    val dri: Set<DRI>,
    val kind: Kind,
    override val sourceSets: Set<DisplaySourceSet>
) : JavadocContentNode(dri, kind, sourceSets) {
    override fun hasAnyContent(): Boolean = children.isNotEmpty()

    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): LeafListNode = copy(sourceSets = sourceSets)
}


public fun JavaContentGroupBuilder.rootList(
    dri: Set<DRI>,
    kind: Kind,
    rootList: List<JavadocList>
) {
    val children = rootList.map {
        LeafListNode(it.tabTitle, it.colTitle, it.children, dri, kind, sourceSets)
    }
    list.add(RootListNode(children, dri, kind, sourceSets))
}

public fun JavaContentGroupBuilder.leafList(
    dri: Set<DRI>,
    kind: Kind,
    leafList: JavadocList
) {
    list.add(LeafListNode(leafList.tabTitle, leafList.colTitle, leafList.children, dri, kind, sourceSets))
}

public fun JavadocList(tabTitle: String, colTitle: String, children: List<JavadocListEntry>): JavadocList {
    return object : JavadocList {
        override val tabTitle = tabTitle
        override val colTitle = colTitle
        override val children = children
    }
}

public class LinkJavadocListEntry(
    public val name: String,
    public val dri: Set<DRI>,
    public val kind: Kind = ContentKind.Symbol,
    public val sourceSets: Set<DisplaySourceSet>
) : JavadocListEntry {
    override val stringTag: String
        get() {
            return if (builtString == null) {
                throw IllegalStateException("stringTag for LinkJavadocListEntry accessed before build() call")
            } else {
                builtString!!
            }
        }

    private var builtString: String? = null

    public fun build(body: (String, Set<DRI>, Kind, List<DisplaySourceSet>) -> String) {
        builtString = body(name, dri, kind, sourceSets.toList())
    }
}

public data class RowJavadocListEntry(val link: LinkJavadocListEntry, val doc: List<ContentNode>) : JavadocListEntry {
    override val stringTag: String = ""
}

public data class JavadocSignatureContentNode(
    val dri: DRI,
    val kind: Kind = ContentKind.Symbol,
    val annotations: ContentNode?,
    val modifiers: ContentNode?,
    val signatureWithoutModifiers: ContentNode,
    val supertypes: ContentNode?
) : JavadocContentNode(setOf(dri), kind, signatureWithoutModifiers.sourceSets) {
    override fun hasAnyContent(): Boolean = true

    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): JavadocSignatureContentNode {
        return copy(signatureWithoutModifiers = signatureWithoutModifiers.withSourceSets(sourceSets))
    }
}
