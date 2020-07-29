package org.jetbrains.dokka.javadoc.pages

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.pages.ContentNode

data class JavadocIndexExtra(val index: List<ContentNode>) : ExtraProperty<Documentable> {
    override val key: ExtraProperty.Key<Documentable, *> = JavadocIndexExtra
    companion object : ExtraProperty.Key<Documentable, JavadocIndexExtra>
}