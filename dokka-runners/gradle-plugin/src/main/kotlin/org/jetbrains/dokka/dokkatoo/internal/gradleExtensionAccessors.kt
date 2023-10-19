/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.dokkatoo.internal

import org.jetbrains.dokka.dokkatoo.DokkatooExtension

// When Dokkatoo is applied to a build script Gradle will auto-generate these accessors

internal fun DokkatooExtension.versions(configure: DokkatooExtension.Versions.() -> Unit) {
    versions.apply(configure)
}
