/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

public enum class KdVisibility {
    PUBLIC, PROTECTED, INTERNAL, PRIVATE,

    // java specific visibilities
    PACKAGE_PROTECTED, PACKAGE_PRIVATE
}

public enum class KdModality {
    FINAL, SEALED, OPEN, ABSTRACT;
}
