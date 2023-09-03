/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.maven

import org.apache.maven.plugins.annotations.Parameter

/**
 * Configuration block that allows adding a `source` link to each signature
 * which leads to [path] with a specific line number (configurable by setting [lineSuffix]),
 * letting documentation readers find source code for each declaration.
 *
 * Example:
 *
 * ```xml
 * <sourceLinks>
 *     <link>
 *         <path>${project.basedir}/src</path>
 *         <url>https://github.com/kotlin/dokka/tree/master/src</url>
 *         <lineSuffix>#L</lineSuffix>
 *     </link>
 * </sourceLinks>
 * ```
 */
public class SourceLinkMapItem {

    /**
     * Path to the local source directory. The path must be relative to the root of current project.
     *
     * Example:
     *
     * ```xml
     * <path>${project.basedir}/src</path>
     * ```
     */
    @Parameter(name = "path", required = true)
    public var path: String = ""

    /**
     * URL of source code hosting service that can be accessed by documentation readers,
     * like GitHub, GitLab, Bitbucket, etc. This URL will be used to generate
     * source code links of declarations.
     *
     * Example:
     *
     * ```xml
     * <url>https://github.com/username/projectname/tree/master/src</url>
     * ```
     */
    @Parameter(name = "url", required = true)
    public var url: String = ""

    /**
     * Suffix used to append source code line number to the URL. This will help readers navigate
     * not only to the file, but to the specific line number of the declaration.
     *
     * The number itself will be appended to the specified suffix. For instance,
     * if this property is set to `#L` and the line number is 10, resulting URL suffix
     * will be `#L10`
     *
     * Suffixes used by popular services:
     * - GitHub: `#L`
     * - GitLab: `#L`
     * - Bitbucket: `#lines-`
     */
    @Parameter(name = "lineSuffix")
    public var lineSuffix: String? = null
}
