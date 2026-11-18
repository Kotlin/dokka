/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

/**
 * Checks whether a declaration with this visibility placed in [packageName] should be documented
 * according to the [sourceSet] configuration.
 *
 * Per-package options (matched by [DokkaConfiguration.PackageOptions.matchingRegex]) take precedence
 * over the source-set-level ones, and the deprecated `includeNonPublic` is honoured only if
 * `documentedVisibilities` was not explicitly overridden, so that only one setting prevails.
 */
@InternalDokkaApi
public fun DokkaConfiguration.Visibility.isAllowedInPackage(
    packageName: String?,
    sourceSet: DokkaConfiguration.DokkaSourceSet,
): Boolean {
    val packageOptions = packageName?.let { name ->
        sourceSet.perPackageOptions.firstOrNull { Regex(it.matchingRegex).matches(name) }
    }

    val (documentedVisibilities, includeNonPublic) =
        @Suppress("DEPRECATION") // for includeNonPublic, preserve backwards compatibility
        if (packageOptions != null) packageOptions.documentedVisibilities to packageOptions.includeNonPublic
        else sourceSet.documentedVisibilities to sourceSet.includeNonPublic

    // if `documentedVisibilities` is explicitly overridden by the user (i.e. not default value by reference),
    // deprecated `includeNonPublic` should not be taken into account, so that only one setting prevails
    val isDocumentedVisibilitiesOverridden = documentedVisibilities !== DokkaDefaults.documentedVisibilities
    return documentedVisibilities.contains(this) || (!isDocumentedVisibilitiesOverridden && includeNonPublic)
}
