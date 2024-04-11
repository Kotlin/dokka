/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

@DokkaGradlePluginDsl
public interface DokkaPerSourceSetConfiguration : DokkaSourceSetConfiguration {
    public val pattern: Property<String>
}

@DokkaGradlePluginDsl
public interface DokkaSourceSetConfiguration : DokkaSourceSetBasedConfiguration {
    public val suppress: Property<Boolean>
    public val displayName: Property<String>
}

@DokkaGradlePluginDsl
public interface DokkaSourceSetBasedConfiguration : DokkaPackageBasedConfiguration {
    public val includeEmptyPackages: Property<Boolean>
    public val includedDocumentation: ConfigurableFileCollection

    // sourceLink("https://www.github.com/owner/repository/tree/main") - link to root of remote repository
    public val sourceLinks: ListProperty<DokkaSourceLinkConfiguration>
    public fun sourceLink(configure: DokkaSourceLinkConfiguration.() -> Unit) {}
    public fun sourceLink(remoteUrl: String, configure: DokkaSourceLinkConfiguration.() -> Unit = {}) {}

    public val externalLinks: ListProperty<DokkaExternalLinkConfiguration>
    public fun externalLink(configure: DokkaExternalLinkConfiguration.() -> Unit) {}
    public fun externalLink(remoteUrl: String, configure: DokkaExternalLinkConfiguration.() -> Unit = {}) {}

    // predefined links
    public fun externalLinkToJdk(enabled: Boolean = true, jdkVersion: Int = 8) {}
    public fun externalLinkToAndroidSdk(enabled: Boolean = true) {}
    public fun externalLinkToKotlinStdlib(enabled: Boolean = true) {}
}

@DokkaGradlePluginDelicateApi
@DokkaGradlePluginDsl
public interface DokkaSourceSet : DokkaSourceSetConfiguration, Named {
    public val platform: Property<KotlinPlatformType>

    public val languageVersion: Property<KotlinVersion>
    public val apiVersion: Property<KotlinVersion>

    public val classpath: ConfigurableFileCollection
    public val sourceFiles: ConfigurableFileCollection

    public val suppressedSourceFiles: ConfigurableFileCollection
    public val samples: ConfigurableFileCollection
}
