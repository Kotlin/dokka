import org.jetbrains.*

plugins {
    org.jetbrains.conventions.base
    id("org.jetbrains.dokka")
    id("io.github.gradle-nexus.publish-plugin")
}

val dokka_version: String by project

group = "org.jetbrains.dokka"
version = dokka_version


println("Publication version: $dokka_version")
tasks.register<ValidatePublications>("validatePublications")

nexusPublishing {
    repositories {
        sonatype {
            username.set(System.getenv("SONATYPE_USER"))
            password.set(System.getenv("SONATYPE_PASSWORD"))
        }
    }
}

val dokkaPublish by tasks.registering {
    if (publicationChannels.any { it.isMavenRepository() }) {
        finalizedBy(tasks.named("closeAndReleaseSonatypeStagingRepository"))
    }
}
