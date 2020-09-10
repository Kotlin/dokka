package org.jetbrains.dokka.base.transformers.pages.serialization

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.pages.*

interface PagesSerializationView: PageNode, ContentPage {
    override val documentable: Documentable?
        get() = null
}

data class MultiModulePageView(
    override val name: String,
    override val children: List<PagesSerializationView>,
    override val content: PagesSerializationContentView,
    override val dri: Set<DRI>,
    override val embeddedResources: List<String>
): MultimoduleRootPage, PagesSerializationView {
    override fun modified(name: String, children: List<PageNode>): PageNode {
        TODO("Not yet implemented")
    }

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage {
        TODO("Not yet implemented")
    }
}

data class ModulePageView(
    override val name: String,
    override val children: List<PagesSerializationView>,
    override val content: PagesSerializationContentView,
    override val embeddedResources: List<String>
): ModulePage, PagesSerializationView {
    override val dri: Set<DRI> = setOf(DRI.topLevel)

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage {
        TODO("Not yet implemented")
    }

    override fun modified(name: String, children: List<PageNode>): PageNode {
        TODO("Not yet implemented")
    }

}

data class PackagePageView(
    override val name: String,
    override val content: PagesSerializationContentView,
    override val dri: Set<DRI>,
    override val children: List<PagesSerializationView>,
    override val embeddedResources: List<String> = listOf()
): PackagePage, PagesSerializationView {

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage {
        TODO("Not yet implemented")
    }

    override fun modified(name: String, children: List<PageNode>): PageNode {
        TODO("Not yet implemented")
    }
}

data class ClasslikePageView(
    override val name: String,
    override val content: PagesSerializationContentView,
    override val dri: Set<DRI>,
    override val children: List<PagesSerializationView>,
    override val embeddedResources: List<String> = listOf()
): ClasslikePage, PagesSerializationView {
    override fun modified(name: String, children: List<PageNode>): PageNode {
        TODO("Not yet implemented")
    }

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage {
        TODO("Not yet implemented")
    }
}

data class MemberPageView(
    override val name: String,
    override val content: ContentNode,
    override val dri: Set<DRI>,
    override val children: List<PageNode> = emptyList(),
    override val embeddedResources: List<String> = listOf()
): MemberPage, PagesSerializationView {
    override fun modified(name: String, children: List<PageNode>): PageNode {
        TODO("Not yet implemented")
    }

    override fun modified(
        name: String,
        content: ContentNode,
        dri: Set<DRI>,
        embeddedResources: List<String>,
        children: List<PageNode>
    ): ContentPage {
        TODO("Not yet implemented")
    }
}