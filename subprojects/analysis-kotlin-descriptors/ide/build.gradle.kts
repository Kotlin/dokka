plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    compileOnly(projects.core)
    compileOnly(projects.subprojects.analysisKotlinApi)

    implementation(projects.subprojects.analysisKotlinDescriptors.compiler)

    api(libs.kotlin.idePlugin.common)
    api(libs.kotlin.idePlugin.idea)
    api(libs.kotlin.idePlugin.core)
    api(libs.kotlin.idePlugin.native)

    // TODO [beresnev] needed for CommonIdePlatformKind, describe
    implementation(libs.kotlin.jps.common)

    // TODO [beresnev] get rid of it
    compileOnly(libs.kotlinx.coroutines.core)
}
