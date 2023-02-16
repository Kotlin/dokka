plugins {
    kotlin("jvm") version "1.8.10"
    id("org.jetbrains.dokka") version ("1.7.20") apply false
}

// The versioning plugin must be applied in all submodules
subprojects {
    repositories {
        mavenCentral()
    }
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.dokka")
    }
    val dokkaPlugin by configurations
    dependencies {
        dokkaPlugin("org.jetbrains.dokka:versioning-plugin:1.7.20")
    }
}
