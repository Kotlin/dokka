/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dokkabuild

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import javax.inject.Inject

/**
 * Common build properties used to build Dokka subprojects.
 *
 * This is an extension created by the [org.jetbrains.conventions.Base_gradle] convention plugin.
 *
 * Default values are set in the root `gradle.properties`, and can be overridden via
 * [project properties](https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties)
 */
abstract class DokkaBuildProperties @Inject constructor(
    private val providers: ProviderFactory,
) {

    /**
     * The main version of Java that should be used to build Dokka source code.
     *
     * Updating the Java target is a breaking change.
     */
    val mainJavaVersion: Provider<JavaLanguageVersion> =
        dokkaProperty("javaToolchain.mainCompiler", JavaLanguageVersion::of)

    /**
     * The version of Java that should be used to run Dokka tests.
     *
     * This value is set in CI/CD environments to make sure that Dokka still works with different
     * versions of Java.
     */
    val testJavaLauncherVersion: Provider<JavaLanguageVersion> =
        dokkaProperty("javaToolchain.testLauncher", JavaLanguageVersion::of)
            .orElse(mainJavaVersion)

    /**
     * The Kotlin language level that Dokka artifacts are compiled to support.
     *
     * Updating the language level is a breaking change.
     */
    val kotlinLanguageLevel: Provider<KotlinVersion> =
        dokkaProperty("kotlinLanguageLevel", KotlinVersion::fromVersion)

    /** Control [org.gradle.api.tasks.testing.Test.maxParallelForks] in integration tests. */
    val integrationTestParallelism: Provider<Int> =
        dokkaProperty("integration_test.parallelism", String::toInt)

    /** Not currently used - should be dropped. */
    val integrationTestExhaustive: Provider<Boolean> =
        dokkaProperty("integration_test.exhaustive", String::toBoolean)
            .orElse(false)

    /** Control whether integration tests should use the `org.jetbrains.dokka.experimental.tryK2` flag. */
    val integrationTestUseK2: Provider<Boolean> =
        dokkaProperty("integration_test.useK2", String::toBoolean)
            .orElse(false)


    private fun <T : Any> dokkaProperty(name: String, convert: (String) -> T) =
        providers.gradleProperty("org.jetbrains.dokka.$name").map(convert)

    companion object {
        const val EXTENSION_NAME = "dokkaBuild"
    }
}
