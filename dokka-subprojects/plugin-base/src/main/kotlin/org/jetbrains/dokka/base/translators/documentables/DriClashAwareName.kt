/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.ExtraProperty

public data class DriClashAwareName(val value: String?): ExtraProperty<Documentable> {
    public companion object : ExtraProperty.Key<Documentable, DriClashAwareName>
    override val key: ExtraProperty.Key<Documentable, *> = Companion
}
