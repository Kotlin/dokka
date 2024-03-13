/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("OPT_IN_IS_NOT_ENABLED") // TODO

package org.jetbrains.dokka.gradle.dsl

@DslMarker
public annotation class DokkaGradlePluginDsl

// should not be used for simple cases
@RequiresOptIn(
    message = "API for advanced use cases",
    level = RequiresOptIn.Level.ERROR
)
public annotation class DokkaGradlePluginDelicateApi

// maybe we don't need to provide this
@RequiresOptIn(
    message = "experimental API, may be dropped in future",
    level = RequiresOptIn.Level.WARNING
)
public annotation class DokkaGradlePluginExperimentalApi
