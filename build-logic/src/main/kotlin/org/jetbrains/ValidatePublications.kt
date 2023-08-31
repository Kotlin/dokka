/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.findByType

open class ValidatePublications : DefaultTask() {

    init {
        group = "verification"
        project.tasks.named("check") {
            dependsOn(this@ValidatePublications)
        }
    }

    @TaskAction
    fun validatePublicationConfiguration() {
        project.subprojects.forEach { subProject ->
            val publishing = subProject.extensions.findByType<PublishingExtension>() ?: return@forEach
            publishing.publications
                .filterIsInstance<MavenPublication>()
                .filter { it.version == project.dokkaVersion }
                .forEach { _ ->
                    subProject.assertPublicationVersion()
                }
        }
    }

    private fun Project.assertPublicationVersion() {
        val versionTypeMatchesPublicationChannels = publicationChannels.all { publicationChannel ->
            publicationChannel.acceptedDokkaVersionTypes.any { acceptedVersionType ->
                acceptedVersionType == dokkaVersionType
            }
        }
        if (!versionTypeMatchesPublicationChannels) {
            throw AssertionError("Wrong version $dokkaVersion for configured publication channels $publicationChannels")
        }
    }
}
