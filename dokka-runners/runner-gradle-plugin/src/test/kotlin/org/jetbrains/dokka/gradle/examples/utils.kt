/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.examples

import org.gradle.api.Project
import org.jetbrains.dokka.gradle.dsl.DokkaExtension

// utils which Gradle will generate
fun Project.dokka(block: DokkaExtension.() -> Unit) {}
