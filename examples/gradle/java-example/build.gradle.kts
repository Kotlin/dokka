/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    base
    `dokka-convention`
}

dependencies {
    dokka(project(":my-java-application"))
    dokka(project(":my-java-features"))
    dokka(project(":my-java-library"))

    dokkaHtmlPlugin("org.jetbrains.dokka:templating-plugin")
}

dokka {
    moduleName.set("My Java Project")
}
