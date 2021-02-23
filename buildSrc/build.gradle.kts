plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    implementation("com.jfrog.bintray.gradle:gradle-bintray-plugin:1.8.4")
    implementation("com.github.jengelman.gradle.plugins:shadow:2.0.4")
    implementation("io.github.gradle-nexus:publish-plugin:1.0.0")
}
