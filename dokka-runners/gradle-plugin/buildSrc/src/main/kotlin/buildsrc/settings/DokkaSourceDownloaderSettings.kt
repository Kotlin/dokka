/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package buildsrc.settings

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property

abstract class DokkaSourceDownloaderSettings : ExtensionAware {

    abstract val dokkaVersion: Property<String>

    companion object {
        const val EXTENSION_NAME = "dokkaSourceDownload"
    }
}
