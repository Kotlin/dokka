package renderers

import org.jetbrains.dokka.DokkaSourceSetID
import org.jetbrains.dokka.DokkaSourceSetImpl
import org.jetbrains.dokka.Platform

val defaultSourceSet = DokkaSourceSetImpl(
    moduleDisplayName = "DEFAULT",
    displayName = "DEFAULT",
    sourceSetID = DokkaSourceSetID("DEFAULT", "DEFAULT"),
    classpath = emptySet(),
    sourceRoots = emptySet(),
    dependentSourceSets = emptySet(),
    samples = emptySet(),
    includes = emptySet(),
    includeNonPublic = false,
    includeRootPackage = false,
    reportUndocumented = false,
    skipEmptyPackages = true,
    skipDeprecated = false,
    jdkVersion = 8,
    sourceLinks = emptySet(),
    perPackageOptions = emptyList(),
    externalDocumentationLinks = emptySet(),
    languageVersion = null,
    apiVersion = null,
    noStdlibLink = false,
    noJdkLink = false,
    suppressedFiles = emptySet(),
    analysisPlatform = Platform.DEFAULT
)
