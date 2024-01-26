/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("ru.vyarus.mkdocs") version "2.4.0"
}

if (project.version.toString().endsWith("-SNAPSHOT")) {
    // Do not generate the root index.html file with the redirect
    // to a snapshot version, otherwise GitHub Pages-based documentation
    // will always lead to the non-yet-released documentation.
    // For more details, see https://github.com/Kotlin/dokka/issues/2869.
    // For configuration details, see https://xvik.github.io/gradle-mkdocs-plugin/3.0.0/examples/#simple-multi-version.
    mkdocs.publish.rootRedirect = false
}
