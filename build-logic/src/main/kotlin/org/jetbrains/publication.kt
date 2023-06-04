package org.jetbrains

import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.DokkaPublicationBuilder.Component.Java
import org.jetbrains.DokkaPublicationBuilder.Component.Shadow

class DokkaPublicationBuilder {
    enum class Component {
        Java, Shadow
    }
}

fun Project.registerDokkaArtifactPublication(
    artifactId: String,
    component: DokkaPublicationBuilder.Component = Java,
) {
    val publicationName = "maven${component.name}"

    configure<PublishingExtension> {
        publications {
            register<MavenPublication>(publicationName) {
                this.artifactId = artifactId
                when (component) {
                    Java -> from(components["java"])

                    Shadow -> {
                        val shadow = extensions.getByType<ShadowExtension>()
                        shadow.component(this)
                        artifact(tasks["sourcesJar"])
                    }
                }
            }
        }
    }
}
