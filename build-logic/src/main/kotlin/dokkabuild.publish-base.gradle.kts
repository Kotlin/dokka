/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import java.net.URI

plugins {
    id("dokkabuild.base")
    `maven-publish`
    signing
    id("dokkabuild.dev-maven-publish")
}

publishing {
    repositories {
        maven {
            name = "mavenCentral"
            url = mavenCentralRepositoryUri()
            credentials {
                username = System.getenv("DOKKA_SONATYPE_USER")
                password = System.getenv("DOKKA_SONATYPE_PASSWORD")
            }
        }
        maven {
            name = "spaceDev"
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
            credentials {
                username = System.getenv("DOKKA_SPACE_PACKAGES_USER")
                password = System.getenv("DOKKA_SPACE_PACKAGES_SECRET")
            }
        }
        maven {
            name = "spaceTest"
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/dokka/test")
            credentials {
                username = System.getenv("DOKKA_SPACE_PACKAGES_USER")
                password = System.getenv("DOKKA_SPACE_PACKAGES_SECRET")
            }
        }
    }

    publications.withType<MavenPublication>().configureEach {
        pom {
            name.convention("Dokka ${project.name}")
            description.convention("Dokka is an API documentation engine for Kotlin")
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
                url.convention("https://github.com/Kotlin/dokka")
            }
        }
    }
}

/**
 * Due to Gradle running publishing tasks in parallel, multiple staging repositories
 * can be created within a couple of seconds with all artifact files scattered throughout them.
 *
 * While Gradle's parallelism can be disabled, the simplest and most reliable option is
 * to just publish to a pre-defined staging repository.
 */
fun mavenCentralRepositoryUri(): URI {
    val repositoryId: String? = System.getenv("DOKKA_MVN_CENTRAL_REPOSITORY_ID")
    return if (repositoryId.isNullOrBlank()) {
        URI("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
    } else {
        URI("https://oss.sonatype.org/service/local/staging/deployByRepositoryId/$repositoryId")
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("DOKKA_SIGN_KEY_ID")?.takeIf(String::isNotBlank),
        System.getenv("DOKKA_SIGN_KEY")?.takeIf(String::isNotBlank),
        System.getenv("DOKKA_SIGN_KEY_PASSPHRASE")?.takeIf(String::isNotBlank),
    )
    sign(publishing.publications)
    // no signing should be required for locally published artifacts,
    // as they are used for manual testing and running integration tests only
    setRequired(provider { gradle.taskGraph.allTasks.any { it is PublishToMavenRepository } })
}

// This is a hack for a Gradle 8 problem, see https://github.com/gradle/gradle/issues/26091
//
// Fails with the following error otherwise:
// > Task ':runner-gradle-plugin-classic:publishDokkaPluginMarkerMavenPublicationToSpaceTestRepository' uses
// > this output of task ':runner-gradle-plugin-classic:signPluginMavenPublication' without declaring an
// > explicit or implicit dependency.
tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}
