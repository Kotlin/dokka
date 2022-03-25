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
                    checkProjectDependenciesArePublished(subProject)
                    subProject.assertPublicationVersion()
                }
        }
    }

    private fun checkProjectDependenciesArePublished(project: Project) {
        val implementationDependencies = project.findDependenciesByName("implementation")
        val apiDependencies = project.findDependenciesByName("api")

        val allDependencies = implementationDependencies + apiDependencies

        allDependencies
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

    private fun Project.findDependenciesByName(name: String): Set<Dependency> {
        return configurations.findByName(name)?.allDependencies.orEmpty()
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

    private class UnpublishedProjectDependencyException(
        project: Project, dependencyProject: Project
    ): GradleException(
        "Published project ${project.path} cannot depend on unpublished project ${dependencyProject.path}"
    )
}
