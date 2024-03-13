/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

@DokkaGradlePluginExperimentalApi
@DokkaGradlePluginDsl
public interface DokkaVersioningConfiguration {
    public val enabled: Property<Boolean>

    public val currentVersion: Property<String>
    public val versionsOrdering: ListProperty<String>
    public val oldVersionsDirectory: DirectoryProperty
    public val oldVersionsDirectoriesOverride: MapProperty<String, Directory>
    public val renderVersionsNavigationOnAllPages: Property<Boolean>
}
