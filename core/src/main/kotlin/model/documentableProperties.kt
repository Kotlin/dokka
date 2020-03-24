package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

data class InheritedFunction(val isInherited: Boolean): ExtraProperty<DFunction> {
    companion object : ExtraProperty.Key<DFunction, InheritedFunction> {
        override fun mergeStrategyFor(left: InheritedFunction, right: InheritedFunction) = MergeStrategy.Fail {
            throw IllegalArgumentException("Function inheritance should be consistent!")
        }
    }
    override val key: ExtraProperty.Key<DFunction, *> = InheritedFunction
}