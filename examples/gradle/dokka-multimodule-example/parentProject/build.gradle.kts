/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URL

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

// You can apply and configure Dokka in each subproject
// individially or configure all subprojects at once
subprojects {
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            documentedVisibilities.set(setOf(
                Visibility.PUBLIC,
                Visibility.PROTECTED
            ))

            // Read docs for more details: https://kotlinlang.org/docs/dokka-gradle.html#source-link-configuration
            sourceLink {
                val exampleDir = "https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-multimodule-example"

                localDirectory.set(rootProject.projectDir)
                remoteUrl.set(URL("$exampleDir"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}

// Configures only the parent MultiModule task,
// this will not affect subprojects
tasks.dokkaHtmlMultiModule {
    moduleName.set("Dokka MultiModule Example")
}
