package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.*
import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink
import java.io.File

internal fun GradleDokkaSourceSetBuilder.toDokkaSourceSetImpl(): DokkaSourceSetImpl = DokkaSourceSetImpl(
    classpath = classpath.toList(),
    displayName = displayNameOrDefault(),
    sourceSetID = sourceSetID,
    sourceRoots = sourceRoots.toSet(),
    dependentSourceSets = dependentSourceSets.getSafe().toSet(),
    samples = samples.toSet(),
    includes = includes.toSet(),
    includeNonPublic = includeNonPublic.getSafe(),
    reportUndocumented = reportUndocumented.getSafe(),
    skipEmptyPackages = skipEmptyPackages.getSafe(),
    skipDeprecated = skipDeprecated.getSafe(),
    jdkVersion = jdkVersion.getSafe(),
    sourceLinks = sourceLinks.getSafe().build().toSet(),
    perPackageOptions = perPackageOptions.getSafe().build(),
    externalDocumentationLinks = externalDocumentationLinksWithDefaults(),
    languageVersion = languageVersion.getSafe(),
    apiVersion = apiVersion.getSafe(),
    noStdlibLink = noStdlibLink.getSafe(),
    noJdkLink = noJdkLink.getSafe(),
    suppressedFiles = suppressedFilesWithDefaults(),
    analysisPlatform = platform.getSafe()
)

private fun GradleDokkaSourceSetBuilder.displayNameOrDefault(): String {
    displayName.getSafe()?.let { return it }
    if (name.endsWith("Main") && name != "Main") {
        return name.removeSuffix("Main")
    }

    return name
}

private fun GradleDokkaSourceSetBuilder.externalDocumentationLinksWithDefaults(): Set<ExternalDocumentationLinkImpl> {
    return externalDocumentationLinks.getSafe().build()
        .run {
            if (noJdkLink.getSafe()) this
            else this + ExternalDocumentationLink.jdk(jdkVersion.getSafe())
        }
        .run {
            if (noStdlibLink.getSafe()) this
            else this + ExternalDocumentationLink.kotlinStdlib()
        }
        .run {
            if (noAndroidSdkLink.getSafe() || !project.isAndroidProject()) this
            else this +
                    ExternalDocumentationLink.androidSdk() +
                    ExternalDocumentationLink.androidX()
        }
        .toSet()
}

private fun GradleDokkaSourceSetBuilder.suppressedFilesWithDefaults(): Set<File> {
    val suppressedFilesForAndroid = if (project.isAndroidProject()) {
        val generatedRoot = project.buildDir.resolve("generated").absoluteFile
        sourceRoots
            .filter { it.startsWith(generatedRoot) }
            .flatMap { it.walk().toList() }
            .toSet()
    } else {
        emptySet()
    }

    return suppressedFiles.toSet() + suppressedFilesForAndroid
}
