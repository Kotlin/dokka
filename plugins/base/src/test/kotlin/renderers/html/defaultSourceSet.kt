package renderers.html

import org.jetbrains.dokka.DokkaSourceSetImpl
import org.jetbrains.dokka.Platform

internal val defaultSourceSet = DokkaSourceSetImpl(
    moduleName = "DEFAULT",
    displayName = "DEFAULT",
    sourceSetID = "DEFAULT",
    classpath = emptyList(),
    sourceRoots = emptyList(),
    dependentSourceSets = emptyList(),
    samples = emptyList(),
    includes = emptyList(),
    includeNonPublic = false,
    includeRootPackage = false,
    reportUndocumented = false,
    skipEmptyPackages = true,
    skipDeprecated = false,
    jdkVersion = 8,
    sourceLinks = emptyList(),
    perPackageOptions = emptyList(),
    externalDocumentationLinks = emptyList(),
    languageVersion = null,
    apiVersion = null,
    noStdlibLink = false,
    noJdkLink = false,
    suppressedFiles = emptyList(),
    analysisPlatform = Platform.DEFAULT
)