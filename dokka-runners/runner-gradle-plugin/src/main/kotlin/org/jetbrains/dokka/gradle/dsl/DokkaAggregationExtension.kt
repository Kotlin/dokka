/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.jetbrains.dokka.gradle.dsl.formats.DokkaFormatsContainer
import org.jetbrains.dokka.gradle.dsl.plugins.DokkaPluginsContainer

@DokkaGradlePluginDsl
public interface DokkaAggregationExtension {
    public val includedDocumentation: ConfigurableFileCollection
    public fun moduleDocumentation(text: String)
    public fun moduleDocumentationFrom(path: Any)

    // TODO: this is rather adhoc, though, not a lot of use cases...
    //   need better naming
    public fun useMultiModule(fileLayout: DokkaMultiModuleFileLayout)
    public fun useCollector()

    // f.e. for included builds. maybe pick another name
    // TODO: better naming
    public fun include(dependencyNotation: Any)

    // the projects below are based on Gradle project paths (not names) ?
    public fun includeProjects(
        vararg projects: String,
        configure: DokkaAggregationProjectsConfiguration.() -> Unit = {}
    )

    public fun includeProjects(
        projects: List<String>,
        configure: DokkaAggregationProjectsConfiguration.() -> Unit = {}
    )

    // TODO: how to make users always write `exclude`, without it, the line looks confusing
    public fun includeAllprojects(
        exclude: List<String> = emptyList(),
        configure: DokkaAggregationProjectsConfiguration.() -> Unit = {}
    )

    public fun includeSubprojects(
        exclude: List<String> = emptyList(),
        configure: DokkaAggregationProjectsConfiguration.() -> Unit = {}
    )

    public val formats: DokkaFormatsContainer
    public fun formats(configure: DokkaFormatsContainer.() -> Unit)

    public val plugins: DokkaPluginsContainer
    public fun plugins(configure: DokkaPluginsContainer.() -> Unit)
}

// TODO: I DON" LIKE THIS
@DokkaGradlePluginDsl
public interface DokkaAggregationProjectsConfiguration {
    // TODO: which specific properties can be inherited?
    //  all from DokkaModuleBasedConfiguration and DokkaSourceSetConfiguration - reasonable default
    // false by default
    @GradleIsolatedProjectsRestrictions("https://github.com/gradle/gradle/issues/25179")
    public val inheritConfiguration: Property<Boolean>

    // false by default
    @GradleIsolatedProjectsRestrictions("https://github.com/gradle/gradle/issues/22514")
    public val applyDokkaPlugin: Property<Boolean>
}
