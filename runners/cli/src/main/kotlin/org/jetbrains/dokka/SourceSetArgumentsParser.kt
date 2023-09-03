/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.delimiter

internal fun parseSourceSet(moduleName: String, args: Array<String>): DokkaConfiguration.DokkaSourceSet {

    if (moduleName.contains(',')) {
        // To figure out why this is needed and if it is still relevant, see the comment here:
        // https://github.com/Kotlin/dokka/issues/3011#issuecomment-1568620493
        throw IllegalArgumentException("Module name cannot contain commas as it is used internally as a delimiter.")
    }

    val parser = ArgParser("sourceSet", prefixStyle = ArgParser.OptionPrefixStyle.JVM)

    val sourceSetName by parser.option(
        ArgType.String,
        description = "Name of the source set"
    ).default("main")

    val displayName by parser.option(
        ArgType.String,
        description = "Display name of the source set, used both internally and externally"
    ).default(DokkaDefaults.sourceSetDisplayName)

    val classpath by parser.option(
        ArgTypeFile,
        description = "Classpath for analysis and interactive samples. Accepts multiple paths separated by semicolons"
    ).delimiter(";")

    val sourceRoots by parser.option(
        ArgTypeFile,
        description = "Source code roots to be analyzed and documented. Accepts multiple paths separated by semicolons",
        fullName = "src"
    ).delimiter(";")

    val dependentSourceSets by parser.option(
        ArgType.String,
        description = "Names of dependent source sets in format \"moduleName/sourceSetName\". " +
                "Accepts multiple paths separated by semicolons"
    ).delimiter(";")

    val samples by parser.option(
        ArgTypeFile,
        description = "List of directories or files that contain sample functions. " +
                "Accepts multiple paths separated by semicolons"
    ).delimiter(";")

    val includes by parser.option(
        ArgTypeFile,
        description = "Markdown files that contain module and package documentation. " +
                "Accepts multiple paths separated by semicolons"
    ).delimiter(";")

    val includeNonPublic: Boolean by parser.option(
        ArgType.Boolean,
        description = "Deprecated, use documentedVisibilities")
        .default(DokkaDefaults.includeNonPublic)

    val documentedVisibilities by parser.option(
        ArgTypeVisibility,
        description = "Visibilities to be documented. Accepts multiple values separated by semicolons"
    ).delimiter(";")

    val reportUndocumented by parser.option(ArgType.Boolean, description = "Whether to report undocumented declarations")
        .default(DokkaDefaults.reportUndocumented)

    val noSkipEmptyPackages by parser.option(
        ArgType.Boolean,
        description = "Whether to create pages for empty packages"
    ).default(!DokkaDefaults.skipEmptyPackages)

    val skipEmptyPackages by lazy { !noSkipEmptyPackages }

    val skipDeprecated by parser.option(ArgType.Boolean, description = "Whether to skip deprecated declarations")
        .default(DokkaDefaults.skipDeprecated)

    val jdkVersion by parser.option(
        ArgType.Int,
        description = "Version of JDK to use for linking to JDK Javadocs"
    ).default(DokkaDefaults.jdkVersion)

    val languageVersion by parser.option(
        ArgType.String,
        description = "Language version used for setting up analysis and samples"
    )

    val apiVersion by parser.option(
        ArgType.String,
        description = "Kotlin API version used for setting up analysis and samples"
    )

    val noStdlibLink by parser.option(ArgType.Boolean, description = "Whether to generate links to Standard library")
        .default(DokkaDefaults.noStdlibLink)

    val noJdkLink by parser.option(ArgType.Boolean, description = "Whether to generate links to JDK Javadocs")
        .default(DokkaDefaults.noJdkLink)

    val suppressedFiles by parser.option(
        ArgTypeFile,
        description = "Paths to files to be suppressed. Accepts multiple paths separated by semicolons."
    ).delimiter(";")

    val analysisPlatform: Platform by parser.option(
        ArgTypePlatform,
        description = "Platform used for setting up analysis"
    ).default(DokkaDefaults.analysisPlatform)

    val perPackageOptions by parser.option(
        ArgType.String,
        description = "List of package source set configuration in format " +
                "\"matchingRegexp,-deprecated,-privateApi,+warnUndocumented,+suppress;...\". " +
                "Accepts multiple values separated by semicolons. "
    ).delimiter(";")

    val externalDocumentationLinks by parser.option(
        ArgType.String,
        description = "External documentation links in format {url}^{packageListUrl}. " +
                "Accepts multiple values separated by `^^`"
    ).delimiter("^^")

    val sourceLinks by parser.option(
        ArgTypeSourceLinkDefinition,
        description = "Mapping between a source directory and a Web service for browsing the code. " +
                "Accepts multiple paths separated by semicolons",
        fullName = "srcLink"
    ).delimiter(";")

    parser.parse(args)

    return object : DokkaConfiguration.DokkaSourceSet {
        override val displayName = displayName
        override val sourceSetID = DokkaSourceSetID(moduleName, sourceSetName)
        override val classpath = classpath.toMutableList()
        override val sourceRoots = sourceRoots.toMutableSet()
        override val dependentSourceSets = dependentSourceSets
            .map { dependentSourceSetName -> dependentSourceSetName.split('/').let { DokkaSourceSetID(it[0], it[1]) } }
            .toMutableSet()
        override val samples = samples.toMutableSet()
        override val includes = includes.toMutableSet()
        @Deprecated("Use [documentedVisibilities] property for a more flexible control over documented visibilities")
        override val includeNonPublic = includeNonPublic
        override val reportUndocumented = reportUndocumented
        override val skipEmptyPackages = skipEmptyPackages
        override val skipDeprecated = skipDeprecated
        override val jdkVersion = jdkVersion
        override val sourceLinks = sourceLinks.toMutableSet()
        override val analysisPlatform = analysisPlatform
        override val perPackageOptions = parsePerPackageOptions(perPackageOptions).toMutableList()
        override val externalDocumentationLinks = parseLinks(externalDocumentationLinks).toMutableSet()
        override val languageVersion = languageVersion
        override val apiVersion = apiVersion
        override val noStdlibLink = noStdlibLink
        override val noJdkLink = noJdkLink
        override val suppressedFiles = suppressedFiles.toMutableSet()
        override val documentedVisibilities: Set<DokkaConfiguration.Visibility> = documentedVisibilities.toSet()
            .ifEmpty { DokkaDefaults.documentedVisibilities }
    }
}
