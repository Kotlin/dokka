package org.jetbrains.dokka.newFrontend.pages

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.SourceSetDependent
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.newFrontend.renderer.ContentNodeSerializer
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

@Serializable
class ModulePageNode(
    override val name: String,
    @Transient override val children: List<PageNode> = emptyList(),
    override val content: ModuleContentNode,
    @Transient override val dri: Set<DRI> = emptySet(),
    @Transient override val documentable: Documentable? = null,
    @Transient override val embeddedResources: List<String> = emptyList()
) : RootPageNode(), ModulePage, NewFrontendContentPage {
    override fun modified(name: String, children: List<PageNode>): RootPageNode = this
}

@Serializable
data class ModuleContentNode(
    val name: String,
    val packages: List<ModulePackageElement>,
    @Transient override val dci: DCI = DCI(emptySet(), ContentKind.Main),
    @Transient override val sourceSets: Set<DisplaySourceSet> = emptySet(),
    override val style: Set<Style> = emptySet(),
    @Transient override val extra: PropertyContainer<ContentNode> = PropertyContainer.empty()
) : ContentNode {
    override fun hasAnyContent(): Boolean = true

    override fun withSourceSets(sourceSets: Set<DisplaySourceSet>): ContentNode = copy(sourceSets = sourceSets)

    override fun withNewExtras(newExtras: PropertyContainer<ContentNode>): ContentNode = copy(extra = newExtras)
}

@Serializable
data class ModulePackageElement(
    val name: String,
    @Transient val dri: DRI = DRI.topLevel,
    @Serializable(with = ContentNodeSerializer::class) val description: ContentNode
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
    override val dci: DCI,
    override val sourceSets: Set<DisplaySourceSet>,
    override val style: Set<Style>,
    override val extra: PropertyContainer<ContentNode>
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