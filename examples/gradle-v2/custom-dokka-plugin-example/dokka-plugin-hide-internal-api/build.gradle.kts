plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    val dokkaVersion = providers.gradleProperty("dokkaVersion").getOrElse("2.1.0")
    compileOnly("org.jetbrains.dokka:dokka-core:$dokkaVersion")
    implementation("org.jetbrains.dokka:dokka-base:$dokkaVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}
