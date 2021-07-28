plugins {
    /**
     * Kotlin plugin necessary because of potential Gradle bug!
     * This is not necessary if the kotlin gradle plugin is added as buildscript
     * dependency like
     *
     * buildscript {
     *     dependencies {
     *         classpath("org.jetbrains.kotlin:kotlin-gradle-plugin")
     *     }
     * }
     */
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(kotlin("stdlib"))
}

