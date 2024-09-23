/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import io.kotest.core.spec.style.FunSpec
import org.gradle.kotlin.dsl.withType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.utils.enableV2MigrationHelpers
import org.jetbrains.dokka.gradle.utils.enableV2Plugin
import org.jetbrains.dokka.gradle.utils.shouldContainExactly

class MigrationHelpersTest : FunSpec({
    context("when multi-module project has DGPv2 with migration helpers enabled") {

        val parentProject = ProjectBuilder.builder()
            .build()
            .enableV2Plugin()
            .enableV2MigrationHelpers()

        val childProject = ProjectBuilder.builder()
            .withParent(parentProject)
            .build()
            .enableV2Plugin()
            .enableV2MigrationHelpers()

        parentProject.plugins.apply("org.jetbrains.dokka")
        childProject.plugins.apply("org.jetbrains.dokka")

        context("in parent project") {
            val parentProjectDokkaTasks = parentProject.tasks.withType<AbstractDokkaTask>()
            test("all DGPv1 tasks should have group 'other'") {
                val dokkaTasks = parentProjectDokkaTasks.associate { it.name to it.group }
                dokkaTasks.shouldContainExactly(
                    "dokkaGfm" to "other",
                    "dokkaGfmCollector" to "other",
                    "dokkaGfmMultiModule" to "other",

                    "dokkaHtml" to "other",
                    "dokkaHtmlCollector" to "other",
                    "dokkaHtmlMultiModule" to "other",

                    "dokkaJavadoc" to "other",
                    "dokkaJavadocCollector" to "other",

                    "dokkaJekyll" to "other",
                    "dokkaJekyllCollector" to "other",
                    "dokkaJekyllMultiModule" to "other",
                )
            }
            test("all DGPv1 tasks should be disabled") {
                val dokkaTasks = parentProjectDokkaTasks.associate { it.name to it.enabled }
                dokkaTasks.shouldContainExactly(
                    "dokkaGfm" to false,
                    "dokkaGfmCollector" to false,
                    "dokkaGfmMultiModule" to false,

                    "dokkaHtml" to false,
                    "dokkaHtmlCollector" to false,
                    "dokkaHtmlMultiModule" to false,

                    "dokkaJavadoc" to false,
                    "dokkaJavadocCollector" to false,

                    "dokkaJekyll" to false,
                    "dokkaJekyllCollector" to false,
                    "dokkaJekyllMultiModule" to false,
                )
            }
        }

        context("in child project") {
            val childProjectDokkaTasks = childProject.tasks.withType<AbstractDokkaTask>()
            test("all DGPv1 tasks should have group 'other'") {
                val dokkaTasks = childProjectDokkaTasks.associate { it.name to it.group }
                dokkaTasks.shouldContainExactly(
                    "dokkaGfm" to "other",
                    "dokkaGfmPartial" to "other",

                    "dokkaHtml" to "other",
                    "dokkaHtmlPartial" to "other",

                    "dokkaJavadoc" to "other",
                    "dokkaJavadocPartial" to "other",

                    "dokkaJekyll" to "other",
                    "dokkaJekyllPartial" to "other",
                )
            }
            test("all DGPv1 tasks should be disabled") {
                val dokkaTasks = childProjectDokkaTasks.associate { it.name to it.enabled }
                dokkaTasks.shouldContainExactly(
                    "dokkaGfm" to false,
                    "dokkaGfmPartial" to false,

                    "dokkaHtml" to false,
                    "dokkaHtmlPartial" to false,

                    "dokkaJavadoc" to false,
                    "dokkaJavadocPartial" to false,

                    "dokkaJekyll" to false,
                    "dokkaJekyllPartial" to false,
                )
            }
        }
    }
})
