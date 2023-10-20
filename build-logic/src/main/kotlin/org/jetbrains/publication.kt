/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class DokkaPublicationBuilder {
    enum class Component {
        Java, Shadow
    }

    var artifactId: String? = null
    var component: Component = Component.Java
}

internal const val MAVEN_JVM_PUBLICATION_NAME = "jvm"

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
