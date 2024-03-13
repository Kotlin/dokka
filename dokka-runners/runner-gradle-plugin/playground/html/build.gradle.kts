/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    html {
        customAssets.from("src/assets")
        customStyleSheets.from("src/styleSheets")
        templatesDirectory = layout.projectDirectory.dir("src/templates")
        separateInheritedMembers = true
        mergeImplicitExpectActualDeclarations = false
        footerMessage = "Created by ***"
        homepageLink = "https://github.com/kotlin/dokka"
    }
}
