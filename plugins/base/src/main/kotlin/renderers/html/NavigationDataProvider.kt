package org.jetbrains.dokka.base.renderers.html

import org.jetbrains.dokka.analysis.PsiDocumentableSource
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.transformers.documentables.isException
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

open class NavigationPageInstaller(val context: DokkaContext) : NavigationDataProvider(), PageTransformer {

    override fun invoke(input: RootPageNode): RootPageNode =
        input.modified(
            children = input.children + NavigationPage(
                root = navigableChildren(input),
                moduleName = context.configuration.moduleName,
                context = context
            )
        )
}

abstract class NavigationDataProvider {
    open fun navigableChildren(input: RootPageNode): NavigationNode = input.withDescendants()
        .first { it is ModulePage || it is MultimoduleRootPage }.let { visit(it as ContentPage) }

    open fun visit(page: ContentPage): NavigationNode =
        NavigationNode(
            name = page.displayableName(),
            dri = page.dri.first(),
            sourceSets = page.sourceSets(),
            icon = chooseNavigationIcon(page),
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

    private fun chooseNavigationIcon(contentPage: ContentPage): NavigationNodeIcon? {
        return if (contentPage is WithDocumentables) {
            val documentable = contentPage.documentables.firstOrNull()
            val isJava = (documentable as? WithSources)?.sources?.any { it.value is PsiDocumentableSource } ?: false

            when (documentable) {
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
                is DEnum -> if (isJava) NavigationNodeIcon.ENUM_CLASS else NavigationNodeIcon.ENUM_CLASS_KT
                is DAnnotation -> {
                    if (isJava) NavigationNodeIcon.ANNOTATION_CLASS else NavigationNodeIcon.ANNOTATION_CLASS_KT
                }
                is DObject -> NavigationNodeIcon.OBJECT
                else -> null
            }
        } else {
            null
        }
    }

    private fun DClass.isAbstract(): Boolean {
        return modifier.values.all { it is KotlinModifier.Abstract || it is JavaModifier.Abstract }
    }

    private fun ContentPage.navigableChildren(): List<NavigationNode> {
        return if (this !is ClasslikePageNode) {
            children
                .filterIsInstance<ContentPage>()
                .map { visit(it) }
                .sortedBy { it.name.toLowerCase() }
        } else {
            emptyList()
        }
    }
}
