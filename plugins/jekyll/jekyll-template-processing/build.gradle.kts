import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)

    implementation(projects.plugins.base)
    implementation(projects.plugins.jekyll)
    implementation(projects.plugins.allModulesPage)
    implementation(projects.plugins.templating)
    implementation(projects.plugins.gfm)
    implementation(projects.plugins.gfm.gfmTemplateProcessing)

    implementation(kotlin("reflect"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

registerDokkaArtifactPublication("dokkaJekyllTemplateProcessing") {
    artifactId = "jekyll-template-processing-plugin"
}
