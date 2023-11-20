/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.signatures.KotlinSignatureUtils.annotations
import org.jetbrains.dokka.base.transformers.documentables.isDeprecated
import org.jetbrains.dokka.base.transformers.documentables.isException
import org.jetbrains.dokka.base.utils.canonicalAlphabeticalOrder
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.analysis.kotlin.internal.DocumentableLanguage
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin

public abstract class NavigationDataProvider(
    dokkaContext: DokkaContext
) {
    private val documentableSourceLanguageParser = dokkaContext.plugin<InternalKotlinAnalysisPlugin>().querySingle { documentableSourceLanguageParser }

    public open fun navigableChildren(input: RootPageNode): NavigationNode = input.withDescendants()
        .first { it is ModulePage || it is MultimoduleRootPage }.let { visit(it as ContentPage) }

    public open fun visit(page: ContentPage): NavigationNode =
        NavigationNode(
            name = page.displayableName(),
            dri = page.dri.first(),
            sourceSets = page.sourceSets(),
            icon = chooseNavigationIcon(page),
            styles = chooseStyles(page),
            children = page.navigableChildren()
        )

    /**
     * Parenthesis is applied in 1 case:
     *  - page only contains functions (therefore documentable from this page is [DFunction])
     */
    private fun ContentPage.displayableName(): String =
        if (this is WithDocumentables && documentables.all { it is DFunction }) {
            "$name()"
        } else {
            name
        }

    private fun chooseNavigationIcon(contentPage: ContentPage): NavigationNodeIcon? =
        if (contentPage is WithDocumentables) {
            val documentable = contentPage.documentables.firstOrNull()
            val isJava = documentable?.hasAnyJavaSources() ?: false

            when (documentable) {
                is DTypeAlias -> NavigationNodeIcon.TYPEALIAS_KT
                is DClass -> when {
                    documentable.isException -> NavigationNodeIcon.EXCEPTION
                    documentable.isAbstract() -> {
                        if (isJava) NavigationNodeIcon.ABSTRACT_CLASS else NavigationNodeIcon.ABSTRACT_CLASS_KT
                    }
                    else -> if (isJava) NavigationNodeIcon.CLASS else NavigationNodeIcon.CLASS_KT
                }
                is DFunction -> NavigationNodeIcon.FUNCTION
                is DProperty -> {
                    val isVar = documentable.extra[IsVar] != null
                    if (isVar) NavigationNodeIcon.VAR else NavigationNodeIcon.VAL
                }
                is DInterface -> if (isJava) NavigationNodeIcon.INTERFACE else NavigationNodeIcon.INTERFACE_KT
                is DEnum,
                is DEnumEntry -> if (isJava) NavigationNodeIcon.ENUM_CLASS else NavigationNodeIcon.ENUM_CLASS_KT
                is DAnnotation -> {
                    if (isJava) NavigationNodeIcon.ANNOTATION_CLASS else NavigationNodeIcon.ANNOTATION_CLASS_KT
                }
                is DObject -> NavigationNodeIcon.OBJECT
                else -> null
            }
        } else {
            null
        }

    private fun Documentable.hasAnyJavaSources(): Boolean {
        return this.sourceSets.any { sourceSet ->
            documentableSourceLanguageParser.getLanguage(this, sourceSet) == DocumentableLanguage.JAVA
        }
    }

    private fun DClass.isAbstract() =
        modifier.values.all { it is KotlinModifier.Abstract || it is JavaModifier.Abstract }

    private fun chooseStyles(page: ContentPage): Set<Style> =
        if (page.containsOnlyDeprecatedDocumentables()) setOf(TextStyle.Strikethrough) else emptySet()

    private fun ContentPage.containsOnlyDeprecatedDocumentables(): Boolean {
        if (this !is WithDocumentables) {
            return false
        }
        return this.documentables.isNotEmpty() && this.documentables.all { it.isDeprecatedForAllSourceSets() }
    }

    private fun Documentable.isDeprecatedForAllSourceSets(): Boolean {
        val sourceSetAnnotations = this.annotations()
        return sourceSetAnnotations.isNotEmpty() && sourceSetAnnotations.all { (_, annotations) ->
            annotations.any { it.isDeprecated() }
        }
    }

    private val navigationNodeOrder: Comparator<NavigationNode> =
        compareBy(canonicalAlphabeticalOrder) { it.name }

    private fun ContentPage.navigableChildren() =
        if (this is ClasslikePage) {
            this.navigableChildren()
        } else {
            children
                .filterIsInstance<ContentPage>()
                .map { visit(it) }
                .sortedWith(navigationNodeOrder)
        }

    private fun ClasslikePage.navigableChildren(): List<NavigationNode> {
        // Classlikes should only have other classlikes as navigable children
        val navigableChildren = children
            .filterIsInstance<ClasslikePage>()
            .map { visit(it) }

        val isEnumPage = documentables.any { it is DEnum }
        return if (isEnumPage) {
            // no sorting for enum entries, should be the same order as in source code
            navigableChildren
        } else {
            navigableChildren.sortedWith(navigationNodeOrder)
        }
    }
}
