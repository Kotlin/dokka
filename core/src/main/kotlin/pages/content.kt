package org.jetbrains.dokka.pages

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.WithChildren
import org.jetbrains.dokka.model.properties.WithExtraProperties

interface Content : WithChildren<Content>, WithExtraProperties<Content> {
    val dci: DCI
    val sourceSets: Set<DisplaySourceSet>
    val style: Set<Style>

    override val children: List<Content>
        get() = emptyList()
}

interface Text: Content {
    val text: String
}

interface BreakLine: Content

interface Header: Content {
    val level: Int
}

interface Code: Content {
    val language: String
}

interface ResolvedLink: Content {
    val address: String
}

interface UnresolvedLink: Content {
    val address: DRI
}

interface Table: Content {
    val header: List<Content>
}

interface ElementList: Content {
    val ordered: Boolean
}

interface Group: Content

interface DivergentGroup: Content {
    val groupID: ContentDivergentGroup.GroupID
    val implicitlySourceSetHinted: Boolean
}

interface DivergentInstance: Content {
    val before: Content?
    val divergent: Content
    val after: Content?
}

interface PlatformHinted: Content {
    val inner: Content
}

