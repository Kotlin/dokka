/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    plugin("com.example:mermaid", "com.example.mermaid.MermaidDokkaPlugin") {
        property("lightTheme", "something")

        // Note: this code compiles currently, and is resolved to `Project.property(String)`
        property("property")
    }
}
