package org.jetbrains.conventions

import org.gradle.kotlin.dsl.invoke

plugins {
    id("org.jetbrains.dokka")
}

tasks.dokkaHtml {
    // Help improve development & integration test speeds, which publish
    // Dokka to MavenLocal but these tests don't require documentation.
    val localPublicationPredicate = provider {
        gradle.taskGraph.allTasks.any { it is PublishToMavenLocal || it is AbstractTestTask }
    }
    onlyIf("running tests or not publishing to MavenLocal") {
        !localPublicationPredicate.get()
    }
    outputDirectory.set(layout.buildDirectory.dir("dokka"))
}
