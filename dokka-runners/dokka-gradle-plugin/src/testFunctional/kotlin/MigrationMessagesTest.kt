/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContainOnlyOnce
import io.kotest.matchers.string.shouldNotContain
import org.gradle.testkit.runner.BuildResult
import org.jetbrains.dokka.gradle.internal.DokkaConstants
import org.jetbrains.dokka.gradle.utils.*

class MigrationMessagesTest : FunSpec({
    context("given multi-module project") {

        val project = migrationMessagesTestProject()

        context("when no plugin flags are set") {
            project.runner
                .addArguments(
                    ":help",
                    "--dry-run",
                    "--warn",
                )
                .addArguments()
                .build {
                    test("output should contain V1 warning") {
                        shouldOnlyContainV1Warning()
                    }
                }
        }

        context("when v2 is disabled") {
            project.runner
                .addArguments(
                    ":help",
                    "--dry-run",
                    "--warn",
                    "-P$V2_PLUGIN_ENABLED_FLAG=false",
                )
                .addArguments()
                .build {
                    test("output should contain V1 warning") {
                        shouldOnlyContainV1Warning()
                    }
                }
        }

        context("when v2 is enabled") {

            project.runner
                .addArguments(
                    ":help",
                    "--dry-run",
                    "-P$V2_PLUGIN_ENABLED_FLAG=true",
                )
                .build {
                    test("output should only contain V2 message") {
                        shouldOnlyContainV2Message()
                    }
                }

            listOf(
                V2_PLUGIN_NO_WARN_FLAG,
                V2_PLUGIN_NO_WARN_FLAG_PRETTY,
            ).forEach { noWarnFlag ->
                context("and message is suppressed with $noWarnFlag") {
                    project.runner
                        .addArguments(
                            ":help",
                            "--dry-run",
                            "-P$V2_PLUGIN_ENABLED_FLAG=true",
                            "-P$noWarnFlag=true",
                        )
                        .build {
                            test("output should not contain any Dokka plugin message") {
                                output.shouldNotContainAnyOf(
                                    "Dokka Gradle Plugin V1",
                                    "Dokka Gradle Plugin V2",
                                    "https://kotl.in/dokka-gradle-migration",
                                    "https://github.com/Kotlin/dokka/issues/",
                                    "org.jetbrains.dokka.experimental.gradlePlugin",
                                    V2_PLUGIN_ENABLED_FLAG,
                                    V2_PLUGIN_NO_WARN_FLAG,
                                    V2_PLUGIN_NO_WARN_FLAG_PRETTY,
                                    noWarnFlag,
                                )
                            }
                        }
                }
            }
        }
    }
}) {
    companion object {
        private const val V2_PLUGIN_ENABLED_FLAG =
            "org.jetbrains.dokka.experimental.gradlePlugin.enableV2"

        private const val V2_PLUGIN_NO_WARN_FLAG =
            "${V2_PLUGIN_ENABLED_FLAG}.nowarn"

        private const val V2_PLUGIN_NO_WARN_FLAG_PRETTY =
            "${V2_PLUGIN_ENABLED_FLAG}.noWarn"

        fun BuildResult.shouldOnlyContainV1Warning() {
            output shouldContainOnlyOnce /* language=text */ """
                |┌──────────────────────────────────────────────────────────────────────────────────────┐
                |│ ⚠ Warning: Dokka Gradle Plugin V1 mode is enabled                                    │
                |│                                                                                      │
                |│   V1 mode is deprecated, and will be removed in Dokka version 2.1.0                  │
                |│                                                                                      │
                |│   Please migrate Dokka Gradle Plugin to V2. This will require updating your project. │
                |│   To get started check out the Dokka Gradle Plugin Migration guide                   │
                |│       https://kotl.in/dokka-gradle-migration                                         │
                |│                                                                                      │
                |│   Once you have prepared your project, enable V2 by adding                           │
                |│       org.jetbrains.dokka.experimental.gradlePlugin.enableV2=true                    │
                |│   to your project's `gradle.properties`                                              │
                |│                                                                                      │
                |│   Please report any feedback or problems to Dokka GitHub Issues                      │
                |│       https://github.com/Kotlin/dokka/issues/                                        │
                |└──────────────────────────────────────────────────────────────────────────────────────┘
                """.trimMargin()
            output shouldNotContain "Dokka Gradle Plugin V2"
        }

        fun BuildResult.shouldOnlyContainV2Message() {
            output shouldContainOnlyOnce /* language=text */ """
                |┌──────────────────────────────────────────────────────────────────────────┐
                |│ Dokka Gradle Plugin V2 is enabled ♡                                      │
                |│                                                                          │
                |│   We would appreciate your feedback!                                     │
                |│   Please report any feedback or problems to Dokka GitHub Issues          │
                |│       https://github.com/Kotlin/dokka/issues/                            │
                |│                                                                          │
                |│   If you need help or advice, check out the migration guide              │
                |│       https://kotl.in/dokka-gradle-migration                             │
                |│                                                                          │
                |│   You can suppress this message by adding                                │
                |│       org.jetbrains.dokka.experimental.gradlePlugin.enableV2.nowarn=true │
                |│   to your project's `gradle.properties`                                  │
                |└──────────────────────────────────────────────────────────────────────────┘
                """.trimMargin()
            output shouldNotContain "Dokka Gradle Plugin V1"
        }

    }
}


/**
 * A simple multi-module project with no configuration (so that it can be used with both Dokka V1 and V2 plugins).
 *
 * It's a multi-module project to verify that even though there are multiple subprojects only one message is logged.
 */
private fun migrationMessagesTestProject(
    config: GradleProjectTest.() -> Unit = {},
): GradleProjectTest {

    return gradleKtsProjectTest("MigrationMessagesTest") {

        gradleProperties {
            dokka {
                v2Plugin = null
                v2PluginNoWarn = null
            }
        }

        settingsGradleKts += """
            |
            |include(":subproject-one")
            |include(":subproject-two")
            |
            """.trimMargin()

        buildGradleKts = """
            |plugins {
            |  kotlin("jvm") version embeddedKotlinVersion apply false
            |  // important: don't register Dokka in the root project, because we _want_ the test to trigger a
            |  // Gradle classloader bug.
            |  //id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
            |}
            |
            """.trimMargin()

        dir("subproject-one") {
            buildGradleKts = """
                |plugins {
                |  kotlin("jvm") version embeddedKotlinVersion
                |  // important: Register different plugins here to make the buildscript classpath different,
                |  // because we _want_ the test to trigger a Gradle classloader bug.
                |  kotlin("plugin.serialization") version embeddedKotlinVersion
                |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
                |}
                |
                """.trimMargin()

            createKotlinFile(
                "src/main/kotlin/One.kt",
                """
                |package com.project.one
                |
                |/** `One` class */
                |class One {
                |    /** prints `One` to the console */  
                |    fun sayName() = println("One")
                |}
                |
                """.trimMargin()
            )
        }

        dir("subproject-two") {

            buildGradleKts = """
                |plugins {
                |  kotlin("jvm") version embeddedKotlinVersion
                |  id("org.jetbrains.dokka") version "${DokkaConstants.DOKKA_VERSION}"
                |}
                |
                """.trimMargin()

            createKotlinFile(
                "src/main/kotlin/Two.kt",
                """
                |package com.project.two
                |
                |/** `Two` class */
                |class Two {
                |    /** prints `Two` to the console */  
                |    fun sayName() = println("Two")
                |}
                |
                """.trimMargin()
            )
        }

        config()
    }
}
