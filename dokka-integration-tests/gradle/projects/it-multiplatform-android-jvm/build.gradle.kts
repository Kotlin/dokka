plugins {
    id("com.android.library") version "/* %{AGP_VERSION} */"
    id("org.jetbrains.dokka") version "/* %{DGP_VERSION} */"
    kotlin("multiplatform") version "/* %{KGP_VERSION} */"
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

kotlin {
    jvm()
    androidTarget()

    sourceSets {
        val androidAndJvmMain by registering
        jvmMain {
            dependsOn(androidAndJvmMain.get())
        }
        androidMain {
            dependsOn(androidAndJvmMain.get())
        }
    }
}

dokka.pluginsConfiguration.html.footerMessage.set("© 2025 Copyright")
