/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// we should be able to generated/have sourceLinks to GH and from inside `sources.jar`
public interface KdSourceInfo {
    public val path: String // TODO?
    public val language: KdSourceLanguage
}

public sealed class KdSourceLanguage {
    public object Kotlin : KdSourceLanguage()
    public object Java : KdSourceLanguage()
}
