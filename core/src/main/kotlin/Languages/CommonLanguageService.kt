package org.jetbrains.dokka


abstract class CommonLanguageService : LanguageService {

    protected fun ContentBlock.renderPackage(node: DocumentationNode) {
        keyword("package")
        nbsp()
        identifier(node.name)
    }

    override fun renderName(node: DocumentationNode): String {
        return when (node.kind) {
            NodeKind.Constructor -> node.owner!!.name
            else -> node.name
        }
    }

    open fun renderModifier(
        block: ContentBlock,
        node: DocumentationNode,
        renderMode: LanguageService.RenderMode,
        nowrap: Boolean = false
    ) = with(block) {
        keyword(node.name)
        if (nowrap) {
            nbsp()
        } else {
            text(" ")
        }
    }

    protected fun renderLinked(
        block: ContentBlock,
        node: DocumentationNode,
        body: ContentBlock.(DocumentationNode) -> Unit
    ) = with(block) {
        val to = node.links.firstOrNull()
        if (to == null)
            body(node)
        else
            link(to) {
                this.body(node)
            }
    }

    protected fun <T> ContentBlock.renderList(
        nodes: List<T>, separator: String = ", ",
        noWrap: Boolean = false, renderItem: (T) -> Unit
    ) {
        if (nodes.none())
            return
        renderItem(nodes.first())
        nodes.drop(1).forEach {
            if (noWrap) {
                symbol(separator.removeSuffix(" "))
                nbsp()
            } else {
                symbol(separator)
            }
            renderItem(it)
        }
    }

    abstract fun showModifierInSummary(node: DocumentationNode): Boolean

    protected fun ContentBlock.renderModifiersForNode(
        node: DocumentationNode,
        renderMode: LanguageService.RenderMode,
        nowrap: Boolean = false
    ) {
        val modifiers = node.details(NodeKind.Modifier)
        for (it in modifiers) {
            if (node.kind == NodeKind.Interface && it.name == "abstract")
                continue
            if (renderMode == LanguageService.RenderMode.SUMMARY && !showModifierInSummary(it)) {
                continue
            }
            renderModifier(this, it, renderMode, nowrap)
        }
    }


}