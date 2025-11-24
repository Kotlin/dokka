/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.generation.kdp

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.ExperimentalDokkaApi
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.kotlin.documentation.KdDocumentationNode
import org.jetbrains.kotlin.documentation.KdLinkReference

internal fun Documentable.tagWrappers(
    sourceSet: DokkaConfiguration.DokkaSourceSet,
    requirement: (TagWrapper) -> Boolean
): List<TagWrapper> {
    val docs = documentation[sourceSet]?.children.orEmpty()
    val filtered = docs.filterNot(requirement)
    require(filtered.isEmpty()) { "Documentation contains wrong nodes: $filtered" }
    return docs
}

internal fun TagWrapper?.toKdDocumentation(): List<KdDocumentationNode> {
//    return emptyList()
    if (this == null) return emptyList()
    val root = requireNotNull(root as? CustomDocTag) { "Only CustomDocTag is supported: $this" }
    require(root.name == "MARKDOWN_FILE") { "Only MARKDOWN_FILE is supported" }
    require(root.params.isEmpty()) { "Params are not supported: $root.params in $this" }

    return children.flatMap(DocTag::toKdDocumentationNode)
}

/**
 * Based on [org.jetbrains.dokka.base.transformers.pages.comments.DocTagToContentConverter]
 */
private fun DocTag.toKdDocumentationNode(): List<KdDocumentationNode> = when (this) {
    is CustomDocTag -> error("SHOULD NOT HAPPEN!")

    // containers
    is P, is Li, is Td -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        listOf(KdDocumentationNode.Paragraph(children.flatMap(DocTag::toKdDocumentationNode)))
    }

    is BlockQuote -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        listOf(KdDocumentationNode.BlockQuote(children.flatMap(DocTag::toKdDocumentationNode)))
    }

    is H1 -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        listOf(KdDocumentationNode.Header(1, children.flatMap(DocTag::toKdDocumentationNode)))
    }

    is H2 -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        listOf(KdDocumentationNode.Header(2, children.flatMap(DocTag::toKdDocumentationNode)))
    }

    is H3 -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        listOf(KdDocumentationNode.Header(3, children.flatMap(DocTag::toKdDocumentationNode)))
    }

    is H4 -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        listOf(KdDocumentationNode.Header(4, children.flatMap(DocTag::toKdDocumentationNode)))
    }

    is H5 -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        listOf(KdDocumentationNode.Header(5, children.flatMap(DocTag::toKdDocumentationNode)))
    }

    is H6 -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        listOf(KdDocumentationNode.Header(6, children.flatMap(DocTag::toKdDocumentationNode)))
    }

    // lists

    // TODO: `li` creates an additional nesting... is it ok?
    is Ul -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        listOf(KdDocumentationNode.BulletList(children.flatMap(DocTag::toKdDocumentationNode)))
    }

    is Ol -> listOf(
        KdDocumentationNode.OrderedList(
            startIndex = params["start"]?.toInt() ?: 1,
            items = children.flatMap(DocTag::toKdDocumentationNode)
        )
    )

    is Br -> error("should not happen")

    // TODO: RECHECK THIS!!!
    is Table -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        require(children.all { it is Th || it is Tr }) {
            "Children are not supported: ${children.map { it::class.simpleName }.distinct()}"
        }
        listOf(
            KdDocumentationNode.Table(
                headers = children.filterIsInstance<Th>().singleOrNullIfEmpty()?.children?.map {
                    require(it is Td)
                    KdDocumentationNode.Paragraph(it.children.flatMap { it.toKdDocumentationNode() })
                },
                rows = children.filterIsInstance<Tr>().map {
                    it.children.map {
                        require(it is Td)
                        KdDocumentationNode.Paragraph(it.children.flatMap { it.toKdDocumentationNode() })
                    }
                }
            )
        )
    }

    // code

    is Pre, is CodeBlock -> listOf(
        KdDocumentationNode.CodeBlock(
            lines = buildString {
                children.forEach {
                    when (it) {
                        is Text -> append(it.body)
                        is Br -> appendLine()
                        else -> error("should not happen: $it")
                    }
                }
            }.split('\n'),
            language = params["lang"] ?: "kotlin"
        )
    )

    // explicitly unsupported
    is Dl, is Dt, is Dd -> listOf(KdDocumentationNode.Text("UNKNOWN: $this"))

    is Text if params["content-type"] == "html" -> listOf(KdDocumentationNode.Html(body))
    else -> toKdDocumentationText()
}

private fun DocTag.toKdDocumentationText(
    styles: Set<KdDocumentationNode.Text.Style> = emptySet()
): List<KdDocumentationNode> = when (this) {
    is B -> children.flatMap {
        it.toKdDocumentationText(styles + KdDocumentationNode.Text.Style.Strong)
    }

    is Strikethrough -> children.flatMap {
        it.toKdDocumentationText(styles + KdDocumentationNode.Text.Style.Strikethrough)
    }

    is I -> children.flatMap {
        it.toKdDocumentationText(styles + KdDocumentationNode.Text.Style.Italic)
    }

    is Text -> {
        require(params.isEmpty()) { "Params are not supported: $params in $this" }
        require(children.isEmpty()) { "Children are not supported: $children in $this" }
        listOf(
            KdDocumentationNode.Text(body, styles)
        )
    }

    is CodeInline -> listOf(
        KdDocumentationNode.CodeInline(
            text = buildString {
                children.forEach {
                    when (it) {
                        is Text -> append(it.body)
                        else -> error("WTF?")
                    }
                }
            },
            language = params["lang"] ?: "kotlin",
            styles = styles
        )
    )

    is A -> listOf(
        KdDocumentationNode.ExternalLink(
            label = children.flatMap(DocTag::toKdDocumentationNode),
            url = params.getValue("href"),
            styles = styles
        )
    )

    is DocumentationLink -> listOf(
        KdDocumentationNode.Link(
            label = children.flatMap(DocTag::toKdDocumentationNode),
            reference = dri.toKdLinkReference(),
            styles = styles
        )
    )

    else -> listOf(
        KdDocumentationNode.Text("UNKNOWN: $this", styles)
    )
}

@OptIn(ExperimentalDokkaApi::class)
private fun DRI.toKdLinkReference(): KdLinkReference {
    return when (val target = target) {
        is PointingToCallableParameters -> KdLinkReference.ValueParameter(target.parameterIndex)
        is PointingToContextParameters -> KdLinkReference.ContextParameter(target.parameterIndex)
        is PointingToGenericParameters -> KdLinkReference.TypeParameter(target.parameterIndex)
        PointingToDeclaration -> when (callable) {
            null if classNames == null -> KdLinkReference.Package(packageName ?: "UNKNOWN_PACKAGE_NAME")
            null -> KdLinkReference.Classifier(toKdClassifierId())
            else -> KdLinkReference.Callable(toKdCallableId())
        }
    }
}
