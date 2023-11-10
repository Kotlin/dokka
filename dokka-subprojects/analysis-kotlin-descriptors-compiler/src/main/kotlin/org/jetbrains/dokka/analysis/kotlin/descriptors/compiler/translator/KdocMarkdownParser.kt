/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.translator

import com.intellij.psi.PsiElement
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser
import org.jetbrains.dokka.analysis.markdown.jb.MarkdownParser.Companion.fqDeclarationName
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.doc.Suppress
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag

internal fun parseFromKDocTag(
    kDocTag: KDocTag?,
    externalDri: (String) -> DRI?,
    kdocLocation: String?,
    parseWithChildren: Boolean = true
): DocumentationNode {
    return if (kDocTag == null) {
        DocumentationNode(emptyList())
    } else {
        fun parseStringToDocNode(text: String) =
            MarkdownParser(externalDri, kdocLocation).parseStringToDocNode(text)

        fun pointedLink(tag: KDocTag): DRI? = (parseStringToDocNode("[${tag.getSubjectName()}]")).let {
            val link = it.children[0].children[0]
            if (link is DocumentationLink) link.dri else null
        }

        val allTags =
            listOf(kDocTag) + if (kDocTag.canHaveParent() && parseWithChildren) getAllKDocTags(findParent(kDocTag)) else emptyList()
        DocumentationNode(
            allTags.map {
                when (it.knownTag) {
                    null -> if (it.name == null) Description(parseStringToDocNode(it.getContent())) else CustomTagWrapper(
                        parseStringToDocNode(it.getContent()),
                        it.name!!
                    )
                    KDocKnownTag.AUTHOR -> Author(parseStringToDocNode(it.getContent()))
                    KDocKnownTag.THROWS -> {
                        val dri = pointedLink(it)
                        Throws(
                            parseStringToDocNode(it.getContent()),
                            dri?.fqDeclarationName() ?: it.getSubjectName().orEmpty(),
                            dri,
                        )
                    }
                    KDocKnownTag.EXCEPTION -> {
                        val dri = pointedLink(it)
                        Throws(
                            parseStringToDocNode(it.getContent()),
                            dri?.fqDeclarationName() ?: it.getSubjectName().orEmpty(),
                            dri
                        )
                    }
                    KDocKnownTag.PARAM -> Param(
                        parseStringToDocNode(it.getContent()),
                        it.getSubjectName().orEmpty()
                    )
                    KDocKnownTag.RECEIVER -> Receiver(parseStringToDocNode(it.getContent()))
                    KDocKnownTag.RETURN -> Return(parseStringToDocNode(it.getContent()))
                    KDocKnownTag.SEE -> {
                        val dri = pointedLink(it)
                        See(
                            parseStringToDocNode(it.getContent()),
                            dri?.fqDeclarationName() ?: it.getSubjectName().orEmpty(),
                            dri,
                        )
                    }
                    KDocKnownTag.SINCE -> Since(parseStringToDocNode(it.getContent()))
                    KDocKnownTag.CONSTRUCTOR -> Constructor(parseStringToDocNode(it.getContent()))
                    KDocKnownTag.PROPERTY -> Property(
                        parseStringToDocNode(it.getContent()),
                        it.getSubjectName().orEmpty()
                    )
                    KDocKnownTag.SAMPLE -> Sample(
                        parseStringToDocNode(it.getContent()),
                        it.getSubjectName().orEmpty()
                    )
                    KDocKnownTag.SUPPRESS -> Suppress(parseStringToDocNode(it.getContent()))
                }
            }
        )
    }
}

//Horrible hack but since link resolution is passed as a function i am not able to resolve them otherwise
@kotlin.Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("This function makes wrong assumptions and is missing a lot of corner cases related to generics, " +
        "parameters and static members. This is not supposed to be public API and will not be supported in the future")
internal fun DRI.fqName(): String? = "$packageName.$classNames".takeIf { packageName != null && classNames != null }

private fun findParent(kDoc: PsiElement): PsiElement =
    if (kDoc.canHaveParent()) findParent(kDoc.parent) else kDoc

private fun PsiElement.canHaveParent(): Boolean = this is KDocSection && knownTag != KDocKnownTag.PROPERTY

private fun getAllKDocTags(kDocImpl: PsiElement): List<KDocTag> =
    kDocImpl.children.filterIsInstance<KDocTag>().filterNot { it is KDocSection } + kDocImpl.children.flatMap {
        getAllKDocTags(it)
    }
