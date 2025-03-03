/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("uitest.dokka")

    kotlin("jvm")
}

dokka {
    dokkaPublications.html {
        includes.setFrom("description.md")
        suppressObviousFunctions.set(false)
        suppressInheritedMembers.set(false)
    }

    dokkaSourceSets.configureEach {
        skipEmptyPackages.set(false)

        sourceLink {
            localDirectory.set(projectDir.resolve("src"))
            remoteUrl("https://github.com/kotlin/dokka/dokka-integration-tests/ui/test-project/jvm/src")
        }
    }
}
