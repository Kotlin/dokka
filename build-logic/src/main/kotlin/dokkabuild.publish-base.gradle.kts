/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.base")
    `maven-publish`
    signing
    id("dokkabuild.dev-maven-publish")
}

publishing {
    repositories {
        maven {
            name = "spaceDev"
            url = uri("https://packages.jetbrains.team/maven/p/kt/dokka-dev")
            credentials {
                username = System.getenv("DOKKA_SPACE_PACKAGES_USER")
                password = System.getenv("DOKKA_SPACE_PACKAGES_SECRET")
            }
        }
        maven {
            name = "spaceTest"
            url = uri("https://packages.jetbrains.team/maven/p/kt/dokka-test")
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
