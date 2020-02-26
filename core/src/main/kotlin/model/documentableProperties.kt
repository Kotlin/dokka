package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

data class InheritedFunction(val isInherited: Boolean): ExtraProperty<Function> {
    object InheritedFunctionKey: ExtraProperty.Key<Function, Boolean> {
        override fun mergeStrategyFor(left: Boolean, right: Boolean) = MergeStrategy.Fail {
            throw IllegalArgumentException("Function inheritance should be consistent!")
        }
    }
    override val key: ExtraProperty.Key<Function, Boolean> =
        InheritedFunctionKey
}