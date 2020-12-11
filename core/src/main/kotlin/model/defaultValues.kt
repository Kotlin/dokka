package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy
import org.jetbrains.dokka.model.properties.WithExtraProperties

class DefaultValue(val value: String): ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, DefaultValue> {
        override fun mergeStrategyFor(left: DefaultValue, right: DefaultValue): MergeStrategy<Documentable> = MergeStrategy.Remove // TODO pass a logger somehow and log this
    }

    override val key: ExtraProperty.Key<Documentable, *>
        get() = Companion
}

val WithExtraProperties<out Documentable>.hasDefaultValue: Boolean
    get() = extra[DefaultValue] != null