package org.jetbrains.conventions

import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE

/**
 * Utility for sharing the Dokka Base Plugin frontend files between subprojects in a safe, cacheable way.
 */

plugins {
    id("org.jetbrains.conventions.base")
}

/** Apply a distinct attribute to the incoming/outgoing configuration */
fun AttributeContainer.dokkaBaseFrontendFilesAttribute() =
    attribute(USAGE_ATTRIBUTE, objects.named("org.jetbrains.dokka.base-frontend-files"))

// incoming configuration
val dokkaBaseFrontendFiles by configurations.registering {
    description =
        "Retrieve Dokka Base Plugin frontend files from other subprojects... but the subproject must :plugins:base:frontend."
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes { dokkaBaseFrontendFilesAttribute() }
}

// outgoing configuration
val dokkaBaseFrontendFilesElements by configurations.registering {
    description = "Provide Dokka Base Plugin frontend files to other subprojects"
    isCanBeConsumed = true
    isCanBeResolved = false
    attributes { dokkaBaseFrontendFilesAttribute() }
}
