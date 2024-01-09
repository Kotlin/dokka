/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

@DokkaGradlePluginDsl
public interface DokkaSourceSetConfiguration : DokkaSourceSetBasedConfiguration {
    public val suppress: Property<Boolean>
    public val displayName: Property<String>
}

@DokkaGradlePluginDsl
public interface DokkaSourceSetBasedConfiguration : DokkaPackageBasedConfiguration {
    public val includeEmptyPackages: Property<Boolean> // old skipEmptyPackages

    public val perPackageConfigurations: SetProperty<DokkaPerPackageConfiguration>
    public fun perPackageConfiguration(configure: DokkaPerPackageConfiguration.() -> Unit)
    public fun perPackageConfiguration(matchingRegex: String, configure: DokkaPerPackageConfiguration.() -> Unit)

    // sourceLink("https://www.github.com/owner/repository")
    // TODO: ListProperty vs DomainObjectCollection
    public val sourceLinks: ListProperty<DokkaSourceLinkConfiguration>
    public fun sourceLink(configure: DokkaSourceLinkConfiguration.() -> Unit)

    // link to root of remote repository
    public fun sourceLink(remoteUrl: String, configure: DokkaSourceLinkConfiguration.() -> Unit = {})

    //TODO: may be we need to add out of the box kotlinx.coroutines, serialization, etc?
    public val externalDocumentationLinks: ListProperty<DokkaExternalDocumentationLinkConfiguration>
    public fun externalDocumentationLink(remoteUrl: String, packageListUrl: String? = null)
    public fun externalDocumentationLinkFrom(remoteUrl: String, packageListPath: Any)

    public fun externalDocumentationLinkToJdk(
        enabled: Boolean = true,
        jdkVersion: Int = 8,
        configure: DokkaExternalDocumentationLinkConfiguration.() -> Unit = {}
    )

    public fun externalDocumentationLinkToAndroidSdk(
        enabled: Boolean = true,
        configure: DokkaExternalDocumentationLinkConfiguration.() -> Unit = {}
    )

    public fun externalDocumentationLinkToKotlinStdlib(
        enabled: Boolean = true,
        configure: DokkaExternalDocumentationLinkConfiguration.() -> Unit = {}
    )

    public fun externalDocumentationLinkToKotlinxCoroutines(
        enabled: Boolean = false,
        configure: DokkaExternalDocumentationLinkConfiguration.() -> Unit = {}
    )

    public fun externalDocumentationLinkToKotlinxSerialization(
        enabled: Boolean = false,
        configure: DokkaExternalDocumentationLinkConfiguration.() -> Unit = {}
    )

    // includedDocumentation/packageDocumentation are shared (in DokkaPerSourceSetConfiguration not in DokkaSourceSet)
    // because we can have in some sourceSet, f.e. jvm, additional packages to document
    // TODO: includedDocumentation is bad naming, `includes` is also bad naming
    public val includedDocumentation: ConfigurableFileCollection

    // for simple cases
    // `path: Any` resolved as project.file(file)
    public fun includeDocumentation(text: String)
    public fun includeDocumentationFrom(path: Any)
    public fun packageDocumentation(packageName: String, text: String)
    public fun packageDocumentationFrom(packageName: String, path: Any)
}
