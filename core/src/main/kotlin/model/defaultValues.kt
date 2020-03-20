package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy
import org.jetbrains.dokka.pages.PlatformData
import java.lang.IllegalStateException

class DefaultValue(val values: PlatformDependent<String>) : ExtraProperty<DParameter> {
    constructor(value: String, pd: PlatformData) : this(PlatformDependent.from(pd, value))

    companion object : ExtraProperty.Key<DParameter, DefaultValue> {
        override fun mergeStrategyFor(left: DefaultValue, right: DefaultValue): MergeStrategy<DParameter> =
            MergeStrategy.Replace(DefaultValue(left.values.merge(right.values)))

        fun <T> PlatformDependent<T>.merge(other: PlatformDependent<T>) = PlatformDependent(
            (map.entries.toList() + other.map.entries.toList()).groupBy({ it.key }) { it.value }.map { (k, v) ->
                if (v.size != 1) throw IllegalStateException("Expected up to one value per platform")
                k to v.single()
            }.toMap()
        )
    }

    override val key: ExtraProperty.Key<DParameter, *>
        get() = Companion
}