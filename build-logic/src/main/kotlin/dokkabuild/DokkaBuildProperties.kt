/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dokkabuild

import org.gradle.api.logging.Logging
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

    val integrationTestParallelism: Provider<Int> =
        dokkaProperty("integration_test.parallelism", String::toInt)
            // fallback & warn if the old property is used
            .orElse(
                providers.gradleProperty("dokka_integration_test_parallelism")
                    .map {
                        logger.warn("dokka_integration_test_parallelism is deprecated - use org.jetbrains.dokka.integration_test.parallelism instead")
                        it.toInt()
                    }
            )

    val integrationTestExhaustive: Provider<Boolean> =
        dokkaProperty("integration_test.exhaustive", String::toBoolean)
            // fallback & warn if the old property is used
            .orElse(
                providers.gradleProperty("dokka_integration_test_is_exhaustive")
                    .map {
                        logger.warn("dokka_integration_test_is_exhaustive is deprecated - use org.jetbrains.dokka.integration_test.exhaustive instead")
                        it.toBoolean()
                    }
            )
            .orElse(
                providers.environmentVariable("DOKKA_INTEGRATION_TEST_IS_EXHAUSTIVE")
                    .map {
                        logger.warn("DOKKA_INTEGRATION_TEST_IS_EXHAUSTIVE is deprecated - use ORG_GRADLE_PROJECT_org.jetbrains.dokka.integration_test.exhaustive instead")
                        it.toBoolean()
                    }
            )

    val integrationTestUseK2: Provider<Boolean> =
        dokkaProperty("integration_test.useK2", String::toBoolean)
            .orElse(false)


    private fun <T : Any> dokkaProperty(name: String, convert: (String) -> T) =
        providers.gradleProperty("org.jetbrains.dokka.$name").map(convert)

    companion object {
        private val logger = Logging.getLogger(DokkaBuildProperties::class.java)

        const val EXTENSION_NAME = "dokkaBuild"
    }
}
