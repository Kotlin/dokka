/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.provider.Property
import java.net.URI

// note: packageList should be downloaded via Gradle and not inside Dokka
@DokkaGradlePluginDsl
public interface DokkaExternalLinkConfiguration {
    public val remoteUrl: Property<String>

    // URI could be pointing to a file or a link
    public val packageListLocation: Property<URI>
}
