import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)


    implementation(kotlin("reflect"))

    compileOnly(projects.plugins.base)
//    implementation(projects.core.testApi)

    implementation(libs.jsoup)
    implementation(kotlin("test-junit"))

    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

registerDokkaArtifactPublication("dokkaBaseTestUtils") {
    artifactId = "dokka-base-test-utils"
}
