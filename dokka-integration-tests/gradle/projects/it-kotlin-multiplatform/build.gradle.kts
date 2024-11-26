plugins {
    kotlin("multiplatform") version "/* %{KGP_VERSION} */"
    id("org.jetbrains.dokka") version "/* %{DGP_VERSION} */"
}

kotlin {
    jvm()
    linuxX64()
    linuxArm64()
    iosX64()
    iosArm64()
}
