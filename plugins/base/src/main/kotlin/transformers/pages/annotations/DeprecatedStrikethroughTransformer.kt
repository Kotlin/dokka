package org.jetbrains.dokka.base.transformers.pages.annotations

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Annotation
import org.jetbrains.dokka.model.Enum
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

class DeprecatedStrikethroughTransformer(val context: DokkaContext) : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode = input.transformContentPagesTree { contentPage ->
        if (contentPage.documentable?.isDeprecated() == true || contentPage.documentable?.hasDeprecatedChildren() == true) {
            val deprecatedDRIs =
                contentPage.dri +
                        contentPage.documentable?.children
                            ?.filter { it.isDeprecated() }
                            ?.map { it.dri }
                            ?.toSet().orEmpty()

            contentPage.modified(content = contentPage.content.addStrikethroughToSignature(deprecatedDRIs))
        } else {
            contentPage
        }
    }

    private fun ContentNode.addStrikethroughToSignature(deprecatedDRIs: Set<DRI>): ContentNode = when (this) {
        is ContentGroup -> if (dci.kind == ContentKind.Symbol && deprecatedDRIs.containsAll(dci.dri)) {
            copy(style = this.style + setOf(TextStyle.Strikethrough))
        } else {
            copy(children = children.map { it.addStrikethroughToSignature(deprecatedDRIs) })
        }
        is ContentTable -> copy(children = children.map { it.addStrikethroughToSignature(deprecatedDRIs) as ContentGroup })
        is PlatformHintedContent -> copy(inner = inner.addStrikethroughToSignature(deprecatedDRIs))
        else -> this
    }

    private fun Documentable.isDeprecated(): Boolean = when (this) {
        is Class -> this.isKotlinOrJavaDeprecated()
        is Annotation -> this.isKotlinOrJavaDeprecated()
        is Object -> this.isKotlinOrJavaDeprecated()
        is Interface -> this.isKotlinOrJavaDeprecated()
        is Enum -> this.isKotlinOrJavaDeprecated()
        is Function -> this.isKotlinOrJavaDeprecated()
        is Property -> this.isKotlinOrJavaDeprecated()
        is EnumEntry -> this.isKotlinOrJavaDeprecated()

        else -> false
    }

    private fun Documentable.hasDeprecatedChildren() = children.any { it.isDeprecated() }

    private fun <T : Documentable> WithExtraProperties<T>.isKotlinOrJavaDeprecated() =
        extra[Annotations]?.content?.any {
            it.dri == DRI("kotlin", "Deprecated")
                    || it.dri == DRI("java.lang", "Deprecated")
        } == true
}