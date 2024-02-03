/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import dokkabuild.PublicationName

plugins {
    id("dokkabuild.publish-base")
    id("com.gradle.plugin-publish")
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    website = "https://kotl.in/dokka"
    vcsUrl = "https://github.com/kotlin/dokka.git"
}

// com.gradle.plugin-publish configures publication in afterEvaluate block
// so to be able to configure it directly in build scripts (f.e. to change artifactId) we need to register it earlier
// more info: https://docs.gradle.org/current/userguide/java_gradle_plugin.html#maven_publish_plugin
publishing.publications.register<MavenPublication>(PublicationName.GRADLE_PLUGIN)

// com.gradle.plugin-publish configures javadoc only for the main plugin artifact,
// so we need to link it manually to other publications
// specifically with artifact `org.jetbrains.dokka.gradle.plugin`
// which is used to resolve plugins via `plugins { id("org.jetbrains.dokka") }`
// it's not needed for gradle plugin portal, but needed for Maven Central
// NOTE: it should be configured in `afterEvaluate`
//       because `javadocJar` task is created in `afterEvaluate` block in `com.gradle.plugin-publish` plugin
afterEvaluate {
    publishing.publications.withType<MavenPublication>()
        .matching { it.name != PublicationName.GRADLE_PLUGIN }
        .configureEach {
            artifact(tasks.named("javadocJar"))
        }
}
