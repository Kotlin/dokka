/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.utils.projects

/**
 * Default version of Kotlin Gradle Plugin used in functional tests.
 *
 * The KGP version does not usually need to be dynamic for DGP functional tests,
 * since DPG functional behaviour should not depend on the KGP version.
 * Tests that run against different KGP versions are in the `dokka-integration-tests` project.
 */
// Must use KGP >= 2.3.0:
// - to avoid KT-77988
// - to test `generatedKotlin` API
const val defaultKgpTestVersion = "2.3.0"
