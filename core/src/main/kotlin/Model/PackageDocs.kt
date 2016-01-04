package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.Singleton
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyPackageDescriptor
import java.io.File

@Singleton
class PackageDocs
        @Inject constructor(val linkResolver: DeclarationLinkResolver?,
                            val logger: DokkaLogger)
{
    val moduleContent: MutableContent = MutableContent()
    private val _packageContent: MutableMap<String, MutableContent> = hashMapOf()
    val packageContent: Map<String, Content>
        get() = _packageContent

    fun parse(fileName: String, linkResolveContext: LazyPackageDescriptor?) {
        val file = File(fileName)
        if (file.exists()) {
            val text = file.readText()
            val tree = parseMarkdown(text)
            var targetContent: MutableContent = moduleContent
            tree.children.forEach {
                if (it.type == MarkdownElementTypes.ATX_1) {
                    val headingText = it.child(MarkdownTokenTypes.ATX_CONTENT)?.text
                    if (headingText != null) {
                        targetContent = findTargetContent(headingText.trimStart())
                    }
                } else {
                    buildContentTo(it, targetContent, { resolveContentLink(it, linkResolveContext) })
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
            return findOrCreatePackageContent(heading.substring("package".length).trim())
        }
        return findOrCreatePackageContent(heading)
    }

    private fun findOrCreatePackageContent(packageName: String) =
        _packageContent.getOrPut(packageName) { -> MutableContent() }

    private fun resolveContentLink(href: String, linkResolveContext: LazyPackageDescriptor?): ContentBlock {
        if (linkResolveContext != null && linkResolver != null) {
            return linkResolver.resolveContentLink(linkResolveContext, href)
        }
        return ContentExternalLink("#")
    }
}