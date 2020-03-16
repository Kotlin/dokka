package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

data class InheritedFunction(val isInherited: Boolean): ExtraProperty<DFunction> {
    object InheritedFunctionKey: ExtraProperty.Key<DFunction, Boolean> {
        override fun mergeStrategyFor(left: Boolean, right: Boolean) = MergeStrategy.Fail {
            throw IllegalArgumentException("Function inheritance should be consistent!")
        }
    }
    override val key: ExtraProperty.Key<DFunction, Boolean> =
        InheritedFunctionKey
}