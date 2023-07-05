plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.subprojects.analysisKotlinApi)

    // TODO

    // TODO [beresnev] get rid of it
    compileOnly(libs.kotlinx.coroutines.core)
}
