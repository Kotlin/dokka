import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(kotlin("stdlib"))
    dokkaPlugin("org.jetbrains.dokka:sitemap-plugin:${System.getenv("DOKKA_VERSION")}")
}
