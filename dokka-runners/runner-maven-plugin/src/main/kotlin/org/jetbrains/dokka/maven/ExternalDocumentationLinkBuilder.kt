/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.maven

import org.apache.maven.plugins.annotations.Parameter
import org.jetbrains.dokka.ExternalDocumentationLink
import org.jetbrains.dokka.ExternalDocumentationLinkImpl
import java.net.URL

/**
 * Configuration block that allows creating links leading to externally hosted
 * documentation of your dependencies.
 *
 * For instance, if you are using types from `kotlinx.serialization`, by default
 * they will be unclickable in your documentation, as if unresolved. However,
 * since API reference for `kotlinx.serialization` is also built by Dokka and is
 * [published on kotlinlang.org](https://kotlinlang.org/api/kotlinx.serialization/),
 * you can configure external documentation links for it, allowing Dokka to generate
 * documentation links for used types, making them clickable and appear resolved.
 *
 * Example:
 *
 * ```xml
 * <externalDocumentationLinks>
 *     <link>
 *         <url>https://kotlinlang.org/api/latest/jvm/stdlib/</url>
 *         <packageListUrl>file:/${project.basedir}/stdlib.package.list</packageListUrl>
 *     </link>
 * </externalDocumentationLinks>
 * ```
 */
public class ExternalDocumentationLinkBuilder {

    /**
     * Root URL of documentation to link with. **Must** contain a trailing slash.
     *
     * Dokka will do its best to automatically find `package-list` for the given URL, and link
     * declarations together.
     *
     * It automatic resolution fails or if you want to use locally cached files instead,
     * consider providing [packageListUrl].
     *
     * Example:
     *
     * ```xml
     * <url>https://kotlinlang.org/api/latest/jvm/stdlib/</url>
     * ```
     */
    @Parameter(name = "url", required = true)
    public var url: URL? = null

    /**
     * Specifies the exact location of a `package-list` instead of relying on Dokka
     * automatically resolving it. Can also be a locally cached file to avoid network calls.
     *
     * Example:
     *
     * ```xml
     * <packageListUrl>file:/${project.basedir}/stdlib.package.list</packageListUrl>
     * ```
     */
    @Parameter(name = "packageListUrl", required = true)
    public var packageListUrl: URL? = null

    public fun build(): ExternalDocumentationLinkImpl = ExternalDocumentationLink(url, packageListUrl)
}
