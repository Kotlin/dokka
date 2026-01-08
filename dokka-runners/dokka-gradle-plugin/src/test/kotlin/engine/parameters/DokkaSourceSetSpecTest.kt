/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
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

        test("should contain all files from sourceRoots when no suppression is configured") {
            val project = createProject()
            val dss = project.createDokkaSourceSetSpec("foo")

            val srcDir = project.file("src/main/kotlin").apply { mkdirs() }
            val file1 = srcDir.resolve("File1.kt").apply { writeText("class File1") }
            val file2 = srcDir.resolve("File2.kt").apply { writeText("class File2") }

            dss.sourceRoots.from(srcDir)

            dss.inputSourceFiles.shouldContainExactly(file1, file2)
        }

        test("should exclude manually suppressed files from suppressedFiles") {
            val project = createProject()
            val dss = project.createDokkaSourceSetSpec("foo")

            val srcDir = project.file("src/main/kotlin").apply { mkdirs() }
            val file1 = srcDir.resolve("File1.kt").apply { writeText("class File1") }
            val file2 = srcDir.resolve("File2.kt").apply { writeText("class File2") }
            val file3 = srcDir.resolve("File3.kt").apply { writeText("class File3") }

            dss.sourceRoots.from(srcDir)
            dss.suppressedFiles.from(file2)
            dss.suppressGeneratedFiles.set(false)

            dss.inputSourceFiles.shouldContainExactly(file1, file3)
        }

        test("should exclude generated files when suppressGeneratedFiles is true") {
            val project = createProject()
            val dss = project.createDokkaSourceSetSpec("foo")

            val srcDir = project.file("src/main/kotlin").apply { mkdirs() }
            val regularFile = srcDir.resolve("Regular.kt").apply { writeText("class Regular") }
            val generatedDir = project.file("build/generated/source").apply { mkdirs() }
            generatedDir.resolve("Generated.kt").apply { writeText("class Generated") }

            dss.sourceRoots.from(srcDir, generatedDir)
            dss.suppressGeneratedFiles.set(true)

            dss.inputSourceFiles.shouldContainExactly(regularFile)
        }

        test("should include generated files when suppressGeneratedFiles is false") {
            val project = createProject()
            val dss = project.createDokkaSourceSetSpec("foo")

            val srcDir = project.file("src/main/kotlin").apply { mkdirs() }
            val regularFile = srcDir.resolve("Regular.kt").apply { writeText("class Regular") }
            val generatedDir = project.file("build/generated/source").apply { mkdirs() }
            val generatedFile = generatedDir.resolve("Generated.kt").apply { writeText("class Generated") }

            dss.sourceRoots.from(srcDir, generatedDir)
            dss.suppressGeneratedFiles.set(false)

            dss.inputSourceFiles.shouldContainExactly(generatedFile, regularFile)
        }

        test("should correctly apply both suppressedFiles and suppressGeneratedFiles filters") {
            val project = createProject()
            val dss = project.createDokkaSourceSetSpec("foo")

            val srcDir = project.file("src/main/kotlin").apply { mkdirs() }
            val file1 = srcDir.resolve("File1.kt").apply { writeText("class File1") }
            val file2 = srcDir.resolve("File2.kt").apply { writeText("class File2") }
            val generatedDir = project.file("build/generated/source").apply { mkdirs() }
            generatedDir.resolve("Generated.kt").apply { writeText("class Generated") }

            dss.sourceRoots.from(srcDir, generatedDir)
            dss.suppressedFiles.from(file2)
            dss.suppressGeneratedFiles.set(true)

            dss.inputSourceFiles.shouldContainExactly(file1)
        }

        test("should handle multiple source roots correctly") {
            val project = createProject()
            val dss = project.createDokkaSourceSetSpec("foo")

            val srcDir1 = project.file("src/main/kotlin").apply { mkdirs() }
            val file1 = srcDir1.resolve("File1.kt").apply { writeText("class File1") }
            val srcDir2 = project.file("src/test/kotlin").apply { mkdirs() }
            val file2 = srcDir2.resolve("File2.kt").apply { writeText("class File2") }
            val srcDir3 = project.file("src/integration/kotlin").apply { mkdirs() }
            val file3 = srcDir3.resolve("File3.kt").apply { writeText("class File3") }

            dss.sourceRoots.from(srcDir1, srcDir2, srcDir3)
            dss.suppressGeneratedFiles.set(false)

            dss.inputSourceFiles.shouldContainExactly(file3, file1, file2)
        }

        test("should filter files correctly with nested directory structures") {
            val project = createProject()
            val dss = project.createDokkaSourceSetSpec("foo")

            val srcDir = project.file("src/main/kotlin").apply { mkdirs() }
            val packageDir = srcDir.resolve("com/example").apply { mkdirs() }
            val file1 = packageDir.resolve("File1.kt").apply { writeText("class File1") }
            val subPackageDir = packageDir.resolve("sub").apply { mkdirs() }
            val file2 = subPackageDir.resolve("File2.kt").apply { writeText("class File2") }
            val file3 = subPackageDir.resolve("File3.kt").apply { writeText("class File3") }

            dss.sourceRoots.from(srcDir)
            dss.suppressedFiles.from(file2)
            dss.suppressGeneratedFiles.set(false)

            dss.inputSourceFiles.shouldContainExactly(file1, file3)
        }

        test("should exclude entire suppressed directories") {
            val project = createProject()
            val dss = project.createDokkaSourceSetSpec("foo")

            val srcDir = project.file("src/main/kotlin").apply { mkdirs() }
            val file1 = srcDir.resolve("File1.kt").apply { writeText("class File1") }
            val suppressedDir = srcDir.resolve("suppressed").apply { mkdirs() }
            suppressedDir.resolve("File2.kt").apply { writeText("class File2") }
            suppressedDir.resolve("File3.kt").apply { writeText("class File3") }

            dss.sourceRoots.from(srcDir)
            dss.suppressedFiles.from(suppressedDir)
            dss.suppressGeneratedFiles.set(false)

            dss.inputSourceFiles.shouldContainExactly(file1)
        }

        test("should contain only files from deeply nested directory structures") {
            val project = createProject()
            val dss = project.createDokkaSourceSetSpec("foo")

            val srcDir = project.file("src/main/kotlin").apply { mkdirs() }
            val level1 = srcDir.resolve("com").apply { mkdirs() }
            val level2 = level1.resolve("example").apply { mkdirs() }
            val level3 = level2.resolve("domain").apply { mkdirs() }
            val level4 = level3.resolve("model").apply { mkdirs() }
            val level5 = level4.resolve("internal").apply { mkdirs() }

            val file1 = level1.resolve("Root.kt").apply { writeText("class Root") }
            val file2 = level3.resolve("Domain.kt").apply { writeText("class Domain") }
            val file3 = level5.resolve("Internal.kt").apply { writeText("class Internal") }

            dss.sourceRoots.from(srcDir)
            dss.suppressGeneratedFiles.set(false)

            dss.inputSourceFiles.shouldContainExactly(file1, file2, file3)
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
