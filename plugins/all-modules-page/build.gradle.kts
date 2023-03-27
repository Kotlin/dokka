import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

registerDokkaArtifactPublication("dokkaAllModulesPage") {
    artifactId = "all-modules-page-plugin"
}

dependencies {
    compileOnly(projects.core)
    implementation(kotlin("reflect"))

    compileOnly(projects.kotlinAnalysis)
    implementation(projects.plugins.base)
    implementation(projects.plugins.templating)
    testImplementation(projects.plugins.base)
    testImplementation(projects.plugins.base.baseTestUtils)
    testImplementation(projects.plugins.gfm)
    testImplementation(projects.plugins.gfm.gfmTemplateProcessing)
    testImplementation(projects.core.contentMatcherTestUtils)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jackson.kotlin)
    constraints {
        implementation(libs.jackson.databind) {
            because("CVE-2022-42003")
        }
    }
    implementation(libs.kotlinx.html)
    implementation(libs.jsoup)

    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}
