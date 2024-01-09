/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.plugins

import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

// TODO: this works only for HTML - do we need to somehow make it configurable only on html format level?
@DokkaGradlePluginDsl
public interface DokkaVersioningPluginConfiguration : DokkaPluginConfiguration {
    public val currentVersion: Property<String>
    public val versionsOrdering: ListProperty<String>
    public val oldVersionsDirectory: DirectoryProperty // maps to olderVersionsDir
    public val oldVersionsDirectoriesOverride: MapProperty<String, Directory> // maps to olderVersions
    public val renderVersionsNavigationOnAllPages: Property<Boolean>
}
