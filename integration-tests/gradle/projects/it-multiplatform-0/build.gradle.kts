import org.jetbrains.dokka.gradle.dokka

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

apply(from = "../template.root.gradle.kts")

kotlin {
    jvm()
    linuxX64("linux")
    macosX64("macos")
    js()
}

dokka {
    dokkaSourceSets {
        create("commonMain")
        create("jvmMain")
        create("linuxMain")
        create("macosMain")
        create("jsMain")
    }
}
