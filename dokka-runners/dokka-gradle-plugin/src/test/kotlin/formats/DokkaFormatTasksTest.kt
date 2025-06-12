/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.formats

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.DokkaBasePlugin
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.dependencies.FormatDependenciesManager
import org.jetbrains.dokka.gradle.utils.enableV2Plugin

class DokkaFormatTasksTest : FunSpec({

    context("verify format tasks") {
        val project = projectWithDgpV2()

        val dokkaExtension = project.extensions.getByType<DokkaExtension>()

        val formatName = "testFormatName"
        val htmlPublication = dokkaExtension.dokkaPublications.create(formatName)

        val htmlFormatDependencies = FormatDependenciesManager(
            project = project,
            baseDependencyManager = dokkaExtension.baseDependencyManager,
            formatName = formatName,
            objects = project.objects,
        )

        val dokkaTasks = DokkaFormatTasks(
            project = project,
            publication = htmlPublication,
            formatDependencies = htmlFormatDependencies,
            providers = project.providers,
        )

        test("expect lifecycle generate task created") {
            dokkaTasks.lifecycleGenerate.name shouldBe "dokkaGenerateTestFormatName"
            dokkaTasks.lifecycleGenerate.get().description shouldBe "Generate Dokka testFormatName publications"
            dokkaTasks.lifecycleGenerate.get().group shouldBe "dokka"
            dokkaTasks.lifecycleGenerate.get().dependsOn shouldHaveSingleElement dokkaTasks.generatePublication
        }

        test("expect generate publication task created") {
            dokkaTasks.generatePublication.name shouldBe "dokkaGeneratePublicationTestFormatName"
            dokkaTasks.generatePublication.get().description shouldBe "Executes the Dokka Generator, generating a testFormatName publication"
            dokkaTasks.generatePublication.get().group shouldBe "dokka"
            dokkaTasks.generatePublication.get().dependsOn.shouldBeEmpty()
        }

        test("expect generate module task created") {
            dokkaTasks.generateModule.name shouldBe "dokkaGenerateModuleTestFormatName"
            dokkaTasks.generateModule.get().description shouldBe "Executes the Dokka Generator, generating a testFormatName module"
            dokkaTasks.generateModule.get().group shouldBe "dokka"
            dokkaTasks.generateModule.get().dependsOn.shouldBeEmpty()
        }
    }
})

private fun projectWithDgpV2(): Project =
    ProjectBuilder.builder().build()
        .enableV2Plugin()
        .also { it.plugins.apply(DokkaBasePlugin::class) }
