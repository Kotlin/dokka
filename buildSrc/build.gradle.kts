plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.github.johnrengelman:shadow:8.1.0")
    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:0.13.0")
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
}
