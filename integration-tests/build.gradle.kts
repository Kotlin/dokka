plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    implementation(kotlin("test-junit"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
    implementation(libs.eclipse.jgit)
}
