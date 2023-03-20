plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    api(projects.testUtils)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
    implementation(libs.eclipse.jgit)
}
