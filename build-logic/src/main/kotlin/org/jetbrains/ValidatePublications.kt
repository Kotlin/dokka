package org.jetbrains

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.SetProperty
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.findByType

/**
 * Verifies that a subproject's publications have a version that is compatible with the targeted repository.
 * This is to prevent publishing `1.8.20-dev` to Maven Central, or `1.8.20-SNAPSHOT` to Space.
 *
 * See https://github.com/Kotlin/dokka/issues/2703#issuecomment-1499599816
 */
abstract class ValidatePublications : DefaultTask() {

    @get:Input
    abstract val publicationChannels: SetProperty<DokkaPublicationChannel>

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
        val versionTypeMatchesPublicationChannels = publicationChannels.get().all { publicationChannel ->
            publicationChannel.acceptedDokkaVersionTypes.any { acceptedVersionType ->
                acceptedVersionType == dokkaVersionType
            }
        }
        if (!versionTypeMatchesPublicationChannels) {
            throw AssertionError("Wrong version $dokkaVersion for configured publication channels $publicationChannels")
        }
    }
}
