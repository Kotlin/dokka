package org.jetbrains.dokka.base.transformers.pages.annotations

import org.jetbrains.dokka.base.renderers.platforms
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

class DeprecatedStrikethroughTransformer(val context: DokkaContext) : PageTransformer {

    override fun invoke(input: RootPageNode): RootPageNode = input.transformContentPagesTree { contentPage ->
        contentPage.platforms().fold(contentPage) { acc, sourceSetData ->
            listOfNotNull(
                contentPage.documentable?.children?.filter { it.isDeprecated(sourceSetData) }?.map { it.dri },
                contentPage.dri.takeIf { acc.documentable?.isDeprecated(sourceSetData) == true }?.toList()
            ).flatten().ifNotEmpty {
                acc.modified(content = acc.content.addStrikethroughToSignature(sourceSetData, this.toSet()))
            } ?: acc
        }
    }

    private fun ContentNode.addStrikethroughToSignature(
        sourceSetData: SourceSetData,
        deprecatedDRIs: Set<DRI>
    ): ContentNode = when (this) {
        is ContentGroup -> if (setOf(sourceSetData) == sourceSets && deprecatedDRIs.containsAll(dci.dri)) {
            copy(style = this.style + setOf(TextStyle.Strikethrough))
        } else {
            copy(children = children.map { it.addStrikethroughToSignature(sourceSetData, deprecatedDRIs) })
        }
        is ContentTable -> copy(children = children.map {
            it.addStrikethroughToSignature(
                sourceSetData,
                deprecatedDRIs
            ) as ContentGroup
        })
        is PlatformHintedContent -> copy(inner = inner.addStrikethroughToSignature(sourceSetData, deprecatedDRIs))
        is ContentDivergentGroup -> copy(children = children.map { it.addStrikethroughToSignature(sourceSetData, deprecatedDRIs) } as List<ContentDivergentInstance>)
        is ContentDivergentInstance -> copy(divergent = divergent.addStrikethroughToSignature(sourceSetData, deprecatedDRIs))
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