package org.jetbrains.dokka.base.transformers.pages.serialization

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*

interface PagesSerializationView: PageNode {
    val content: PagesSerializationContentView
    val embeddedResources: List<String>
    val dri: Set<DRI>

    fun withNewContent(newContent: PagesSerializationContentView): PagesSerializationView
}

data class MultiModulePageView(
    override val name: String,
    override val children: List<PagesSerializationView>,
    override val content: PagesSerializationContentView,
    override val dri: Set<DRI>,
    override val embeddedResources: List<String>
) : MultimoduleRootPage, PagesSerializationView, RootPageNode() {

    override fun withNewContent(newContent: PagesSerializationContentView): PagesSerializationView =
        copy(content = newContent)

    override fun modified(name: String, children: List<PageNode>): RootPageNode =
        copy(name = name, children = children as List<PagesSerializationView>)
}

data class ModulePageView(
    override val name: String,
    override val children: List<PagesSerializationView>,
    override val content: PagesSerializationContentView,
    override val embeddedResources: List<String>
) : ModulePage, PagesSerializationView, RootPageNode() {

    override val dri: Set<DRI> = setOf(DRI.topLevel)

    override fun withNewContent(newContent: PagesSerializationContentView): PagesSerializationView =
        copy(content = newContent)

    override fun modified(name: String, children: List<PageNode>): RootPageNode =
        copy(name = name, children = children as List<PagesSerializationView>)

}

data class PackagePageView(
    override val name: String,
    override val content: PagesSerializationContentView,
    override val dri: Set<DRI>,
    override val children: List<PagesSerializationView>,
    override val embeddedResources: List<String> = listOf()
) : PackagePage, PagesSerializationView {
    override fun withNewContent(newContent: PagesSerializationContentView): PagesSerializationView =
        copy(content = newContent)

    override fun modified(name: String, children: List<PageNode>): PageNode =
        copy(name = name, children = children as List<PagesSerializationView>)
}

data class ClasslikePageView(
    override val name: String,
    override val content: PagesSerializationContentView,
    override val dri: Set<DRI>,
    override val children: List<PagesSerializationView>,
    override val embeddedResources: List<String> = listOf()
) : ClasslikePage, PagesSerializationView {
    override fun withNewContent(newContent: PagesSerializationContentView): PagesSerializationView =
        copy(content = newContent)

    override fun modified(name: String, children: List<PageNode>): PageNode =
        copy(name = name, children = children as List<PagesSerializationView>)
}

data class MemberPageView(
    override val name: String,
    override val content: PagesSerializationContentView,
    override val dri: Set<DRI>,
    override val children: List<PagesSerializationView> = emptyList(),
    override val embeddedResources: List<String> = listOf()
) : MemberPage, PagesSerializationView {
    override fun withNewContent(newContent: PagesSerializationContentView): PagesSerializationView =
        copy(content = newContent)

    override fun modified(name: String, children: List<PageNode>): PageNode =
        copy(name = name, children = children as List<PagesSerializationView>)
}

interface RendererSpecificPageView: PagesSerializationView {
    val strategy: RenderingStrategyView
    override val content: PagesSerializationContentView
        get() = TODO("Not yet implemented")
    override val embeddedResources: List<String>
        get() = emptyList()
    override val dri: Set<DRI>
        get() = emptySet()

    override fun withNewContent(newContent: PagesSerializationContentView): PagesSerializationView = this
}

data class RendererSpecificRootPageView(
    override val name: String,
    override val children: List<PagesSerializationView>,
    override val strategy: RenderingStrategyView
) : RootPageNode(), RendererSpecificPageView {
    override fun modified(name: String, children: List<PageNode>): RendererSpecificRootPageView =
        RendererSpecificRootPageView(name, children as List<PagesSerializationView>, strategy)
}

data class RendererSpecificResourcePageView(
    override val name: String,
    override val children: List<PagesSerializationView>,
    override val strategy: RenderingStrategyView
): RendererSpecificPageView {
    override fun modified(name: String, children: List<PageNode>): RendererSpecificResourcePageView =
        RendererSpecificResourcePageView(name, children as List<PagesSerializationView>, strategy)
}

sealed class RenderingStrategyView {
    data class CopyView(val from: String) : RenderingStrategyView()
    data class WriteView(val text: String, val variables: List<String>) : RenderingStrategyView()
    object DoNothingView: RenderingStrategyView()
}