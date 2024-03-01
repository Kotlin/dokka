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
            implementation(libs.kotlinx.coroutines)
        }
    }
}
