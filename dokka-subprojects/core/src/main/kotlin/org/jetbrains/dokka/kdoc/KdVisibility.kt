/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

public sealed class KdVisibility {
    public object Public : KdVisibility()
    public object Protected : KdVisibility()
    public object Internal : KdVisibility()
    public object Private : KdVisibility()

    // java specific visibilities
    public object PackageProtected : KdVisibility()
    public object PackagePrivate : KdVisibility()
}
