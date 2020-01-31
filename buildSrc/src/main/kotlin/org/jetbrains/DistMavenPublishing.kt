package org.jetbrains

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

fun Project.configureDistMaven() { // TODO: This can probably be written cleaner
    val repoLocation = uri(file("${rootProject.buildDir}/dist-maven"))
    var distMaven: MavenArtifactRepository? = null
    pluginManager.withPlugin("maven-publish") {
        this@configureDistMaven.extensions.findByType(PublishingExtension::class.java)?.repositories {
            distMaven = maven {
                name = "distMaven"
                url = repoLocation
            }
        }
    }
    tasks.register("publishToDistMaven") {
        group = "publishing"
        description = "Publishes all Maven publications to Maven repository 'distMaven'"
        dependsOn(tasks.withType(PublishToMavenRepository::class.java).matching {
            it.repository == distMaven
        })
    }
}