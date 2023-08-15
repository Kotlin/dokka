import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.subprojects.analysisKotlinApi)

    implementation(projects.plugins.base)
    implementation(projects.plugins.kotlinAsJava)

    implementation(kotlin("reflect"))
    implementation(libs.soywiz.korte)
    implementation(libs.kotlinx.html)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.plugins.base.baseTestUtils)
    testImplementation(projects.core.testApi)
    testImplementation(libs.jsoup)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

registerDokkaArtifactPublication("javadoc-plugin")
