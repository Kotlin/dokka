plugins {
    id("com.android.kotlin.multiplatform.library") version "/* %{AGP_VERSION} */"
    id("org.jetbrains.dokka") version "/* %{DGP_VERSION} */"
    kotlin("multiplatform") version "/* %{KGP_VERSION} */"
}

kotlin {
    androidLibrary {
        namespace = "com.example.kmpfirstlib"
        compileSdk = 33
        minSdk = 24

        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }
    }

    sourceSets {
        androidMain {
            dependencies {
                implementation("androidx.appcompat:appcompat:1.1.0")
            }
        }
        getByName("androidHostTest") {
            dependencies {
            }
        }

        getByName("androidDeviceTest") {
            dependencies {
            }
        }
    }
}
