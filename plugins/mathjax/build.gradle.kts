import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)
    implementation(kotlin("reflect"))
    implementation(projects.plugins.base)

    testImplementation(libs.jsoup)
    testImplementation(projects.plugins.base.baseTestUtils)
    testImplementation(projects.core.contentMatcherTestUtils)
    testImplementation(kotlin("test-junit"))
    testImplementation(projects.kotlinAnalysis)

    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

registerDokkaArtifactPublication("mathjaxPlugin") {
    artifactId = "mathjax-plugin"
}
