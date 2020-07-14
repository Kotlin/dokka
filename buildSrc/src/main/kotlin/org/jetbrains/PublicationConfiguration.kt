package org.jetbrains

import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

fun Project.configurePublication(artifactId: String, useShadow: Boolean = false) {
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("maven") {
                this.artifactId = artifactId
                if (useShadow) {
                    val shadow = extensions.getByType(ShadowExtension::class.java)
                    shadow.component(this)
                } else {
                    from(components["java"])
                }
            }
        }
    }
    configureBintrayPublication("maven")
    plugins.all {
        if (this is JavaPlugin) {
            val extension = extensions.getByType(JavaPluginExtension::class.java)
            @Suppress("UnstableApiUsage") extension.withSourcesJar()
        }
    }
}
