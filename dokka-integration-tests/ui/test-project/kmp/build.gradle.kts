plugins {
    id("uitest.dokka")

    kotlin("multiplatform")
}

kotlin {
    jvm()
    linuxX64()
    macosX64()
    js()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
        }
    }
}
