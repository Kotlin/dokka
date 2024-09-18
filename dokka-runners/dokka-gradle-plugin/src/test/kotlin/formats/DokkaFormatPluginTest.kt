/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.formats

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.utils.enableV2Plugin

class DokkaFormatPluginTest : FunSpec({

    context("version alignment") {

        context("When project has a org.jetbrains.dokka dependency without a version") {
            val project = ProjectBuilder.builder().build()
                .enableV2Plugin()
            project.pluginManager.apply(type = DokkaPlugin::class)

            project.extensions.configure<DokkaExtension> {
                dokkaEngineVersion.convention("1.2.3")
            }

            project.dependencies.add("dokkaPlugin", "org.jetbrains.dokka:xyz")

            test("expect Dokka version defaults to DokkaExtension.versions.dokkaEngine") {
                project.configurations
                    .getByName("dokkaHtmlPluginIntransitiveResolver~internal")
                    .incoming
                    .resolutionResult
                    .root
                    .dependencies
                    .map {
                        // 1.2.3 is not a valid Dokka version, so expect 'unresolved' dependencies,
                        // but that's fine, we only want to check that attempt contains the custom version.
                        it as UnresolvedDependencyResult
                        it.attempted.displayName
                    }
                    .shouldContainExactlyInAnyOrder(
                        "org.jetbrains.dokka:xyz:1.2.3",
                        // DGP adds these dependencies automatically
                        "org.jetbrains.dokka:templating-plugin:1.2.3",
                        "org.jetbrains.dokka:dokka-base:1.2.3",
                    )
            }
        }
    }
})
