/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration

plugins {
    kotlin("jvm") version "1.9.10"
    id("org.jetbrains.dokka") version "1.9.10"
}

repositories {
    mavenCentral()
}

dokka {
    html {
        // Dokka's stylesheets and assets with conflicting names will be overriden.
        // In this particular case, logo-styles.css will be overriden and ktor-logo.png will
        // be added as an additional image asset
        customStyleSheets.from(file("logo-styles.css"))
        customAssets.from(file("ktor-logo.png"))

        // Text used in the footer
        footerMessage.set("(c) Custom Format Dokka example")
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
}
