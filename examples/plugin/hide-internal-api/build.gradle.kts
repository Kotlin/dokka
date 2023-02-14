import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "1.8.10"
    id("org.jetbrains.dokka") version "1.7.20"
    `maven-publish`
    signing
}

group = "org.example"
version = "1.4-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

val dokkaVersion: String by project
dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("org.jetbrains.dokka:dokka-core:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-base:$dokkaVersion")

    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.dokka:dokka-test-api:$dokkaVersion")
    testImplementation("org.jetbrains.dokka:dokka-base-test-utils:$dokkaVersion")
}

val dokkaOutputDir = "$buildDir/dokka"

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
    dokkaHtml {
        outputDirectory.set(file(dokkaOutputDir))
    }
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.dokkaHtml)
    archiveClassifier.set("javadoc")
    from(dokkaOutputDir)
}

java {
    withSourcesJar()
}

publishing {
    publications {
        val dokkaTemplatePlugin by creating(MavenPublication::class) {
            artifactId = project.name
            from(components["java"])
            artifact(javadocJar.get())

            pom {
                name.set("Dokka template plugin")
                description.set("This is a plugin template for Dokka")
                url.set("https://github.com/Kotlin/dokka-plugin-template/")

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("JetBrains")
                        name.set("JetBrains Team")
                        organization.set("JetBrains")
                        organizationUrl.set("http://www.jetbrains.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Kotlin/dokka-plugin-template.git")
                    url.set("https://github.com/Kotlin/dokka-plugin-template/tree/master")
                }
            }
        }
        signPublicationsIfKeyPresent(dokkaTemplatePlugin)
    }

    repositories {
        maven {
            url = URI("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PASSWORD")
            }
        }
    }
}

fun Project.signPublicationsIfKeyPresent(publication: MavenPublication) {
    val signingKeyId: String? = System.getenv("SIGN_KEY_ID")
    val signingKey: String? = System.getenv("SIGN_KEY")
    val signingKeyPassphrase: String? = System.getenv("SIGN_KEY_PASSPHRASE")

    if (!signingKey.isNullOrBlank()) {
        extensions.configure<SigningExtension>("signing") {
            if (signingKeyId?.isNotBlank() == true) {
                useInMemoryPgpKeys(signingKeyId, signingKey, signingKeyPassphrase)
            } else {
                useInMemoryPgpKeys(signingKey, signingKeyPassphrase)
            }
            sign(publication)
        }
    }
}
