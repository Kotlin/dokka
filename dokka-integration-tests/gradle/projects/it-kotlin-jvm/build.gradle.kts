plugins {
    kotlin("jvm") version "/* %{KGP_VERSION} */"
    id("org.jetbrains.dokka") version "/* %{DGP_VERSION} */"
}

dokka.pluginsConfiguration.html.footerMessage.set("© 2025 Copyright")
