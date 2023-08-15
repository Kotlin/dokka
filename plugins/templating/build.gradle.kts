import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

registerDokkaArtifactPublication("templating-plugin")

dependencies {
    compileOnly(projects.core)

    api(libs.jsoup)

    implementation(projects.plugins.base)

    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.plugins.base.baseTestUtils)
    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.html)
}
