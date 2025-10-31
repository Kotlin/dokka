plugins {
    id("com.android.library") version "/* %{AGP_VERSION} */"
    id("org.jetbrains.dokka") version "/* %{DGP_VERSION} */"
    kotlin("multiplatform") version "/* %{KGP_VERSION} */"
}

android {
    namespace = "org.jetbrains.dokka.it.android"
    defaultConfig {
        minSdkVersion(21)
        setCompileSdkVersion(29)
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
