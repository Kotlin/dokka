plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.gradlePlugin.kotlin)
    implementation(libs.gradlePlugin.dokka)
}
