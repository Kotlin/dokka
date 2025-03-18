/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

public interface KdSample {
    public val id: KdSampleId
    // content here
}

public typealias KdSampleId = String
