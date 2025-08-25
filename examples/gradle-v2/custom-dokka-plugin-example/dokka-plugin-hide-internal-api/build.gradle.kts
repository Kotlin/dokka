plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    val dokkaVersion = "2.1.0-Beta"
    compileOnly("org.jetbrains.dokka:dokka-core:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-base:$dokkaVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}
