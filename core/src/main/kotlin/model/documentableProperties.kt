package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

data class InheritedFunction(val inheritedFrom: DRI?): ExtraProperty<DFunction> {
    companion object : ExtraProperty.Key<DFunction, InheritedFunction> {
        override fun mergeStrategyFor(left: InheritedFunction, right: InheritedFunction) = MergeStrategy.Fail {
            throw IllegalArgumentException("Function inheritance should be consistent!")
        }
    }

    val isInherited: Boolean
        get() = inheritedFrom != null

    override val key: ExtraProperty.Key<DFunction, *> = InheritedFunction
}

data class ImplementedInterfaces(val interfaces: List<DRI>): ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, ImplementedInterfaces> {
        override fun mergeStrategyFor(left: ImplementedInterfaces, right: ImplementedInterfaces) = MergeStrategy.Fail {
            throw IllegalArgumentException("Implemented interfaces should be consistent!")
        }
    }

    override val key: ExtraProperty.Key<Documentable, *> = ImplementedInterfaces
}