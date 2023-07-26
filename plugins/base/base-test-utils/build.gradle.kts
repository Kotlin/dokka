import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.plugins.base)

    api(projects.subprojects.analysisKotlinApi)

    // TODO [beresnev] analysis switcher
    //runtimeOnly(project(path = ":subprojects:analysis-kotlin-symbols", configuration = "shadow"))
    //runtimeOnly(project(path = ":subprojects:analysis-kotlin-descriptors", configuration = "shadow"))

    implementation(kotlin("reflect"))

    implementation(projects.core.testApi)

    implementation(libs.jsoup)
    implementation(kotlin("test-junit"))

    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

registerDokkaArtifactPublication("dokkaBaseTestUtils") {
    artifactId = "dokka-base-test-utils"
}
