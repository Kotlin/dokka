/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.jetbrains.dokka.gradle.dsl.formats.DokkaFormatConfiguration
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget


// save as for target
public fun KotlinMultiplatformExtension.withDokkaJar() {}

public fun KotlinTarget.withDokkaJar(publish: Boolean) {
}

public fun KotlinTarget.withDokkaJavadocJar(publish: Boolean) {
}

public fun KotlinTarget.withDokkaJar(format: String, classifier: String) {
}

public fun KotlinTarget.withDokkaJar(format: DokkaFormatConfiguration, classifier: String) {
}

public fun KotlinTarget.withDokkaJavadocJar(format: String) {
}

public fun KotlinTarget.withDokkaJavadocJar(format: DokkaFormatConfiguration) {
}

// low-level API
//public fun Project.registerDokkaJarTask(
//    format: String, // dokka.formats.html.name or just `html`
//    classifier: String?
//): TaskProvider<Jar> = TODO()
//
//public fun Project.registerDokkaJarTask(
//    format: DokkaFormatConfiguration,
//    classifier: String?
//): TaskProvider<Jar> = registerDokkaJarTask(format.name, classifier)
//
//public fun Project.registerDokkaJavadocJarTask(
//    format: String, // dokka.formats.html
//): TaskProvider<Jar> = registerDokkaJarTask(format, "javadoc")
//
//public fun Project.registerDokkaJavadocJarTask(
//    format: DokkaFormatConfiguration,
//): TaskProvider<Jar> = registerDokkaJarTask(format, "javadoc")
