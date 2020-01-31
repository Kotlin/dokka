package org.jetbrains

import com.jfrog.bintray.gradle.BintrayExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate

fun Project.configureBintrayPublication(vararg publications: String) {
    val dokka_version: String by this
    val dokka_publication_channel: String by this
    extensions.configure<BintrayExtension>("bintray") {
        user = System.getenv("BINTRAY_USER")
        key = System.getenv("BINTRAY_KEY")

        pkg = PackageConfig().apply {
            repo = dokka_publication_channel
            name = "dokka"
            userOrg = "kotlin"
            desc = "Dokka, the Kotlin documentation tool"
            vcsUrl = "https://github.com/kotlin/dokka.git"
            setLicenses("Apache-2.0")
            version = VersionConfig().apply {
                name = dokka_version
            }
        }
        setPublications(*publications)
    }
}