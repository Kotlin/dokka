/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.utils.create_
import org.jetbrains.dokka.gradle.utils.enableV2Plugin

class DokkaPluginTest : FunSpec({

    test("expect plugin id can be applied to project successfully") {
        val project = ProjectBuilder.builder().build()
            .enableV2Plugin()
        project.plugins.apply("org.jetbrains.dokka")
        project.plugins.hasPlugin("org.jetbrains.dokka") shouldBe true
        project.plugins.hasPlugin(DokkaPlugin::class) shouldBe true
    }

    test("expect plugin class can be applied to project successfully") {
        val project = ProjectBuilder.builder().build()
            .enableV2Plugin()
        project.plugins.apply(type = DokkaPlugin::class)
        project.plugins.hasPlugin("org.jetbrains.dokka") shouldBe true
        project.plugins.hasPlugin(DokkaPlugin::class) shouldBe true
    }

    context("Dokkatoo property conventions") {
        val project = ProjectBuilder.builder().build()
            .enableV2Plugin()
        project.plugins.apply("org.jetbrains.dokka")

        val extension = project.extensions.getByType<DokkaExtension>()

        context("DokkatooSourceSets") {
            val testSourceSet = extension.dokkaSourceSets.create_("Test") {
                externalDocumentationLinks.create_("gradle") {
                    url("https://docs.gradle.org/7.6.1/javadoc")
                }
            }

            context("JDK external documentation link") {
                val jdkLink = testSourceSet.externalDocumentationLinks.getByName("jdk")

                test("when enableJdkDocumentationLink is false, expect jdk link is disabled") {
                    testSourceSet.enableJdkDocumentationLink.set(false)
                    jdkLink.enabled.get() shouldBe false
                }

                test("when enableJdkDocumentationLink is true, expect jdk link is enabled") {
                    testSourceSet.enableJdkDocumentationLink.set(true)
                    jdkLink.enabled.get() shouldBe true
                }

                (5..10).forEach { jdkVersion ->
                    test("when jdkVersion is $jdkVersion, expect packageListUrl uses package-list file") {
                        testSourceSet.jdkVersion.set(jdkVersion)
                        jdkLink.packageListUrl.get().toString() shouldEndWith "package-list"
                    }
                }

                (11..22).forEach { jdkVersion ->
                    test("when jdkVersion is $jdkVersion, expect packageListUrl uses element-list file") {
                        testSourceSet.jdkVersion.set(jdkVersion)
                        jdkLink.packageListUrl.get().toString() shouldEndWith "element-list"
                    }
                }
            }

            context("external doc links") {
                test("package-list url should be appended to Javadoc URL") {
                    val gradleDocLink = testSourceSet.externalDocumentationLinks.getByName("gradle")
                    gradleDocLink.packageListUrl.get()
                        .toString() shouldBe "https://docs.gradle.org/7.6.1/javadoc/package-list"
                }
            }
        }
    }
})
