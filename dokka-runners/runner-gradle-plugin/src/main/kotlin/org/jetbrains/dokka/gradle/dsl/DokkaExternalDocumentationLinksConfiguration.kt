/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.net.URI

// note: packageList should be downloaded via Gradle and not inside Dokka
@DokkaGradlePluginDsl
public interface DokkaExternalDocumentationLinkConfiguration {
    public val removeUrl: Property<String>
    public val packageListLocation: Property<URI>

    public fun packageListUrl(url: String)
    public fun packageListFile(path: Any)
}

@DokkaGradlePluginDsl
public interface DokkaExternalDocumentationLinksConfiguration {
    public val links: ListProperty<DokkaExternalDocumentationLinkConfiguration>

    public fun externalLink(configure: DokkaExternalDocumentationLinkConfiguration.() -> Unit = {}) {}

    public fun externalLink(
        remoteUrl: String,
        configure: DokkaExternalDocumentationLinkConfiguration.() -> Unit = {}
    ) {
    }

    public fun linkToJdk(jdkVersion: Int = 8, enabled: Boolean = true) {}
    public fun linkToAndroidSdk(enabled: Boolean = true) {}

    public fun linkToKotlinStdlib(
        enabled: Boolean = true,
        configure: DokkaExternalDocumentationLinkConfiguration.() -> Unit = {}
    ) {
    }

    // optional: for stable kotlinx libraries
    public fun linkToKotlinxCoroutines(
        enabled: Boolean = true,
        configure: DokkaExternalDocumentationLinkConfiguration.() -> Unit = {}
    ) {
    }

    public fun linkToKotlinxSerialization(
        enabled: Boolean = true,
        configure: DokkaExternalDocumentationLinkConfiguration.() -> Unit = {}
    ) {
    }
}
