/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.templating

import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.pages.ContentNode

public data class InsertTemplateExtra(val command: Command) : ExtraProperty<ContentNode> {

    public companion object : ExtraProperty.Key<ContentNode, InsertTemplateExtra>

    override val key: ExtraProperty.Key<ContentNode, *>
        get() = Companion
}
