/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package dokkabuild

import org.gradle.api.provider.Provider
import java.io.File

abstract class DevMavenPublishExtension(
    /**
     * Resolves Dev Maven repos from the current project's dependencies.
     */
    val devMavenRepositories: Provider<List<File>>,
) {
    companion object {
        const val DEV_MAVEN_PUBLISH_EXTENSION_NAME = "devMavenPublish"
    }
}
