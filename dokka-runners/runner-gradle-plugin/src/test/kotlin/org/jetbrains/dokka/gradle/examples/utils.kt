/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.examples

import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.jetbrains.dokka.gradle.dsl.DokkaExtension
import org.jetbrains.dokka.gradle.dsl.settings.DokkaSettingsExtension

// utils which Gradle will generate
val Project.dokka: DokkaExtension get() = TODO()
fun Project.dokka(block: DokkaExtension.() -> Unit) {}

val Settings.dokka: DokkaSettingsExtension get() = TODO()
fun Settings.dokka(block: DokkaSettingsExtension.() -> Unit) {}
