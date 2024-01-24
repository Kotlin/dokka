/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dokkabuild

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

object PublicationName {
    const val JVM = "jvm"
    const val GRADLE_PLUGIN = "pluginMaven"
}

fun Project.overridePublicationArtifactId(
    artifactId: String,
    publicationName: String = PublicationName.JVM
) {
    extensions.configure<PublishingExtension> {
        publications.withType<MavenPublication>().named(publicationName) {
            this.artifactId = artifactId
        }
    }
}
