package org.jetbrains.dokka.newFrontend.pages

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*

interface NewFrontendContentPage : PageNode, ContentPage {
    override fun modified(name: String, children: List<PageNode>): PageNode = this

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage = this
}

class MultiModulePageNode(
    override val name: String,
    override val children: List<PageNode>,
    override val content: ContentNode,
    override val dri: Set<DRI>,
    override val documentable: Documentable?,
    override val embeddedResources: List<String>
) : MultimoduleRootPage, NewFrontendContentPage

class ModulePageNode(
    override val name: String,
    val description: ContentNode,
    override val children: List<PackagePageNode> = emptyList(),
    override val content: ModuleContentNode,
    override val dri: Set<DRI> = emptySet(),
    override val documentable: Documentable? = null,
    override val embeddedResources: List<String> = emptyList()
) : RootPageNode(), ModulePage, NewFrontendContentPage {
    override fun modified(name: String, children: List<PageNode>): RootPageNode = this
}

data class ModuleContentNode(
    val name: String,
    val packages: List<ModulePackageElement>,
    override val dci: DCI = DCI(emptySet(), ContentKind.Main),
    override val sourceSets: Set<DisplaySourceSet> = emptySet(),
    override val style: Set<Style> = emptySet(),
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentNode {
    override fun hasAnyContent(): Boolean = true

    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): ContentNode = copy(sourceSets = sourceSets)

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = copy(extra = newExtras)
}

data class ModulePackageElement(
    val name: String,
    val dri: DRI,
    val sourceSets: Set<DisplaySourceSet>,
    val description: ContentNode
)

class PackagePageNode(
    override val name: String,
    override val children: List<PageNode>,
    override val content: PackageContentNode,
    override val dri: Set<DRI>,
    override val documentable: Documentable?,
    override val embeddedResources: List<String>
) : PackagePage, NewFrontendContentPage

data class PackageContentNode(
    val name: String,
    val description: ContentNode,
    override val dci: DCI,
    override val sourceSets: Set<DisplaySourceSet>,
    override val style: Set<Style> = emptySet(),
    override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentNode {
    override fun hasAnyContent(): Boolean = true

    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): ContentNode = copy(sourceSets = sourceSets)

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = copy(extra = newExtras)
}

class ClasslikePageNode(
    override val name: String,
    override val children: List<PageNode>,
    override val content: ClasslikeContentNode,
    override val dri: Set<DRI>,
    override val documentable: Documentable?,
    override val embeddedResources: List<String>
) : ClasslikePage, NewFrontendContentPage

data class ClasslikeContentNode(
    override val dci: DCI,
    override val sourceSets: Set<DisplaySourceSet>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode>
) : ContentNode {
    override fun hasAnyContent(): Boolean = true

    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): ContentNode = copy(sourceSets = sourceSets)

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = copy(extra = newExtras)
}

class MemberPageNode(
    override val name: String,
    override val children: List<PageNode>,
    override val content: MemberContentNode,
    override val dri: Set<DRI>,
    override val documentable: Documentable?,
    override val embeddedResources: List<String>
) : MemberPage, NewFrontendContentPage

data class MemberContentNode(
    override val dci: DCI,
    override val sourceSets: Set<DisplaySourceSet>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode>
) : ContentNode {
    override fun hasAnyContent(): Boolean = true

    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): ContentNode = copy(sourceSets = sourceSets)

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = copy(extra = newExtras)
}