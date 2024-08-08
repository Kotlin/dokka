@file:Suppress("FunctionName")

/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.dokka.parameters

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.jetbrains.dokka.gradle.internal.DokkaInternalApi
import java.io.Serializable

/**
 * Configuration builder that allows setting some options for specific packages
 * matched by [matchingRegex].
 *
 * Example in Gradle Kotlin DSL:
 *
 * ```kotlin
 * tasks.dokkaHtml {
 *     dokkaSourceSets.configureEach {
 *         perPackageOption {
 *             matchingRegex.set(".*internal.*")
 *             suppress.set(true)
 *         }
 *     }
 * }
 * ```
 */
abstract class DokkaPackageOptionsSpec
@DokkaInternalApi
constructor() :
    HasConfigurableVisibilityModifiers,
    Serializable {

    /**
     * Regular expression that is used to match the package.
     *
     * Default is any string: `.*`.
     */
    @get:Input
    abstract val matchingRegex: Property<String>

    /**
     * Whether this package should be skipped when generating documentation.
     *
     * Default is `false`.
     */
    @get:Input
    abstract val suppress: Property<Boolean>

    /**
     * Set of visibility modifiers that should be documented.
     *
     * This can be used if you want to document protected/internal/private declarations within a
     * specific package, as well as if you want to exclude public declarations and only document internal API.
     *
     * Can be configured for a whole source set, see [DokkaSourceSetSpec.documentedVisibilities].
     *
     * Default is [VisibilityModifier.Public].
     */
    @get:Input
    abstract override val documentedVisibilities: SetProperty<VisibilityModifier>

    /**
     * Whether to document declarations annotated with [Deprecated].
     *
     * Can be overridden on source set level by setting [DokkaSourceSetSpec.skipDeprecated].
     *
     * Default is `false`.
     */
    @get:Input
    abstract val skipDeprecated: Property<Boolean>

    /**
     * Whether to emit warnings about visible undocumented declarations, that is declarations from
     * this package and without KDocs, after they have been filtered by [documentedVisibilities].
     *
     *
     * Can be overridden on source set level by setting [DokkaSourceSetSpec.reportUndocumented].
     *
     * Default is `false`.
     */
    @get:Input
    abstract val reportUndocumented: Property<Boolean>
}
