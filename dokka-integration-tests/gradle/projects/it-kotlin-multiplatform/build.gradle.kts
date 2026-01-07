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

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()
}

dokka.pluginsConfiguration.html.footerMessage.set("© 2025 Copyright")
