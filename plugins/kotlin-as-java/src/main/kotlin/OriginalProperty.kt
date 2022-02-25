package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy

class OriginalProperty(val original: DProperty) : ExtraProperty<DFunction> {
    override val key = OriginalProperty
    companion object: ExtraProperty.Key<DFunction, OriginalProperty>
}