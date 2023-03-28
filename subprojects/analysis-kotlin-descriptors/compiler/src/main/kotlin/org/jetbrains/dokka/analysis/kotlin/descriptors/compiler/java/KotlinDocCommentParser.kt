package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.java

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.java.DocComment
import org.jetbrains.dokka.analysis.java.DocCommentParser
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.CompilerDescriptorAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.from
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.translator.parseFromKDocTag
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.utilities.DokkaLogger

internal class KotlinDocCommentParser(
    private val context: DokkaContext,
    private val logger: DokkaLogger
) : DocCommentParser {

    override fun canParse(docComment: DocComment): Boolean {
        return docComment is KotlinDocComment
    }

    override fun parse(docComment: DocComment, context: PsiNamedElement): DocumentationNode {
        val kotlinDocComment = docComment as KotlinDocComment
        return parseDocumentation(kotlinDocComment)
    }

    fun parseDocumentation(element: KotlinDocComment, parseWithChildren: Boolean = true): DocumentationNode {
        val sourceSet = context.configuration.sourceSets.let { sourceSets ->
            sourceSets.firstOrNull { it.sourceSetID.sourceSetName == "jvmMain" }
                ?: sourceSets.first { it.analysisPlatform == Platform.jvm }
        }
        val kdocFinder = context.plugin<CompilerDescriptorAnalysisPlugin>().querySingle { kdocFinder }
        return parseFromKDocTag(
            kDocTag = element.comment,
            externalDri = { link: String ->
                try {
                    kdocFinder.resolveKDocLink(element.descriptor, link, sourceSet)
                        .firstOrNull()
                        ?.let { DRI.from(it) }
                } catch (e1: IllegalArgumentException) {
                    logger.warn("Couldn't resolve link for $link")
                    null
                }
            },
            kdocLocation = null,
            parseWithChildren = parseWithChildren
        )
    }
}

