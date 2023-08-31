/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.resolvers.external

import org.jetbrains.dokka.links.DRI

/**
 * Provides the path to the page documenting a [DRI] in an external documentation source
 */
public fun interface ExternalLocationProvider {
    /**
     * @return Path to the page containing the [dri] or null if the path cannot be created
     * (eg. when the package-list does not contain [dri]'s package)
     */
    public fun resolve(dri: DRI): String?
}
