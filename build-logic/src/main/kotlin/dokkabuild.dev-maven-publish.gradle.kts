/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import dokkabuild.DevMavenPublishExtension
import dokkabuild.DevMavenPublishExtension.Companion.DEV_MAVEN_PUBLISH_EXTENSION_NAME
import dokkabuild.utils.consumable
import dokkabuild.utils.declarable
import dokkabuild.utils.resolvable
import org.gradle.kotlin.dsl.support.uppercaseFirstChar

/**
 * Utility for publishing a project to a local Maven directory for use in integration tests.
 *
 * Using a local directory is beneficial because Maven Local
 * [has some flaws](https://docs.gradle.org/8.6/userguide/declaring_repositories.html#sec:case-for-maven-local),
 * and we can more tightly control what artifacts are published and are persisted.
 *
 * It's possible to publish to a local directory using a regular [PublishToMavenRepository] task,
 * but when the project has a SNAPSHOT version the output will be timestamped, so Gradle will
 * _always_ publish a new artifact. This causes two issues:
 *
 * - The publication tasks, and any test tasks, will _always_ be out-of-date, even if no code changed.
 * - The local directory will endlessly grow in size
 *   (which can be remedied by running `./gradlew clean`, but this is not ideal)
 *
 * To overcome this we manually set the system property `maven.repo.local` to a local directory.
 * Gradle will respect this property, and publish artifacts to the local directory only when
 * they have changed, improving performance.
 */
plugins {
    base
}

/**
 * Directory for the output of the current subproject's 'publishToMavenLocal'
 */
val currentProjectDevMavenRepo = gradle.rootProject.layout.buildDirectory.dir("dev-maven-repo")

val devMavenPublishAttribute = Attribute.of("dev-maven-publish", String::class.java)

dependencies {
    attributesSchema {
        attribute(devMavenPublishAttribute)
    }
}

val publishToDevMavenRepo by tasks.registering {
    description = "Publishes all Maven publications to the dev Maven repository."
    group = PublishingPlugin.PUBLISH_TASK_GROUP
}


plugins.withType<MavenPublishPlugin>().all {
    extensions
        .getByType<PublishingExtension>()
        .publications
        .withType<MavenPublication>().all publication@{
            val publicationName = this@publication.name
            val installTaskName = "publish${publicationName.uppercaseFirstChar()}PublicationToDevMavenRepo"

            // Register a new publication task for each publication.
            val installTask = tasks.register<PublishToMavenLocal>(installTaskName) {
                description = "Publishes Maven publication '$publicationName' to the test Maven repository."
                group = PublishingPlugin.PUBLISH_TASK_GROUP
                publication = this@publication

                val destinationDir = currentProjectDevMavenRepo.get().asFile
                inputs.property("currentProjectDevMavenRepoPath", destinationDir.invariantSeparatorsPath)

                doFirst {
                    /**
                     * `maven.repo.local` will set the destination directory for this [PublishToMavenLocal] task.
                     *
                     * @see org.gradle.api.internal.artifacts.mvnsettings.DefaultLocalMavenRepositoryLocator.getLocalMavenRepository
                     */
                    System.setProperty("maven.repo.local", destinationDir.absolutePath)
                }
            }

            publishToDevMavenRepo.configure {
                dependsOn(installTask)
            }

            tasks.check {
                mustRunAfter(installTask)
            }
        }
}


val devPublication: Configuration by configurations.creating {
    description = "Depend on project-local Dev Maven repositories"
    declarable()
}

val devPublicationResolver: Configuration by configurations.creating {
    description = "Resolve project-local Dev Maven repositories"
    resolvable()
    extendsFrom(devPublication)
    attributes {
        attribute(devMavenPublishAttribute, "devMavenRepo")
    }
}

val devPublicationConsumable: Configuration by configurations.creating {
    description = "Provide project-local Dev Maven repositories dependencies"
    consumable()
    attributes {
        attribute(devMavenPublishAttribute, "devMavenRepo")
    }
    outgoing {
        artifact(currentProjectDevMavenRepo) {
            builtBy(publishToDevMavenRepo)
        }
    }
}

val devMavenPublishExtension = extensions.create<DevMavenPublishExtension>(
    DEV_MAVEN_PUBLISH_EXTENSION_NAME,
    // fetch Dev Maven Repos from the dependencies
    devPublicationResolver.incoming.files,
)
