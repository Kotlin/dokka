package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy
import java.lang.IllegalStateException

class DefaultValue(val value: String): ExtraProperty<DParameter> {
    companion object : ExtraProperty.Key<DParameter, DefaultValue>

    override val key: ExtraProperty.Key<DParameter, *>
        get() = Companion
}