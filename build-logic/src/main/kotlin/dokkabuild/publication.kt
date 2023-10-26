/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dokkabuild

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

internal const val MAVEN_JVM_PUBLICATION_NAME = "jvm"
const val MAVEN_GRADLE_PLUGIN_PUBLICATION_NAME = "pluginMaven"

fun Project.overridePublicationArtifactId(
    artifactId: String,
    publicationName: String = MAVEN_JVM_PUBLICATION_NAME
) {
    extensions.configure<PublishingExtension> {
        publications.withType<MavenPublication>().named(publicationName) {
            this.artifactId = artifactId
        }
    }
}
