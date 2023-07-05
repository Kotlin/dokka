plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.subprojects.analysisKotlinApi)

    api(libs.kotlin.compiler)

    implementation(projects.subprojects.analysisMarkdownJb)
    implementation(projects.subprojects.analysisJavaPsi)

    testImplementation(projects.core.contentMatcherTestUtils)
    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)

    // TODO [beresnev] get rid of it
    compileOnly(libs.kotlinx.coroutines.core)
}
