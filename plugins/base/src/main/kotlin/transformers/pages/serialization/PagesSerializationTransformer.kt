package org.jetbrains.dokka.base.transformers.pages.serialization

import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.pages.PlatformHintedContent
import org.jetbrains.dokka.transformers.pages.PageTransformer

open class PagesSerializationTransformer(
    val contentSerializationTransformer: ContentSerializationTransformer
) : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode = convert(input) as RootPageNode

    open fun convert(pageNode: PageNode): PagesSerializationView =
        when (pageNode) {
            is ContentPage -> {
                when (pageNode) {
                    is ModulePage -> convert(pageNode)
                    is PackagePage -> convert(pageNode)
                    is ClasslikePage -> convert(pageNode)
                    is MemberPage -> convert(pageNode)
                    //TODO what should happen if we have 2 plugins and both add custom pages? can we do it better?
                    else -> throw IllegalStateException("Attempting to convert unsupported page: ${pageNode.javaClass.canonicalName}")
                }
            }
            else -> throw IllegalStateException("Attempting to convert unsupported page: ${pageNode.javaClass.canonicalName}")
        }

    open fun <T> convert(modulePage: T): PagesSerializationView
            where T : ModulePage,
                  T : ContentPage =
        with(modulePage) {
            ModulePageView(
                name = name,
                children = children.map { convert(it) },
                content = contentSerializationTransformer(content),
                embeddedResources = embeddedResources
            )
        }

    open fun <T> convert(packagePage: T): PackagePageView
            where T : PackagePage,
                  T : ContentPage =
        with(packagePage) {
            PackagePageView(
                name = name,
                dri = dri,
                children = children.map { convert(it) },
                content = contentSerializationTransformer(content),
                embeddedResources = embeddedResources
            )
        }

    open fun <T> convert(classlikePage: T): ClasslikePageView
            where T : ClasslikePage,
                  T : ContentPage =
        with(classlikePage) {
            ClasslikePageView(
                name = name,
                dri = dri,
                children = children.map { convert(it) },
                content = contentSerializationTransformer(content),
                embeddedResources = embeddedResources
            )
        }

    open fun <T> convert(memberPage: T): MemberPageView
            where T : MemberPage,
                  T : ContentPage =
        with(memberPage) {
            MemberPageView(
                name = name,
                dri = dri,
                children = children.map { convert(it) },
                content = contentSerializationTransformer(content),
                embeddedResources = embeddedResources
            )
        }
}

open class ContentSerializationTransformer : (ContentNode) -> PagesSerializationContentView {
    override fun invoke(contentNode: ContentNode): PagesSerializationContentView = convert(contentNode)

    open fun convert(contentNode: ContentNode): PagesSerializationContentView =
        when (contentNode) {
            is ContentText -> convert(contentNode)
            is ContentBreakLine -> convert(contentNode)
            is ContentHeader -> convert(contentNode)
            is ContentCode -> convert(contentNode)
            is ContentResolvedLink -> convert(contentNode)
            is ContentDRILink -> convert(contentNode)
            is ContentTable -> convert(contentNode)
            is ContentList -> convert(contentNode)
            is ContentGroup -> convert(contentNode)
            is ContentDivergentGroup -> convert(contentNode)
            is ContentDivergentInstance -> convert(contentNode)
            is PlatformHintedContent -> convert(contentNode)
            else -> throw IllegalStateException("Trying to convert unsupported content node: ${contentNode.javaClass.canonicalName}")
        }

    open fun convert(node: ContentText): TextView =
        with(node) {
            TextView(
                text = text,
                dci = dci,
                sourceSets = sourceSets,
                style = style,
                extra = convertExtra(extra)
            )
        }

    open fun convert(node: ContentBreakLine): BreakLineView =
        with(node) {
            BreakLineView(
                dci = dci,
                sourceSets = sourceSets,
                style = style,
                extra = convertExtra(extra)
            )
        }

    open fun convert(node: ContentHeader): HeaderView =
        with(node) {
            HeaderView(
                level = level,
                dci = dci,
                sourceSets = sourceSets,
                style = style,
                extra = convertExtra(extra),
                children = children.map { convert(it) }
            )
        }

    open fun convert(node: ContentCode): CodeView =
        when (node) {
            is ContentCodeBlock -> CodeView(
                language = node.language,
                dci = node.dci,
                style = node.style + TextStyle.Block,
                children = node.children.map { convert(it) },
                sourceSets = node.sourceSets,
                extra = convertExtra(node.extra)
            )
            is ContentCodeInline -> CodeView(
                language = node.language,
                dci = node.dci,
                style = node.style,
                children = node.children.map { convert(it) },
                sourceSets = node.sourceSets,
                extra = convertExtra(node.extra)
            )
            else -> throw IllegalStateException("Cant convert unknown ContentCode: ${node.javaClass.canonicalName}")
        }

    open fun convert(node: ContentResolvedLink): ResolvedLinkView =
        with(node) {
            ResolvedLinkView(
                address = address,
                dci = dci,
                style = style,
                children = children.map { convert(it) },
                sourceSets = sourceSets,
                extra = convertExtra(extra)
            )
        }

    open fun convert(node: ContentDRILink): UnresolvedLinkView =
        with(node) {
            UnresolvedLinkView(
                address = address,
                dci = dci,
                style = style,
                children = children.map { convert(it) },
                sourceSets = sourceSets,
                extra = convertExtra(extra)
            )
        }

    open fun convert(node: ContentTable): TableView =
        with(node) {
            TableView(
                header = header.map { convert(it) },
                dci = dci,
                style = style,
                children = children.map { convert(it) },
                sourceSets = sourceSets,
                extra = convertExtra(extra)
            )
        }

    open fun convert(node: ContentList): ListView =
        with(node) {
            ListView(
                ordered = ordered,
                dci = dci,
                style = style,
                children = children.map { convert(it) },
                sourceSets = sourceSets,
                extra = convertExtra(extra)
            )
        }

    open fun convert(node: ContentGroup): GroupView =
        with(node) {
            GroupView(
                dci = dci,
                style = style,
                children = children.map { convert(it) },
                sourceSets = sourceSets,
                extra = convertExtra(extra)
            )
        }

    open fun convert(node: ContentDivergentGroup): DivergentGroupView =
        with(node) {
            DivergentGroupView(
                groupID = groupID,
                implicitlySourceSetHinted = implicitlySourceSetHinted,
                dci = dci,
                style = style,
                children = children.map { convert(it) },
                sourceSets = sourceSets,
                extra = convertExtra(extra)
            )
        }

    open fun convert(node: ContentDivergentInstance): DivergentInstanceView =
        with(node) {
            DivergentInstanceView(
                before = before?.let { convert(it) },
                divergent = convert(divergent),
                after = after?.let { convert(it) },
                dci = dci,
                style = style,
                sourceSets = sourceSets,
                extra = convertExtra(extra)
            )
        }

    open fun convert(node: PlatformHintedContent): PlatformHintedContentView =
        with(node) {
            PlatformHintedContentView(
                sourceSets = sourceSets,
                inner = convert(inner)
            )
        }

    //TODO
    open fun convertExtra(extra: PropertyContainer<ContentNode>): PropertyContainer<Content> =
        PropertyContainer.empty()
}