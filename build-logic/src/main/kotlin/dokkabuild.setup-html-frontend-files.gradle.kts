/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE

/**
 * Utility for sharing the Dokka HTML frontend files between subprojects in a safe, cacheable way.
 */

plugins {
    id("dokkabuild.base")
}

/** Apply a distinct attribute to the incoming/outgoing configuration */
fun AttributeContainer.dokkaHtmlFrontendFilesAttribute() =
    attribute(USAGE_ATTRIBUTE, objects.named("org.jetbrains.dokka.html-frontend-files"))

// incoming configuration
val dokkaHtmlFrontendFiles by configurations.registering {
    description = "Retrieve Dokka HTML frontend files from other subprojects"
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes { dokkaHtmlFrontendFilesAttribute() }
}

// outgoing configuration
val dokkaHtmlFrontendFilesElements by configurations.registering {
    description = "Provide Dokka HTML frontend files to other subprojects"
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes { dokkaHtmlFrontendFilesAttribute() }
}
