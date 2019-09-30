package org.jetbrains.dokka

import com.google.inject.Inject
import com.google.inject.Singleton
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.LocalTimeCounter
import com.intellij.openapi.util.text.StringUtil
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.parser.LinkMap
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import java.io.File

@Singleton
class PackageDocs
        @Inject constructor(val linkResolver: DeclarationLinkResolver?,
                            val logger: DokkaLogger,
                            val environment: KotlinCoreEnvironment,
                            val refGraph: NodeReferenceGraph,
                            val elementSignatureProvider: ElementSignatureProvider)
{
    val moduleContent: MutableContent = MutableContent()
    private val _packageContent: MutableMap<String, MutableContent> = hashMapOf()
    val packageContent: Map<String, Content>
        get() = _packageContent

    fun parse(fileName: String, linkResolveContext: List<PackageFragmentDescriptor>) {
        val file = File(fileName)
        if (file.exists()) {
            val text = StringUtil.convertLineSeparators(file.readText())
            val tree = parseMarkdown(text)
            val linkMap = LinkMap.buildLinkMap(tree.node, text)
            var targetContent: MutableContent = moduleContent
            tree.children.forEach {
                if (it.type == MarkdownElementTypes.ATX_1) {
                    val headingText = it.child(MarkdownTokenTypes.ATX_CONTENT)?.text
                    if (headingText != null) {
                        targetContent = findTargetContent(headingText.trimStart())
                    }
                } else {
                    buildContentTo(it, targetContent, LinkResolver(linkMap) { resolveContentLink(fileName, it, linkResolveContext) })
                }
            }
        } else {
            logger.warn("Include file $file was not found.")
        }
    }

    private fun parseHtmlAsJavadoc(text: String, packageName: String, file: File) {
        val javadocText = text
                .replace("*/", "*&#47;")
                .removeSurrounding("<html>", "</html>", true).trim()
                .removeSurrounding("<body>", "</body>", true)
                .lineSequence()
                .map { "* $it" }
                .joinToString (separator = "\n", prefix = "/**\n", postfix = "\n*/")
        parseJavadoc(javadocText, packageName, file)
    }

    private fun CharSequence.removeSurrounding(prefix: CharSequence, suffix: CharSequence, ignoringCase: Boolean = false): CharSequence {
        if ((length >= prefix.length + suffix.length) && startsWith(prefix, ignoringCase) && endsWith(suffix, ignoringCase)) {
            return subSequence(prefix.length, length - suffix.length)
        }
        return subSequence(0, length)
    }


    private fun parseJavadoc(text: String, packageName: String, file: File) {

        val psiFileFactory = PsiFileFactory.getInstance(environment.project)
        val psiFile = psiFileFactory.createFileFromText(
                file.nameWithoutExtension + ".java",
                JavaFileType.INSTANCE,
                "package $packageName; $text\npublic class C {}",
                LocalTimeCounter.currentTime(),
                false,
                true
        )

        val psiClass = PsiTreeUtil.getChildOfType(psiFile, PsiClass::class.java)!!
        val parser = JavadocParser(refGraph, logger, elementSignatureProvider, linkResolver?.externalDocumentationLinkResolver!!)
        findOrCreatePackageContent(packageName).apply {
            val content = parser.parseDocumentation(psiClass).content
            children.addAll(content.children)
            content.sections.forEach {
                addSection(it.tag, it.subjectName).children.addAll(it.children)
            }
        }
    }


    fun parseJava(fileName: String, packageName: String) {
        val file = File(fileName)
        if (file.exists()) {
            val text = file.readText()

            val trimmedText = text.trim()

            if (trimmedText.startsWith("/**")) {
                parseJavadoc(text, packageName, file)
            } else if (trimmedText.toLowerCase().startsWith("<html>")) {
                parseHtmlAsJavadoc(trimmedText, packageName, file)
            }
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
        _packageContent.getOrPut(packageName) { MutableContent() }

    private fun resolveContentLink(fileName: String, href: String, linkResolveContext: List<PackageFragmentDescriptor>): ContentBlock {
        if (linkResolver != null) {
            linkResolveContext
                    .asSequence()
                    .map { p -> linkResolver.tryResolveContentLink(p, href) }
                    .filterNotNull()
                    .firstOrNull()
                    ?.let { return it }
        }
        logger.warn("Unresolved link to `$href` in include ($fileName)")
        return ContentExternalLink("#")
    }
}