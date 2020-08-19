package org.jetbrains

import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.*
import org.jetbrains.DokkaPublicationChannel.*
import java.net.URI

class DokkaPublicationBuilder {
    enum class Component {
        Java, Shadow
    }

    var artifactId: String? = null
    var component: Component = Component.Java
}


fun Project.registerDokkaArtifactPublication(publicationName: String, configure: DokkaPublicationBuilder.() -> Unit) {
    configure<PublishingExtension> {
        publications {
            register<MavenPublication>(publicationName) {
                val builder = DokkaPublicationBuilder().apply(configure)
                this.artifactId = builder.artifactId
                when (builder.component) {
                    DokkaPublicationBuilder.Component.Java -> from(components["java"])
                    DokkaPublicationBuilder.Component.Shadow -> run {
                        artifact(tasks["sourcesJar"])
                        extensions.getByType(ShadowExtension::class.java).component(this)
                    }
                }
            }
        }
    }
    configureBintrayPublicationIfNecessary(publicationName)
    configureSpacePublicationIfNecessary(publicationName)
    createDokkaPublishTaskIfNecessary()
}

fun Project.configureSpacePublicationIfNecessary(vararg publications: String) {
    if (publicationChannel.isSpaceRepository) {
        configure<PublishingExtension> {
            repositories {
                /* already registered */
                findByName(SpaceDokkaDev.name)?.let { return@repositories }
                maven {
                    name = SpaceDokkaDev.name
                    url = URI.create("https://maven.pkg.jetbrains.space/kotlin/p/dokka/dev")
                    credentials {
                        username = System.getenv("SPACE_PACKAGES_USER")
                        password = System.getenv("SPACE_PACKAGES_SECRET")
                    }
                }
            }
        }
    }

    whenEvaluated {
        tasks.withType<PublishToMavenRepository> {
            val isSpaceRepository = this.repository.name == SpaceDokkaDev.name
            if (!isSpaceRepository) return@withType
            this.isEnabled = this.isEnabled && publication.name in publications
            if (!this.isEnabled) {
                this.group = "disabled"
            }
        }
    }
}

fun Project.createDokkaPublishTaskIfNecessary() {
    tasks.maybeCreate("dokkaPublish").run {
        if (publicationChannel.isSpaceRepository) {
            dependsOn(tasks.named("publish"))
        }
        if (publicationChannel.isBintrayRepository) {
            dependsOn(tasks.named("bintrayUpload"))
        }
    }
}

fun Project.configureBintrayPublicationIfNecessary(vararg publications: String) {
    if (publicationChannel.isBintrayRepository) {
        configureBintrayPublication(*publications)
    }
}

private fun Project.configureBintrayPublication(vararg publications: String) {
    extensions.configure<BintrayExtension>("bintray") {
        user = System.getenv("BINTRAY_USER")
        key = System.getenv("BINTRAY_KEY")
        dryRun = System.getenv("BINTRAY_DRY_RUN") == "true" ||
                project.properties["bintray_dry_run"] == "true"
        pkg = PackageConfig().apply {
            repo = when (publicationChannel) {
                SpaceDokkaDev -> throw IllegalStateException("$SpaceDokkaDev is not a bintray repository")
                BintrayKotlinDev -> "kotlin-dev"
                BintrayKotlinEap -> "kotlin-eap"
                BintrayKotlinDokka -> "dokka"
            }
            name = "dokka"
            userOrg = "kotlin"
            desc = "Dokka, the Kotlin documentation tool"
            vcsUrl = "https://github.com/kotlin/dokka.git"
            setLicenses("Apache-2.0")
            version = VersionConfig().apply {
                name = dokkaVersion
            }
        }
        setPublications(*publications)
    }
}



