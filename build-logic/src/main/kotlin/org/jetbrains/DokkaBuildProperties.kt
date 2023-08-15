package org.jetbrains

import org.gradle.api.plugins.ExtensionAware
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
) : ExtensionAware {

    val dokkaVersion: Provider<String> = providers.gradleProperty("dokka_version")

    val dokkaVersionType: Provider<DokkaVersionType> =
        // workaround for https://github.com/gradle/gradle/issues/19088
        dokkaVersion.flatMap { providers.provider { DokkaVersionType.from(it) } }
    // replace in Gradle 8:
    // dokkaVersion.map { DokkaVersionType.from(it) }


    //region Compilation
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
    //endregion

    //region Testing
    /** Control the parallelism of integration tests */
    val integrationTestParallelism: Provider<Int> =
        dokkaProperty("integrationTest.parallelism", String::toInt)

    /** Enable re-running integration tests with all potential input variables */
    val integrationTestExhaustive: Provider<Boolean> =
        dokkaProperty("integrationTest.exhaustive")
            .orElse(providers.environmentVariable("DOKKA_INTEGRATION_TEST_IS_EXHAUSTIVE"))
            .map(String::toBoolean)
    //endregion

    //region Publishing
    /** Determines which Maven Repositories are enabled for publishing. */
    val publicationChannels: Set<DokkaPublicationChannel> =
        dokkaProperty("publicationChannels").map { publicationChannels ->
            publicationChannels.split("&")
                .filter(String::isNotBlank)
                .map(DokkaPublicationChannel.Companion::fromPropertyString)
                .toSet()
        }.getOrElse(emptySet())


    val signingKeyId: Provider<String> =
        dokkaProperty("signing.key_id").orElse(providers.systemProperty("SIGN_KEY_ID"))

    val signingKey: Provider<String> =
        dokkaProperty("signing.key").orElse(providers.systemProperty("SIGN_KEY"))

    val signingKeyPassphrase: Provider<String> =
        dokkaProperty("signing.key_passphrase").orElse(providers.systemProperty("SIGN_KEY_PASSPHRASE"))
    //endregion

    private fun dokkaProperty(name: String): Provider<String> =
        providers.gradleProperty("org.jetbrains.dokka.$name")

    private fun <T : Any> dokkaProperty(name: String, convert: (String) -> T): Provider<T> =
        providers.gradleProperty("org.jetbrains.dokka.$name").map(convert)

    companion object {
        const val EXTENSION_NAME = "dokkaBuild"
    }
}
