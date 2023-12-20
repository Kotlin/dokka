/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.maven

import org.apache.maven.plugins.annotations.Parameter
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.DokkaDefaults

/**
 * Configuration block that allows setting some options for specific packages
 * matched by [matchingRegex].
 *
 * Example:
 *
 * ```xml
 * <configuration>
 *     <perPackageOptions>
 *         <packageOptions>
 *             <matchingRegex>.*api.*</matchingRegex>
 *             <suppress>false</suppress>
 *             <reportUndocumented>false</reportUndocumented>
 *             <skipDeprecated>false</skipDeprecated>
 *             <documentedVisibilities>
 *                 <visibility>PUBLIC</visibility>
 *                 <visibility>PROTECTED</visibility>
 *             </documentedVisibilities>
 *         </packageOptions>
 *     </perPackageOptions>
 * </configuration>
 * ```
 */
public class PackageOptions : DokkaConfiguration.PackageOptions {

    /**
     * Regular expression that is used to match the package.
     *
     * If multiple packages match the same `matchingRegex`, the longest `matchingRegex` will be used.
     *
     * Default is any string: `.*`.
     */
    @Parameter
    override var matchingRegex: String = ".*"

    /**
     * Whether this package should be skipped when generating documentation.
     *
     * Default is `false`.
     */
    @Parameter
    override var suppress: Boolean = DokkaDefaults.suppress

    /**
     * List of visibility modifiers that should be documented.
     *
     * This can be used if you want to document protected/internal/private declarations within a
     * specific package, as well as if you want to exclude public declarations and only document internal API.
     *
     * Default is [DokkaConfiguration.Visibility.PUBLIC].
     */
    @Parameter(property = "visibility")
    override var documentedVisibilities: Set<DokkaConfiguration.Visibility> = DokkaDefaults.documentedVisibilities

    /**
     * Whether to document declarations annotated with [Deprecated].
     *
     * Can be set on project level with [AbstractDokkaMojo.skipDeprecated].
     *
     * Default is `false`.
     */
    @Parameter
    override var skipDeprecated: Boolean = DokkaDefaults.skipDeprecated

    /**
     * Whether to emit warnings about visible undocumented declarations, that is declarations from
     * this package and without KDocs, after they have been filtered by [documentedVisibilities].
     *
     * This setting works well with [AbstractDokkaMojo.failOnWarning].
     *
     * Default is `false`.
     */
    @Parameter
    override var reportUndocumented: Boolean = DokkaDefaults.reportUndocumented

    @Parameter
    @Deprecated("Use [documentedVisibilities] property for a more flexible control over documented visibilities")
    override var includeNonPublic: Boolean = DokkaDefaults.includeNonPublic
}
