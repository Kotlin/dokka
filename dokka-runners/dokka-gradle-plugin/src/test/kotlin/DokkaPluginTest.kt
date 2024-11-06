/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Exhaustive
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.boolean
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.hasPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.dokka.gradle.utils.create_
import org.jetbrains.dokka.gradle.utils.enableV2Plugin
import org.jetbrains.dokka.gradle.workers.WorkerIsolation

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

    context("Dokka property conventions") {

        context("Dokka Generator isolation") {
            val project = projectWithDgpV2()

            val extension = project.extensions.getByType<DokkaExtension>()

            test("default worker isolation should be classloader") {
                extension.dokkaGeneratorIsolation.orNull.asClue { value ->
                    withClue("value must not be null") {
                        value shouldNotBe null
                    }
                    value.shouldBeInstanceOf<WorkerIsolation>()
                }
            }
        }

        context("DokkaSourceSets") {
            val project = projectWithDgpV2()

            val extension = project.extensions.getByType<DokkaExtension>()

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

                test("externalDocumentationLinks should be enabled by default") {
                    val fooLink = testSourceSet.externalDocumentationLinks.create("foo")
                    fooLink.enabled.orNull shouldBe true
                }

                test("kotlinStdlib externalDocumentationLink should be disabled when DokkaSourceSetSpec.enableKotlinStdLibDocumentationLink is disabled") {
                    testSourceSet.enableKotlinStdLibDocumentationLink.set(false)
                    val kotlinStdlib = testSourceSet.externalDocumentationLinks.getByName("kotlinStdlib")
                    kotlinStdlib.enabled.orNull shouldBe false
                }

                context("Android externalDocumentationLinks should be disabled when DokkaSourceSetSpec.enableAndroidDocumentationLink is disabled") {
                    testSourceSet.enableAndroidDocumentationLink.set(false)
                    test("androidSdk") {
                        val androidSdk = testSourceSet.externalDocumentationLinks.getByName("androidSdk")
                        androidSdk.enabled.orNull shouldBe false
                    }
                    test("androidX") {
                        val androidX = testSourceSet.externalDocumentationLinks.getByName("androidX")
                        androidX.enabled.orNull shouldBe false
                    }
                }
            }

            context("perPackageOptions") {
                test("new element should have expected convention values") {

                    // perPackageOptions aren't named, so we can't create and fetch a specific element.
                    // Instead, clear all other elements and create a new one, then fetch the first.
                    testSourceSet.perPackageOptions.clear()
                    testSourceSet.perPackageOption { }

                    val perPackageOption = testSourceSet.perPackageOptions
                        .shouldBeSingleton()
                        .single()

                    withClue("matchingRegex") {
                        perPackageOption.matchingRegex.orNull shouldBe ".*"
                    }
                    withClue("suppress") {
                        perPackageOption.suppress.orNull shouldBe false
                    }
                    withClue("skipDeprecated") {
                        perPackageOption.skipDeprecated.orNull shouldBe false
                    }
                    withClue("reportUndocumented") {
                        perPackageOption.reportUndocumented.orNull shouldBe false
                    }
                    withClue("documentedVisibilities") {
                        perPackageOption.documentedVisibilities.orNull.shouldContainExactly(VisibilityModifier.Public)
                    }
                }
            }
        }

        context("DokkaPublications") {

            test("DokkaExtension 'suppress' settings provides convention values for DokkaPublications") {
                val project = projectWithDgpV2()

                val dokka = project.extensions.getByType<DokkaExtension>()

                val publication = dokka.dokkaPublications.create("TestPublication")

                checkAll(
                    Exhaustive.boolean(),
                    Exhaustive.boolean(),
                ) { suppressInheritedMembersValue, suppressObviousFunctionsValue ->

                    @Suppress("DEPRECATION")
                    dokka.suppressInheritedMembers.set(suppressInheritedMembersValue)
                    @Suppress("DEPRECATION")
                    dokka.suppressObviousFunctions.set(suppressObviousFunctionsValue)

                    withClue("suppressInheritedMembers should be $suppressInheritedMembersValue") {
                        publication.suppressInheritedMembers.orNull shouldBe suppressInheritedMembersValue
                    }
                    withClue("suppressObviousFunctions should be $suppressObviousFunctionsValue") {
                        publication.suppressObviousFunctions.orNull shouldBe suppressObviousFunctionsValue
                    }
                }
            }

            test("DokkaExtension 'suppress' settings convention values can be overridden by DokkaPublications") {
                val project = projectWithDgpV2()

                val dokka = project.extensions.getByType<DokkaExtension>()

                val publication = dokka.dokkaPublications.create("TestPublication}")

                checkAll(
                    Exhaustive.boolean(),
                    Exhaustive.boolean(),
                ) { suppressInheritedMembersValue,
                    suppressObviousFunctionsValue ->

                    @Suppress("DEPRECATION")
                    dokka.suppressInheritedMembers.set(suppressInheritedMembersValue)
                    @Suppress("DEPRECATION")
                    dokka.suppressObviousFunctions.set(suppressObviousFunctionsValue)

                    val suppressInheritedMembersOverride = !suppressInheritedMembersValue
                    val suppressObviousFunctionsOverride = !suppressObviousFunctionsValue

                    publication.apply {
                        suppressInheritedMembers.set(suppressInheritedMembersOverride)
                        suppressObviousFunctions.set(suppressObviousFunctionsOverride)
                    }

                    withClue("suppressInheritedMembers should be $suppressInheritedMembersOverride") {
                        publication.suppressInheritedMembers.orNull shouldBe suppressInheritedMembersOverride
                    }
                    withClue("suppressObviousFunctions should be $suppressObviousFunctionsOverride") {
                        publication.suppressObviousFunctions.orNull shouldBe suppressObviousFunctionsOverride
                    }
                }
            }
        }
    }
})

private fun projectWithDgpV2(): Project =
    ProjectBuilder.builder().build()
        .enableV2Plugin()
        .also { it.plugins.apply("org.jetbrains.dokka") }
