package org.jetbrains.conventions

import org.gradle.kotlin.dsl.invoke
import org.jetbrains.isLocalPublication

plugins {
    id("org.jetbrains.dokka")
}

tasks.dokkaHtml {
    onlyIf { !isLocalPublication }
    outputDirectory.set(layout.buildDirectory.dir("dokka").map { it.asFile })
}
