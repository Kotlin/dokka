import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

apply(from = "../template.root.gradle.kts")

dependencies {
    dokkaPlugin("my:plugin:version")
    dokkaJavadocPlugin("my:plugin-extending-javadoc:version")
    implementation(kotlin("stdlib"))
}

tasks.dokkaGfm {
    outputDirectory = File(buildDir, "gfm").absolutePath
}

