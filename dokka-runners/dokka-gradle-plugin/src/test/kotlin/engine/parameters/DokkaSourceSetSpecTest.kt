/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.newInstance
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.dokka.gradle.utils.enableV2Plugin

class DokkaSourceSetSpecTest : FunSpec({

    val project = ProjectBuilder.builder().build()
        .enableV2Plugin()

    context("deprecations") {
        val dss = project.createDokkaSourceSetSpec()

        test("expect noStdlibLink === enableKotlinStdLibDocumentationLink") {
            @Suppress("DEPRECATION")
            dss.noStdlibLink shouldBeSameInstanceAs dss.enableKotlinStdLibDocumentationLink
        }
        test("expect noAndroidSdkLink === enableAndroidDocumentationLink") {
            @Suppress("DEPRECATION")
            dss.noAndroidSdkLink shouldBeSameInstanceAs dss.enableAndroidDocumentationLink
        }
        test("expect noJdkLink === enableJdkDocumentationLink") {
            @Suppress("DEPRECATION")
            dss.noJdkLink shouldBeSameInstanceAs dss.enableJdkDocumentationLink
        }
    }
}) {

    companion object {
        private fun Project.createDokkaSourceSetSpec(
            name: String = "main",
            configure: DokkaSourceSetSpec.() -> Unit = {}
        ): DokkaSourceSetSpec =
            objects.newInstance(DokkaSourceSetSpec::class, name).apply(configure)
    }
}
