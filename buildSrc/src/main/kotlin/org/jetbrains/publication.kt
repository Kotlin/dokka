package org.jetbrains

import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.jfrog.bintray.gradle.BintrayExtension
import kotlinx.validation.ApiValidationExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
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
            val publicationProvider = register<MavenPublication>(publicationName) {
                val builder = DokkaPublicationBuilder().apply(configure)
                artifactId = builder.artifactId
                when (builder.component) {
                    DokkaPublicationBuilder.Component.Java -> from(components["java"])
                    DokkaPublicationBuilder.Component.Shadow -> run {
                        artifact(tasks["sourcesJar"])
                        extensions.getByType(ShadowExtension::class.java).component(this)
                    }
                }
                configurePom("Dokka ${project.name}")
            }
            signPublicationIfKeyPresent(publicationProvider)
        }
    }

    configureBintrayPublicationIfNecessary(publicationName)
    configureSpacePublicationIfNecessary(publicationName)
    createDokkaPublishTaskIfNecessary()
    registerBinaryCompatibilityCheck(publicationName)
}

fun Project.configureSpacePublicationIfNecessary(vararg publications: String) {
    if (SpaceDokkaDev in this.publicationChannels) {
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
            if (this.repository.name == SpaceDokkaDev.name) {
                this.isEnabled = this.isEnabled && publication.name in publications
                if (!this.isEnabled) {
                    this.group = "disabled"
                }
            }
        }
    }
}

fun Project.createDokkaPublishTaskIfNecessary() {
    tasks.maybeCreate("dokkaPublish").run {
        if (publicationChannels.any { it.isSpaceRepository }) {
            dependsOn(tasks.named("publish"))
        }
        if (publicationChannels.any { it.isBintrayRepository }) {
            dependsOn(tasks.named("bintrayUpload"))
        }
    }
}

fun Project.configureBintrayPublicationIfNecessary(vararg publications: String) {
    if (publicationChannels.any { it.isBintrayRepository }) {
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
            val bintrayPublicationChannels = publicationChannels.filter { it.isBintrayRepository }
            if (bintrayPublicationChannels.size > 1) {
                throw IllegalArgumentException(
                    "Only a single bintray repository can be used for publishing at once. Found $publicationChannels"
                )
            }

            repo = when (bintrayPublicationChannels.single()) {
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


private fun MavenPublication.configurePom(projectName: String) {
    pom {
        name.set(projectName)
        description.set("Dokka is a documentation engine for Kotlin and Java, performing the same function as Javadoc for Java")
        url.set("https://github.com/Kotlin/dokka")

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
            connection.set("scm:git:git://github.com/Kotlin/dokka.git")
            url.set("https://github.com/Kotlin/dokka/tree/master")
        }
    }
}

@Suppress("UnstableApiUsage")
private fun Project.signPublicationIfKeyPresent(publicationProvider: Provider<MavenPublication>) {
    val signingKey = System.getenv("SIGN_KEY")
    val signingKeyPassphrase = System.getenv("SIGN_KEY_PASSPHRASE")

    if (!signingKey.isNullOrBlank()) {
        extensions.configure<SigningExtension>("signing") {
            useInMemoryPgpKeys(signingKey, signingKeyPassphrase)
            sign(publicationProvider.get())
        }
    }
}
