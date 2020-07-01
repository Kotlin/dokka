import org.jetbrains.dokka.gradle.dokka

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-dev/")
    jcenter()
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

afterEvaluate {
    logger.quiet("Gradle version: ${gradle.gradleVersion}")
    logger.quiet("Kotlin version: ${property("kotlin_version")}")
}
