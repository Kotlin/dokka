/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestScope
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier.Public
import org.jetbrains.dokka.gradle.utils.create_
import org.jetbrains.dokka.gradle.utils.enableV2Plugin
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

class DokkaSourceSetSpecTest : FunSpec({

    context("verify DokkaSourceSetSpec conventions") {
        val project = createProject()
        val dss = project.createDokkaSourceSetSpec("foo")

        test("analysisPlatform") {
            dss.analysisPlatform.orNull shouldBe KotlinPlatform.DEFAULT
        }
        test("apiVersion") {
            dss.apiVersion.orNull shouldBe null
        }
        test("classpath") {
            dss.classpath.shouldBeEmpty()
        }
        test("dependentSourceSets") {
            dss.dependentSourceSets.shouldBeEmpty()
        }
        test("displayName") {
            dss.displayName.orNull shouldBe "foo"
        }
        test("documentedVisibilities") {
            dss.documentedVisibilities.orNull.shouldContainExactly(Public)
        }
        test("enableAndroidDocumentationLink") {
            dss.enableAndroidDocumentationLink.orNull shouldBe false
        }
        test("enableJdkDocumentationLink") {
            dss.enableJdkDocumentationLink.orNull shouldBe true
        }
        test("enableKotlinStdLibDocumentationLink") {
            dss.enableKotlinStdLibDocumentationLink.orNull shouldBe true
        }
        test("externalDocumentationLinks") {
            dss.externalDocumentationLinks
                .map { it.run { "$name enabled:${enabled.orNull} url:${url.orNull} packageList:${packageListUrl.orNull}" } }
                .shouldContainExactly(
                    "androidSdk enabled:false url:https://developer.android.com/reference/kotlin/ packageList:https://developer.android.com/reference/kotlin/package-list",
                    "androidX enabled:false url:https://developer.android.com/reference/kotlin/ packageList:https://developer.android.com/reference/kotlin/androidx/package-list",
                    "jdk enabled:true url:https://docs.oracle.com/en/java/javase/11/docs/api/ packageList:https://docs.oracle.com/en/java/javase/11/docs/api/element-list",
                    "kotlinStdlib enabled:true url:https://kotlinlang.org/api/core/ packageList:https://kotlinlang.org/api/core/package-list",
                )
        }
        test("includes") {
            dss.includes.shouldBeEmpty()
        }
        test("jdkVersion") {
            dss.jdkVersion.orNull shouldBe 11
        }
        test("languageVersion") {
            dss.languageVersion.orNull shouldBe null
        }
        test("name") {
            dss.name shouldBe "foo"
        }
        test("perPackageOptions") {
            dss.perPackageOptions.shouldBeEmpty()
        }
        test("reportUndocumented") {
            dss.reportUndocumented.orNull shouldBe false
        }
        test("samples") {
            dss.samples.shouldBeEmpty()
        }
        test("skipDeprecated") {
            dss.skipDeprecated.orNull shouldBe false
        }
        test("skipEmptyPackages") {
            dss.skipEmptyPackages.orNull shouldBe true
        }
        test("sourceLinks") {
            dss.sourceLinks.shouldBeEmpty()
        }
        test("sourceRoots") {
            dss.sourceRoots.shouldBeEmpty()
        }
        test("sourceSetId") {
            dss.sourceSetId.orNull?.toString() shouldBe "DokkaSourceSetIdSpec(:/foo)"
        }
        test("sourceSetScope") {
            dss.sourceSetScope.orNull shouldBe ":"
            dss.sourceSetScope.orNull shouldBe project.path
        }
        test("suppress") {
            dss.suppress.orNull shouldBe false
        }
        test("suppressGeneratedFiles") {
            dss.suppressGeneratedFiles.orNull shouldBe true
        }
        test("suppressedFiles") {
            dss.suppressedFiles.toList() shouldBe listOf(project.file("build/generated"))
        }
        test("inputSourceFiles") {
            dss.inputSourceFiles.shouldBeEmpty() // because `sourceRoots` is empty
        }
    }

    context("DokkaSourceSetSpec displayName ->") {

        data class TestCase(
            val name: String,
            val platform: KotlinPlatform,
            val expectedDisplayName: String,
        )

        buildList {
            enumValues<KotlinPlatform>().forEach { platform ->
                add(TestCase("main", platform, platform.displayName))
                add(TestCase("Main", platform, platform.displayName))

                add(TestCase("commonMain", platform, "common"))
                add(TestCase("jvmMain", platform, "jvm"))
                add(TestCase("FooMain", platform, "Foo"))

                add(TestCase("foo", platform, "foo"))
                add(TestCase("Domain", platform, "Domain"))
                add(TestCase("CustomMainActor", platform, "CustomMainActor"))
            }
        }.forEach { (name, platform, expectedDisplayName) ->
            test("given name '$name' and platform $platform, expect displayName is '$expectedDisplayName'") {
                val project = createProject()
                val dss = project.createDokkaSourceSetSpec(name)
                dss.analysisPlatform.set(platform)
                dss.displayName.orNull shouldBe expectedDisplayName
            }
        }
    }

    context("DokkaSourceSetSpec inputSourceFiles ->") {

        val project = createProject()

        val projectDir = project.projectDir.toPath()
        fun Path.createClass(name: String) {
            createDirectories()
            resolve("$name.kt").writeText("class $name")
        }

        val mainSourceDir =
            projectDir.resolve("src/main/kotlin").apply {
                resolve("com/example").apply {
                    createClass("MainCls")
                    resolve("sub").apply {
                        createClass("Sub1MainCls")
                        createClass("Sub2MainCls")
                    }
                }
            }
        val generatedSrcDir =
            projectDir.resolve("build/generated/kotlin").apply {
                createClass("BuildGenCls")
                resolve("sub").apply {
                    createClass("Sub1BuildGenCls")
                    createClass("Sub2BuildGenCls")
                }
            }
        val customGeneratedSrcDir =
            projectDir.resolve("src/customGenerated/kotlin").apply {
                createClass("CustomGenCls")
                resolve("sub").apply {
                    createClass("Sub1CustomGenCls")
                    createClass("Sub2CustomGenCls")
                }
            }

        fun TestScope.createDss(): DokkaSourceSetSpec {
            val dss = project.createDokkaSourceSetSpec(testCase.name.testName)
            dss.sourceRoots.from(mainSourceDir, generatedSrcDir, customGeneratedSrcDir)
            return dss
        }

        fun DokkaSourceSetSpec.inputSourceFilesToRelativePaths(): List<Path> =
            inputSourceFiles.map { it.toPath().relativeTo(projectDir) }

        test("build/generated should be excluded by default") {
            val dss = createDss()
            dss.inputSourceFilesToRelativePaths() shouldContainExactlyInAnyOrder listOf(
                Path("src/customGenerated/kotlin/CustomGenCls.kt"),
                Path("src/customGenerated/kotlin/sub/Sub1CustomGenCls.kt"),
                Path("src/customGenerated/kotlin/sub/Sub2CustomGenCls.kt"),
                Path("src/main/kotlin/com/example/MainCls.kt"),
                Path("src/main/kotlin/com/example/sub/Sub1MainCls.kt"),
                Path("src/main/kotlin/com/example/sub/Sub2MainCls.kt"),
            )
        }
        test("when suppressGeneratedFiles is set to false, expect all source files are included") {
            val dss = createDss()
            dss.suppressGeneratedFiles.set(false)
            dss.inputSourceFilesToRelativePaths() shouldContainExactlyInAnyOrder listOf(
                Path("build/generated/kotlin/BuildGenCls.kt"),
                Path("build/generated/kotlin/sub/Sub1BuildGenCls.kt"),
                Path("build/generated/kotlin/sub/Sub2BuildGenCls.kt"),
                Path("src/customGenerated/kotlin/CustomGenCls.kt"),
                Path("src/customGenerated/kotlin/sub/Sub1CustomGenCls.kt"),
                Path("src/customGenerated/kotlin/sub/Sub2CustomGenCls.kt"),
                Path("src/main/kotlin/com/example/MainCls.kt"),
                Path("src/main/kotlin/com/example/sub/Sub1MainCls.kt"),
                Path("src/main/kotlin/com/example/sub/Sub2MainCls.kt"),
            )
        }
        test("input source files should only contain files, not directories") {
            val dss = createDss()
            dss.suppressGeneratedFiles.set(false)
            dss.inputSourceFiles.files.shouldForAll { it.shouldBeAFile() }
        }
        test("expect files can be excluded by exact path") {
            val dss = createDss()
            dss.suppressGeneratedFiles.set(false)
            dss.suppressedFiles.from(
                "build/generated/kotlin/sub/Sub1BuildGenCls.kt",
                "src/customGenerated/kotlin/sub/Sub1CustomGenCls.kt",
                "src/main/kotlin/com/example/sub/Sub1MainCls.kt",
            )
            dss.inputSourceFilesToRelativePaths() shouldContainExactlyInAnyOrder listOf(
                Path("build/generated/kotlin/BuildGenCls.kt"),
                Path("build/generated/kotlin/sub/Sub2BuildGenCls.kt"),
                Path("src/customGenerated/kotlin/CustomGenCls.kt"),
                Path("src/customGenerated/kotlin/sub/Sub2CustomGenCls.kt"),
                Path("src/main/kotlin/com/example/MainCls.kt"),
                Path("src/main/kotlin/com/example/sub/Sub2MainCls.kt"),
            )
        }
        test("expect files can be excluded by base directories") {
            val dss = createDss()
            dss.suppressGeneratedFiles.set(false)
            dss.suppressedFiles.from(
                "build/",
                "src/",
            )
            dss.inputSourceFilesToRelativePaths().shouldBeEmpty()
        }
        test("expect files can be excluded by sub directories") {
            val dss = createDss()
            dss.suppressGeneratedFiles.set(false)
            dss.suppressedFiles.from(
                "build/generated/kotlin/sub/",
                "src/customGenerated/kotlin/sub/",
                "src/main/kotlin/com/example/sub/",
            )
            dss.inputSourceFilesToRelativePaths() shouldContainExactlyInAnyOrder listOf(
                Path("build/generated/kotlin/BuildGenCls.kt"),
                Path("src/customGenerated/kotlin/CustomGenCls.kt"),
                Path("src/main/kotlin/com/example/MainCls.kt"),
            )
        }
        test("expect nested files can be excluded") {
            val dss = createDss()
            dss.suppressGeneratedFiles.set(false)
            dss.suppressedFiles.from(
                "build/generated/kotlin/sub/",
                "src/customGenerated/kotlin/sub/",
                "src/main/kotlin/com/example/sub/",
            )
            dss.inputSourceFilesToRelativePaths() shouldContainExactlyInAnyOrder listOf(
                Path("build/generated/kotlin/BuildGenCls.kt"),
                Path("src/customGenerated/kotlin/CustomGenCls.kt"),
                Path("src/main/kotlin/com/example/MainCls.kt"),
            )
        }
        test("expect suppressGeneratedFiles and suppressedFiles (by specific files) exclude all generated and specified files") {
            val dss = createDss()
            dss.suppressGeneratedFiles.set(true)
            dss.suppressedFiles.from(
                "src/customGenerated/kotlin/sub/Sub1CustomGenCls.kt",
                "src/customGenerated/kotlin/sub/Sub2CustomGenCls.kt",
                "src/main/kotlin/com/example/sub/Sub1MainCls.kt",
                "src/main/kotlin/com/example/sub/Sub2MainCls.kt",
            )
            dss.inputSourceFilesToRelativePaths() shouldContainExactlyInAnyOrder listOf(
                Path("src/customGenerated/kotlin/CustomGenCls.kt"),
                Path("src/main/kotlin/com/example/MainCls.kt"),
            )
        }
        test("expect suppressGeneratedFiles and suppressedFiles (by directories) exclude all generated and specified files") {
            val dss = createDss()
            dss.suppressGeneratedFiles.set(true)
            dss.suppressedFiles.from(
                "src/customGenerated/kotlin/",
            )
            dss.inputSourceFilesToRelativePaths() shouldContainExactlyInAnyOrder listOf(
                Path("src/main/kotlin/com/example/MainCls.kt"),
                Path("src/main/kotlin/com/example/sub/Sub1MainCls.kt"),
                Path("src/main/kotlin/com/example/sub/Sub2MainCls.kt"),
            )
        }
    }
}) {
    companion object {
        private fun createProject(): Project {
            val project = ProjectBuilder.builder().build()
            project.enableV2Plugin()
            project.plugins.apply(type = DokkaPlugin::class)
            return project
        }

        private fun Project.createDokkaSourceSetSpec(
            name: String,
            configure: DokkaSourceSetSpec.() -> Unit = {}
        ): DokkaSourceSetSpec {
            return extensions
                .getByType<DokkaExtension>()
                .dokkaSourceSets
                .create_(name, configure)
        }

    }
}
