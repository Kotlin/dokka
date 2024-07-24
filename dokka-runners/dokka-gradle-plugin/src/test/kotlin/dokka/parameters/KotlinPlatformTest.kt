/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.dokka.parameters

import io.kotest.core.spec.style.FunSpec
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.gradle.dokka.parameters.KotlinPlatform.Companion.dokkaType

class KotlinPlatformTest : FunSpec({

    test("should have same default as Dokka type") {
        KotlinPlatform.DEFAULT.dokkaType shouldBe Platform.DEFAULT
    }

    test("Dokka platform should have equivalent KotlinPlatform") {

        Platform.values().shouldForAll { dokkaPlatform ->
            dokkaPlatform shouldBeIn KotlinPlatform.values.map { it.dokkaType }
        }
    }

    test("platform strings should map to same KotlinPlatform and Platform") {
        listOf(
            "androidJvm",
            "android",
            "metadata",
            "jvm",
            "js",
            "wasm",
            "native",
            "common",
        ).shouldForAll {
            Platform.fromString(it) shouldBe KotlinPlatform.fromString(it).dokkaType
        }
    }
})
