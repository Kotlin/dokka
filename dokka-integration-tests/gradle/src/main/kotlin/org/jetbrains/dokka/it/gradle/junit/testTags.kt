/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle.junit

import org.junit.jupiter.api.Tag
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * JUnit [Tag] indicating the test uses Dokka Gradle Plugin in V1 mode.
 */
@Tag("DGPv1")
@Target(FUNCTION, CLASS)
@MustBeDocumented
annotation class TestsDGPv1


/**
 * JUnit [Tag] indicating the test uses Dokka Gradle Plugin in V2 mode.
 */
@Tag("DGPv2")
@Target(FUNCTION, CLASS)
@MustBeDocumented
annotation class TestsDGPv2


/**
 * JUnit [Tag] indicating the test involves Android Gradle Plugin.
 *
 * If a test is annotated with [TestsAndroidGradlePlugin] then
 * [DokkaGradlePluginTestExtension] will run the test multiple times,
 * and provide a [DokkaGradleProjectRunner] with a valid [TestedVersions.agp].
 * Otherwise, [TestedVersions.agp] will be `null`.
 */
@Tag("AndroidGradlePlugin")
@Target(FUNCTION, CLASS)
@MustBeDocumented
annotation class TestsAndroidGradlePlugin


/**
 * JUnit [Tag] indicating the test uses Compose.
 *
 * When used with [DokkaGradlePluginTest], [TestedVersions.composeGradlePlugin] will be set.
 * Otherwise, [TestedVersions.composeGradlePlugin] will be `null`.
 */
@Tag("Compose")
@Target(FUNCTION, CLASS)
@MustBeDocumented
annotation class TestsCompose
