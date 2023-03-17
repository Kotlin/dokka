plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    implementation(projects.core.testApi)

    implementation(kotlin("reflect"))
    implementation(libs.assertk)
}
