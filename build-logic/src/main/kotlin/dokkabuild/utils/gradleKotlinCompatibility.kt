/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dokkabuild.utils

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dokkaBuild
import org.gradle.kotlin.dsl.libs
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// We must use Kotlin language/api version 1.4 (DEPRECATED) to support Gradle 7,
// and to do that we need to use old Kotlin version, which has support for such an old Kotlin version (e.g., 2.0.20)
fun Project.configureGradleKotlinCompatibility() {
    /**
     * The AA is built with the latest compiler version (a bootstrap compiler)
     * To be compatible with the AA, Dokka analysis should be compiled with approximately the same version
     * See https://kotlinlang.org/docs/kotlin-evolution-principles.html#evolving-the-binary-format
     */
    val analysisK2Projects = listOf("analysis-kotlin-symbols", "runner-maven-plugin")
    if (!dokkaBuild.enforceGradleKotlinCompatibility.get() ||  project.name in analysisK2Projects) return

    @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalBuildToolsApi::class)
    extensions.configure<KotlinJvmProjectExtension>("kotlin") {
        val btaCompilerVersion = libs.versions.bta.kotlin.compiler
        val btaLanguageVersion = libs.versions.bta.kotlin.language.map(KotlinVersion::fromVersion)
        compilerVersion.set(btaCompilerVersion)
        coreLibrariesVersion = btaCompilerVersion.get()
        compilerOptions {
            languageVersion.set(btaLanguageVersion)
            apiVersion.set(btaLanguageVersion)
            freeCompilerArgs.addAll(
                "-Xsuppress-version-warnings",
                // we need this flag to be able to use newer Analysis API versions
                "-Xskip-metadata-version-check"
            )
        }
    }
}
