plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false

//    id("com.android.library") /* %{AGP_VERSION} */ apply false
//    kotlin("multiplatform") /* %{KGP_VERSION} */ apply false
//    id("org.jetbrains.compose") version "1.5.14" apply false // TODO make compose version parameterised

    id("org.jetbrains.dokka") /* %{DGP_VERSION} */
}

dependencies {
    dokka(project(":core"))
    dokka(project(":material3"))
}
