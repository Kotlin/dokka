/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.dokkaVersionType
import org.jetbrains.DokkaVersionType

plugins {
    id("ru.vyarus.mkdocs") version "2.4.0"
}

if (dokkaVersionType != DokkaVersionType.RELEASE) {
    // Do not generate the root index.html file with the redirect
    // to a non-release version, otherwise GitHub pages based documentation
    // will always lead to the non-stable documentation.
    // For more details, see https://github.com/Kotlin/dokka/issues/2869.
    // For configuration details, see https://xvik.github.io/gradle-mkdocs-plugin/3.0.0/examples/#simple-multi-version.
    mkdocs.publish.rootRedirect = false
}
