package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.*
import java.io.File
import java.net.URL

// TODO NOW: Test
internal fun GradleDokkaSourceSetBuilder.toDokkaSourceSetImpl(): DokkaSourceSetImpl {
    return DokkaSourceSetImpl(
        classpath = classpath.toSet(),
        moduleDisplayName = moduleNameOrDefault(),
        displayName = displayNameOrDefault(),
        sourceSetID = sourceSetID,
        sourceRoots = sourceRoots.toSet(),
        dependentSourceSets = dependentSourceSets.get().toSet(),
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
        analysisPlatform = analysisPlatformOrDefault()
    )
}

private fun GradleDokkaSourceSetBuilder.moduleNameOrDefault(): String {
    return moduleDisplayName.getSafe() ?: project.name
}

private fun GradleDokkaSourceSetBuilder.displayNameOrDefault(): String {
    return displayName.getSafe() ?: name.substringBeforeLast("Main", platform.getSafe().toString())
}

private fun GradleDokkaSourceSetBuilder.externalDocumentationLinksWithDefaults(): Set<ExternalDocumentationLinkImpl> {
    return externalDocumentationLinks.getSafe().build()
        .run {
            if (noJdkLink.get()) this
            else this + ExternalDocumentationLink(
                url =
                if (jdkVersion.getSafe() < 11) "https://docs.oracle.com/javase/" +
                        "${jdkVersion.getSafe()}/docs/api/"
                else "https://docs.oracle.com/en/java/javase/" +
                        "${jdkVersion.getSafe()}/docs/api/java.base/",
                packageListUrl =
                if (jdkVersion.getSafe() < 11) "https://docs.oracle.com/javase/" +
                        "${jdkVersion.getSafe()}/docs/api/package-list"
                else "https://docs.oracle.com/en/java/javase/" +
                        "${jdkVersion.getSafe()}/docs/api/element-list"
            )
        }
        .run {
            if (noStdlibLink.getSafe()) this
            else this + ExternalDocumentationLink("https://kotlinlang.org/api/latest/jvm/stdlib/")
        }
        .run {
            if (noAndroidSdkLink.getSafe() || !project.isAndroidProject()) this
            else this +
                    ExternalDocumentationLink("https://developer.android.com/reference/") +
                    ExternalDocumentationLink(
                        url = URL("https://developer.android.com/reference/kotlin/"),
                        packageListUrl = URL("https://developer.android.com/reference/androidx/package-list")
                    )
        }
        .toSet()
}

private fun GradleDokkaSourceSetBuilder.analysisPlatformOrDefault(): Platform {
    val analysisPlatform = analysisPlatform.getSafe()
    if (analysisPlatform != null) return analysisPlatform

    platform.getSafe()?.let { platform ->
        return when (platform.toLowerCase()) {
            "androidjvm", "android" -> Platform.jvm
            "metadata" -> Platform.common
            else -> Platform.fromString(platform)
        }
    }
    return Platform.DEFAULT
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
