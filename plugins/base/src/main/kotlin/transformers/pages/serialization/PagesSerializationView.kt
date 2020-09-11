package org.jetbrains.dokka.base.transformers.pages.serialization

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*

interface PagesSerializationView: PageNode {
    val content: PagesSerializationContentView
}

data class MultiModulePageView(
    override val name: String,
    override val children: List<PagesSerializationView>,
    override val content: PagesSerializationContentView,
    val dri: Set<DRI>,
    val embeddedResources: List<String>
) : MultimoduleRootPage, PagesSerializationView, RootPageNode() {
    override fun modified(name: String, children: List<PageNode>): RootPageNode {
        TODO("Not yet implemented")
    }
}

data class ModulePageView(
    override val name: String,
    override val children: List<PagesSerializationView>,
    override val content: PagesSerializationContentView,
    val embeddedResources: List<String>
) : ModulePage, PagesSerializationView, RootPageNode() {
    val dri: Set<DRI> = setOf(DRI.topLevel)

    override fun modified(name: String, children: List<PageNode>): RootPageNode {
        TODO("Not yet implemented")
    }

}

data class PackagePageView(
    override val name: String,
    override val content: PagesSerializationContentView,
    val dri: Set<DRI>,
    override val children: List<PagesSerializationView>,
    val embeddedResources: List<String> = listOf()
) : PackagePage, PagesSerializationView {

    override fun modified(name: String, children: List<PageNode>): PageNode {
        TODO("Not yet implemented")
    }
}

data class ClasslikePageView(
    override val name: String,
    override val content: PagesSerializationContentView,
    val dri: Set<DRI>,
    override val children: List<PagesSerializationView>,
    val embeddedResources: List<String> = listOf()
) : ClasslikePage, PagesSerializationView {
    override fun modified(name: String, children: List<PageNode>): PageNode {
        TODO("Not yet implemented")
    }
}

data class MemberPageView(
    override val name: String,
    override val content: PagesSerializationContentView,
    val dri: Set<DRI>,
    override val children: List<PagesSerializationView> = emptyList(),
    val embeddedResources: List<String> = listOf()
) : MemberPage, PagesSerializationView {
    override fun modified(name: String, children: List<PageNode>): PageNode {
        TODO("Not yet implemented")
    }
}