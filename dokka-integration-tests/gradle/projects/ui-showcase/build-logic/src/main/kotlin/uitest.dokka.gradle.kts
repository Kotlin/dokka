/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.intellij.lang.annotations.Language
import org.jetbrains.dokka.gradle.AbstractDokkaTask

plugins {
    id("org.jetbrains.dokka")
}

tasks.withType<AbstractDokkaTask>().configureEach {
    @Language("JSON")
    val dokkaBaseConfiguration = """
    {
      "footerMessage": "(c) 2024 My footer message",
      "homepageLink": "https://github.com/Kotlin/dokka/tree/master/dokka-integration-tests/ui/test-project"
    }
    """.trimIndent()
    pluginsMapConfiguration.set(
        mapOf(
            // fully qualified plugin name to json configuration
            "org.jetbrains.dokka.base.DokkaBase" to dokkaBaseConfiguration
        )
    )
}
