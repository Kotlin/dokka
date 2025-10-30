plugins {
    id("com.android.library") version "/* %{AGP_VERSION} */"
    id("org.jetbrains.dokka") version "/* %{DGP_VERSION} */"
    //kotlin("android") version "/* %{KGP_VERSION} */"
}

android {
    namespace = "org.jetbrains.dokka.it.android"
    compileSdk = 33
    defaultConfig {
        minSdk = 21
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.1.0")
}

// WORKAROUND https://github.com/Kotlin/dokka/issues/3701
afterEvaluate {
    dokka.dokkaSourceSets
        .matching { it.name == "debug" }
        .configureEach {
            sourceRoots.setFrom(emptyList<String>())
        }
}
