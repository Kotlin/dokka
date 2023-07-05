import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)

    implementation(projects.plugins.base)
    implementation(projects.plugins.gfm)
    implementation(projects.plugins.allModulesPage)
    implementation(projects.plugins.templating)

    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

registerDokkaArtifactPublication("dokkaGfmTemplateProcessing") {
    artifactId = "gfm-template-processing-plugin"
}
