package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy
import java.lang.IllegalStateException

class DefaultValue(val value: String): ExtraProperty<DParameter> {
    companion object : ExtraProperty.Key<DParameter, DefaultValue> {
        override fun mergeStrategyFor(left: DefaultValue, right: DefaultValue): MergeStrategy<DParameter> = if (left.value == right.value)
            MergeStrategy.Replace(left)
        else
            MergeStrategy.Fail {throw IllegalStateException("Default values need to be the same")}

    }

    override val key: ExtraProperty.Key<DParameter, *>
        get() = Companion
}