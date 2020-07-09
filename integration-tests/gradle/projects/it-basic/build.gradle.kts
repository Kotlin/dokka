import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

apply(from = "../template.root.gradle.kts")

val customDokkaTask by tasks.register<DokkaTask>("customDokka")

dependencies {
    implementation(kotlin("stdlib"))
}


