import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)
    implementation(kotlin("reflect"))
    implementation(projects.plugins.base)
    testImplementation(projects.plugins.base)
    testImplementation(projects.plugins.base.baseTestUtils)
    implementation(libs.jackson.kotlin)
    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)

    constraints {
        implementation(libs.jackson.databind) {
            because("CVE-2022-42003")
        }
    }
}

registerDokkaArtifactPublication("gfmPlugin") {
    artifactId = "gfm-plugin"
}
