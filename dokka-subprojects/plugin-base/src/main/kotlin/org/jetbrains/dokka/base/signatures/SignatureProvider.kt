/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.signatures

import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.pages.ContentNode

public fun interface SignatureProvider {
    public fun signature(documentable: Documentable): List<ContentNode>
}
