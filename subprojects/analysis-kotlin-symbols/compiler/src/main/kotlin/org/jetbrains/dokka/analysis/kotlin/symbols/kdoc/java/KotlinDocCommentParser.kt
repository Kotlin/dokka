package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.java

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.java.doccomment.DocComment
import org.jetbrains.dokka.analysis.java.parsers.DocCommentParser
import org.jetbrains.dokka.analysis.kotlin.symbols.compiler.SymbolsAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.symbols.compiler.getPsiFilesFromPaths
import org.jetbrains.dokka.analysis.kotlin.symbols.compiler.getSourceFilePaths
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.parseFromKDocTag
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.resolveKDocLink
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromSymbol
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtFile

internal class KotlinDocCommentParser(
    private val context: DokkaContext
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
        val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }
        val someKtFile = getPsiFilesFromPaths<KtFile>(
            kotlinAnalysis[sourceSet].project,
            getSourceFilePaths(sourceSet.sourceRoots.map { it.canonicalPath })
        ).firstOrNull() ?: throw IllegalStateException()

            analyze(someKtFile) {
                return parseFromKDocTag(
                    kDocTag = element.comment,
                    externalDri = { link: String ->
                        val linkedSymbol = resolveKDocLink(link, element.resolveDocContext.ktElement)
                        if (linkedSymbol == null) null
                        else getDRIFromSymbol(linkedSymbol)
                    },
                    kdocLocation = null,
                    parseWithChildren = parseWithChildren
                )
            }
    }
}

