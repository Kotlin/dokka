plugins {
    id("com.android.library") version "/* %{AGP_VERSION} */"
    id("org.jetbrains.dokka") version "/* %{DGP_VERSION} */"
    kotlin("android") version "/* %{KGP_VERSION} */"
}

android {
    namespace = "org.jetbrains.dokka.it.android"
    defaultConfig {
        minSdkVersion(21)
        setCompileSdkVersion(30)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.1.0")
}

dokka.pluginsConfiguration.html.footerMessage.set("© 2025 Copyright")
