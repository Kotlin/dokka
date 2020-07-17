import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

apply(from = "../template.root.gradle.kts")

dependencies {
    implementation(kotlin("stdlib"))
}

tasks.withType<DokkaTask> {
    dokkaSourceSets {
        configureEach {
            moduleDisplayName = "Basic Project"
        }
    }
}
