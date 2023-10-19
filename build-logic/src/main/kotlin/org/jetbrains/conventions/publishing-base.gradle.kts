/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.conventions

plugins {
    `maven-publish`
    signing
}

// TODO: recheck ENV variables - may be replace them with dokka specific renaming to DOKKA_*

publishing {
    repositories {
        maven {
            name = "mavenCentral"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                // TODO: recheck credentials
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
        maven {
            // TODO: recheck if we need it at all
            name = "snapshot"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            credentials {
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
        maven {
            name = "spaceDev"
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
            credentials {
                username = System.getenv("SPACE_PACKAGES_USER")
                password = System.getenv("SPACE_PACKAGES_SECRET")
            }
        }
        maven {
            name = "spaceTest"
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/dokka/test")
            credentials {
                // TODO: should it be different credentials from dev?
                username = System.getenv("SPACE_PACKAGES_USER")
                password = System.getenv("SPACE_PACKAGES_SECRET")
            }
        }
        // Publish to a project-local Maven directory, for verification. To test, run:
        // ./gradlew publishAllPublicationsToProjectLocalRepository
        // and check $rootDir/build/maven-project-local
        maven {
            name = "projectLocal"
            url = uri(rootProject.layout.buildDirectory.dir("maven-project-local"))
        }
    }

    publications.withType<MavenPublication>().configureEach {
        pom {
            name.convention("Dokka ${project.name}")
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

signing {
    useInMemoryPgpKeys(
        System.getenv("SIGN_KEY_ID")?.takeIf(String::isNotBlank),
        System.getenv("SIGN_KEY")?.takeIf(String::isNotBlank),
        System.getenv("SIGN_KEY_PASSPHRASE")?.takeIf(String::isNotBlank),
    )
    sign(publishing.publications)
    setRequired(provider { !project.version.toString().endsWith("-SNAPSHOT") })
}
