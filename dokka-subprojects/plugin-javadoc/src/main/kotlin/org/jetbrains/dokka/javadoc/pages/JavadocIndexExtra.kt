/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc.pages

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.pages.ContentNode

public data class JavadocIndexExtra(val index: List<ContentNode>) : ExtraProperty<Documentable> {
    override val key: ExtraProperty.Key<Documentable, *> = JavadocIndexExtra
    public companion object : ExtraProperty.Key<Documentable, JavadocIndexExtra>
}
