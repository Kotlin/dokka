plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    compileOnly("org.jetbrains.dokka:dokka-core:2.2.0")
    implementation("org.jetbrains.dokka:dokka-base:2.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}
