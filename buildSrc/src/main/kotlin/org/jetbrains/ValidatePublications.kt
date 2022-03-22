package org.jetbrains

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.findByType

open class ValidatePublications : DefaultTask() {

    class UnpublishedProjectDependencyException(
        project: Project, dependencyProject: Project
    ) : GradleException(
        "Published project ${project.path} cannot depend on unpublished project ${dependencyProject.path}"
    )


    @TaskAction
    fun validatePublicationConfiguration() {
        @Suppress("LocalVariableName")
        project.subprojects.forEach { subProject ->
            val publishing = subProject.extensions.findByType<PublishingExtension>() ?: return@forEach
            publishing.publications
                .filterIsInstance<MavenPublication>()
                .filter { it.version == project.dokkaVersion }
                .forEach { publication ->
                    checkProjectDependenciesArePublished(subProject)
                    subProject.assertPublicationVersion()
                }
        }
    }

    private fun checkProjectDependenciesArePublished(project: Project) {
        (project.configurations.findByName("implementation")?.allDependencies.orEmpty() +
                project.configurations.findByName("api")?.allDependencies.orEmpty())
            .filterIsInstance<ProjectDependency>()
            .forEach { projectDependency ->
                val publishing = projectDependency.dependencyProject.extensions.findByType<PublishingExtension>()
                    ?: throw UnpublishedProjectDependencyException(
                        project = project, dependencyProject = projectDependency.dependencyProject
                    )

                val isPublished = publishing.publications.filterIsInstance<MavenPublication>()
                    .any { it.version == project.dokkaVersion }

                if (!isPublished) {
                    throw UnpublishedProjectDependencyException(project, projectDependency.dependencyProject)
                }
            }
    }

    private fun Project.assertPublicationVersion() {
        if (System.getenv("SKIP_VERSION_CHECK")?.contains("true", ignoreCase = true) == true)
            return

        if (!publicationChannels.all { publicationChannel ->
                publicationChannel.acceptedDokkaVersionTypes.any { acceptedVersionType ->
                    acceptedVersionType == dokkaVersionType
                }
            }) {
            throw AssertionError("Wrong version $dokkaVersion for configured publication channels $publicationChannels")
        }
    }

    init {
        group = "verification"
        project.tasks.named("check") {
            dependsOn(this@ValidatePublications)
        }
    }
}
