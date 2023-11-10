/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

internal fun parsePerPackageOptions(args: List<String>): List<DokkaConfiguration.PackageOptions> = args.map { it.split(",") }.map {
    val matchingRegex = it.first()

    val options = it.subList(1, it.size)

    val deprecated = options.find { it.endsWith("skipDeprecated") }?.startsWith("+")
        ?: DokkaDefaults.skipDeprecated

    val reportUndocumented = options.find { it.endsWith("reportUndocumented") }?.startsWith("+")
        ?: DokkaDefaults.reportUndocumented

    val privateApi = options.find { it.endsWith("includeNonPublic") }?.startsWith("+")
        ?: DokkaDefaults.includeNonPublic

    val suppress = options.find { it.endsWith("suppress") }?.startsWith("+")
        ?: DokkaDefaults.suppress

    val documentedVisibilities = options
        .filter { it.matches(Regex("\\+visibility:.+")) } // matches '+visibility:' with at least one symbol after the semicolon
        .map { DokkaConfiguration.Visibility.fromString(it.split(":")[1]) }
        .toSet()
        .ifEmpty { DokkaDefaults.documentedVisibilities }

    PackageOptionsImpl(
        matchingRegex,
        includeNonPublic = privateApi,
        documentedVisibilities = documentedVisibilities,
        reportUndocumented = reportUndocumented,
        skipDeprecated = !deprecated,
        suppress = suppress
    )
}
