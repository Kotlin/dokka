package org.jetbrains.dokka.base.templating

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.pages.ContentNode

data class InsertTemplateExtra(val command: Command) : ExtraProperty<ContentNode> {

    companion object : ExtraProperty.Key<ContentNode, InsertTemplateExtra>

    override val key: ExtraProperty.Key<ContentNode, *>
        get() = Companion
}