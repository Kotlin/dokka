plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
}

dependencies {
    compileOnly(projects.core)

    api(libs.intellij.java.psi.api)

    implementation(projects.subprojects.analysisMarkdownJb)

    implementation(libs.intellij.java.psi.impl)
    implementation(libs.intellij.platform.util.api)
    implementation(libs.intellij.platform.util.rt)
    implementation(libs.intellij.platform.jpsModel.impl)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.jsoup)
}
