package org.jetbrains.dokka

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import java.io.File

public class PackageDocs(val documentationBuilder: DocumentationBuilder,
                         val linkResolveContext: DeclarationDescriptor?,
                         val logger: DokkaLogger) {
    public val moduleContent: MutableContent = MutableContent()
    private val _packageContent: MutableMap<String, MutableContent> = hashMapOf()
    public val packageContent: Map<String, Content>
        get() = _packageContent

    fun parse(path: String) {
        val file = File(path)
        if (file.exists()) {
            val text = file.readText()
            val tree = parseMarkdown(text)
            var targetContent: MutableContent = moduleContent
            tree.children.forEach {
                if (it.type == MarkdownElementTypes.ATX_1) {
                    val headingText = it.child(MarkdownTokenTypes.TEXT)?.text
                    if (headingText != null) {
                        targetContent = findTargetContent(headingText)
                    }
                } else {
                    buildContentTo(it, targetContent, { resolveContentLink(it) })
                }
            }
        } else {
            logger.warn("Include file $file was not found.")
        }
    }

    private fun findTargetContent(heading: String): MutableContent {
        if (heading.startsWith("Module") || heading.startsWith("module")) {
            return moduleContent
        }
        if (heading.startsWith("Package") || heading.startsWith("package")) {
            return findOrCreatePackageContent(heading.substring("package".length()).trim())
        }
        return findOrCreatePackageContent(heading)
    }

    private fun findOrCreatePackageContent(packageName: String) =
        _packageContent.getOrPut(packageName) { -> MutableContent() }

    private fun resolveContentLink(href: String): ContentBlock {
        if (linkResolveContext != null) {
            return documentationBuilder.resolveContentLink(linkResolveContext, href)
        }
        return ContentExternalLink("#")
    }
}