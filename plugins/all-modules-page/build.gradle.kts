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
    compileOnly(projects.subprojects.analysisKotlinApi)

    implementation(projects.plugins.base)
    implementation(projects.plugins.templating)

    implementation(projects.subprojects.analysisMarkdownJb)

    implementation(libs.kotlinx.html)

    testImplementation(projects.plugins.base)
    testImplementation(projects.plugins.base.baseTestUtils)
    testImplementation(projects.plugins.gfm)
    testImplementation(projects.plugins.gfm.gfmTemplateProcessing)
    testImplementation(projects.core.contentMatcherTestUtils)
    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(project(path = ":subprojects:analysis-kotlin-descriptors", configuration = "shadow"))
}
