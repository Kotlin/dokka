package org.jetbrains.conventions

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import org.jetbrains.DokkaPublicationChannel
import org.jetbrains.DokkaPublicationChannel.MavenProjectLocal
import org.jetbrains.DokkaPublicationChannel.SpaceDokkaDev
import org.jetbrains.DokkaVersionType

plugins {
    id("org.jetbrains.conventions.base")
    `maven-publish`
    signing
    id("org.jetbrains.conventions.dokka")
}

val javadocJar by tasks.registering(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles a Javadoc JAR using Dokka HTML"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

publishing {
    repositories {
        // Publish to a project-local Maven directory, for verification. To test, run:
        // ./gradlew publishAllPublicationsToMavenProjectLocalRepository
        // and check $rootDir/build/maven-project-local
        maven(rootProject.layout.buildDirectory.dir("maven-project-local")) {
            name = MavenProjectLocal.repositoryName
        }

        maven("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev") {
            name = SpaceDokkaDev.repositoryName
            credentials {
                username = System.getenv("SPACE_PACKAGES_USER")
                password = System.getenv("SPACE_PACKAGES_SECRET")
            }
        }
    }

    publications.withType<MavenPublication>().configureEach {
        artifact(javadocJar)

        pom {
            name.convention(provider { "Dokka ${project.name}" })
            description.convention("Dokka is an API documentation engine for Kotlin and Java, performing the same function as Javadoc for Java")
            url.convention("https://github.com/Kotlin/dokka")

            licenses {
                license {
                    name.convention("The Apache Software License, Version 2.0")
                    url.convention("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.convention("repo")
                }
            }

            developers {
                developer {
                    id.convention("JetBrains")
                    name.convention("JetBrains Team")
                    organization.convention("JetBrains")
                    organizationUrl.convention("https://www.jetbrains.com")
                }
            }

            scm {
                connection.convention("scm:git:git://github.com/Kotlin/dokka.git")
                url.convention("https://github.com/Kotlin/dokka/tree/master")
            }
        }
    }
}

plugins.withType<ShadowPlugin>().configureEach {
    // manually disable publication of Shadow elements https://github.com/johnrengelman/shadow/issues/651#issue-839148311
    // This is done to preserve compatibility and have the same behaviour as previous versions of Dokka.
    // For more details, see https://github.com/Kotlin/dokka/pull/2704#issuecomment-1499517930
    val javaComponent = components["java"] as AdhocComponentWithVariants
    javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    val artifactVersion = provider { DokkaVersionType.from(publication?.version) }
    val artifactCoords = provider { publication?.run { "$groupId:$artifactId:$version" }.toString() }

    // add an `onlyIf {}` check for each possibly enabled channel
    DokkaPublicationChannel.values().forEach { channel ->
        val channelRepoName = channel.repositoryName

        val channelEnabledPredicate = provider {
            // is this task publishing to $channel?
            val publishingToChannel = repository == publishing.repositories.findByName(channelRepoName)

            if (!publishingToChannel) {
                // this check isn't applicable for the repository used for this task, so don't block publication
                true
            } else {
                // only allow publication if $channel one of the enabled publication channels
                channel in dokkaBuild.publicationChannels
            }
        }

        onlyIf("given this task is publishing to $channelRepoName, then $channelRepoName must be enabled") {
            channelEnabledPredicate.get().also { enabled ->
                val gav = artifactCoords.get()
                logger.lifecycle("$path - publishing $gav to $channelRepoName is ${if (enabled) "enabled" else "disabled"}")
            }
        }


        val compatibleVersionPredicate = provider {
            // is this task publishing to $channel?
            val publishingToChannel = repository == publishing.repositories.findByName(channelRepoName)

            if (!publishingToChannel) {
                // this check isn't applicable for the repository used for this task, so don't block publication
                true
            } else {
                // only allow publication if $channel one of the enabled publication channels
                channel.acceptedDokkaVersionTypes.any {acceptedVersionType ->
                    acceptedVersionType == artifactVersion.orNull
                }
            }
        }

        onlyIf("given this task is publishing to $channelRepoName, then the version must be compatible") {
            compatibleVersionPredicate.get()
        }
    }
}

val dokkaPublish by tasks.registering {
    description = "Lifecycle task for running all enabled Dokka Publication tasks for remote repositories"
    dependsOn("publish", "publishToSonatype", "publishPlugins")
}

val signingKey = dokkaBuild.signingKey.orNull

if (!signingKey.isNullOrBlank()) {
    val signingKeyId = dokkaBuild.signingKeyId.orNull
    val signingKeyPassphrase = dokkaBuild.signingKeyPassphrase.orNull

    extensions.configure<SigningExtension> {
        if (!signingKeyId.isNullOrBlank()) {
            useInMemoryPgpKeys(signingKeyId, signingKey, signingKeyPassphrase)
        } else {
            useInMemoryPgpKeys(signingKey, signingKeyPassphrase)
        }

        // The signing plugin is not fully compatible with modern Gradle features.
        // As a workaround, use afterEvaluate {}
        afterEvaluate {
            sign(extensions.getByType<PublishingExtension>().publications)
        }
    }
}
