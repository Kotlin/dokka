/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

object TestConstants {
    /**
     * Dokka HTML files that are only used for styling the generated HTML content.
     *
     * These files should be excluded from file-content assertion checks, to avoid
     * minor style changes affecting the tests.
     */
    val DokkaHtmlAssetsFiles = listOf(
        "ui-kit/ui-kit.min.js",
        "ui-kit/ui-kit.min.css",
        "styles/logo-styles.css",
        "styles/font-jb-sans-auto.css",
        "styles/style.css",
        "styles/main.css",
        "scripts/main.js",
        "scripts/safe-local-storage_blocking.js",
        "scripts/navigation-loader.js",
        "scripts/platform-content-handler.js",
    )
}
