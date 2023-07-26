import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.subprojects.analysisKotlinApi)

    implementation(projects.plugins.base)

    implementation(kotlin("reflect"))

    testImplementation(libs.jsoup)
    testImplementation(projects.plugins.base)
    testImplementation(projects.plugins.base.baseTestUtils)
    testImplementation(projects.core.contentMatcherTestUtils)
    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(project(path = ":subprojects:analysis-kotlin-descriptors", configuration = "shadow"))
}

registerDokkaArtifactPublication("kotlinAsJavaPlugin") {
    artifactId = "kotlin-as-java-plugin"
}
