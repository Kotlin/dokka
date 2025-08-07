/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.utils.enableV2Plugin

class ModulePathTests : FunSpec({
    context("given multi-module project") {
        val rootProject = projectWithDgpV2(name = "my-root")
        val subProject1 = projectWithDgpV2(name = "sp1", parent = rootProject)
        val subProject2 = projectWithDgpV2(name = "sp2", parent = subProject1)
        val subProject3 = projectWithDgpV2(name = "sp3", parent = subProject2)

        test("expect rootProject modulePath defaults to rootProject name") {
            rootProject.dokkaExtension.modulePath.orNull shouldBe "my-root"
        }
        test("expect subproject1 modulePath defaults to project path") {
            subProject1.dokkaExtension.modulePath.orNull shouldBe "sp1"
        }
        test("expect subProject2 modulePath defaults to project path") {
            subProject2.dokkaExtension.modulePath.orNull shouldBe "sp1/sp2"
        }
        test("expect subProject3 modulePath defaults to project path") {
            subProject3.dokkaExtension.modulePath.orNull shouldBe "sp1/sp2/sp3"
        }
    }
})

private fun projectWithDgpV2(
    name: String,
    parent: Project? = null,
): Project =
    ProjectBuilder.builder()
        .withName(name)
        .apply {
            if (parent != null) withParent(parent)
        }
        .build()
        .enableV2Plugin()
        .also { it.plugins.apply("org.jetbrains.dokka") }

private val Project.dokkaExtension: DokkaExtension
    get() = extensions.getByType()
