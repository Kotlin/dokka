/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("OPT_IN_IS_NOT_ENABLED") // TODO

package org.jetbrains.dokka.gradle.dsl

@RequiresOptIn(
    message = "Most likely it doesn't support Gradle Isolated Projects",
    level = RequiresOptIn.Level.WARNING
)
public annotation class GradleIsolatedProjectsRestrictions(val reason: String)

@DslMarker
public annotation class DokkaGradlePluginDsl

@RequiresOptIn(
    message = "TODO",
    level = RequiresOptIn.Level.WARNING
)
public annotation class DokkaDelicateApi