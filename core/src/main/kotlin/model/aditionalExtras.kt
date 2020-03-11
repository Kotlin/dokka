package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

class AdditionalModifiers(val content: Set<ExtraModifiers>) : ExtraProperty<Documentable> {
    object AdditionalKey : ExtraProperty.Key<Documentable, AdditionalModifiers> {
        override fun mergeStrategyFor(
            left: AdditionalModifiers,
            right: AdditionalModifiers
        ): MergeStrategy<Documentable> = MergeStrategy.Replace(AdditionalModifiers(left.content + right.content))
    }

    override fun equals(other: Any?): Boolean = if (other is AdditionalModifiers) other.content == content else false
    override fun hashCode() = content.hashCode()
    override val key: ExtraProperty.Key<Documentable, *> = AdditionalKey
}

class Annotations(val content: List<Annotation>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, Annotations>

    override val key: ExtraProperty.Key<Documentable, *> = Annotations

    data class Annotation(val dri: DRI, val params: Map<String, String>)
}