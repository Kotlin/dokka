package org.jetbrains.dokka.base.transformers.pages.annotations

import org.jetbrains.dokka.base.renderers.platforms
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer

class DeprecatedStrikethroughTransformer(val context: DokkaContext) : PageTransformer {
    override fun invoke(input: RootPageNode): RootPageNode = input.transformContentPagesTree { contentPage ->
        contentPage.platforms().fold(contentPage) { acc, sourceSetData ->
            if (acc.documentable?.isDeprecated(sourceSetData) == true || acc.documentable?.hasDeprecatedChildren(
                    sourceSetData
                ) == true
            ) {
                val deprecatedDRIs =
                    if (acc.documentable?.isDeprecated(sourceSetData) == true) contentPage.dri else emptySet<DRI>() +
                            contentPage.documentable?.children
                                ?.filter { it.isDeprecated(sourceSetData) }
                                ?.map { it.dri }
                                ?.toSet().orEmpty()

                acc.modified(content = acc.content.addStrikethroughToSignature(deprecatedDRIs))
            } else {
                acc
            }
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

    private fun Documentable.isDeprecated(sourceSetData: SourceSetData) = when (this) {
        is DClass -> this.isKotlinOrJavaDeprecated(sourceSetData)
        is DAnnotation -> this.isKotlinOrJavaDeprecated(sourceSetData)
        is DObject -> this.isKotlinOrJavaDeprecated(sourceSetData)
        is DInterface -> this.isKotlinOrJavaDeprecated(sourceSetData)
        is DEnum -> this.isKotlinOrJavaDeprecated(sourceSetData)
        is DFunction -> this.isKotlinOrJavaDeprecated(sourceSetData)
        is DProperty -> this.isKotlinOrJavaDeprecated(sourceSetData)
        is DEnumEntry -> this.isKotlinOrJavaDeprecated(sourceSetData)

        else -> false
    }

    private fun Documentable.hasDeprecatedChildren(sourceSetData: SourceSetData) =
        children.any { it.isDeprecated(sourceSetData) }

    private fun <T : Documentable> WithExtraProperties<T>.isKotlinOrJavaDeprecated(sourceSetData: SourceSetData) =
        extra[Annotations]?.content?.get(sourceSetData)?.any {
            it.dri == DRI("kotlin", "Deprecated")
                    || it.dri == DRI("java.lang", "Deprecated")
        } == true
}