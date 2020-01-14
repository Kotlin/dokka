package utils

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.PassConfigurationImpl
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.SourceRootImpl
import java.io.File

object Builders {
    data class ConfigBuilder(
        val format: String = "html",
        val generateIndexPages: Boolean = true,
        val cacheRoot: String? = null,
        val impliedPlatforms: List<String> = emptyList(),
        val passesConfigurations: List<PassBuilder> = emptyList(),
        var pluginsClasspath: List<String> = emptyList()
    ) {
        operator fun invoke(out: String) =
            DokkaConfigurationImpl(
                outputDir = out,
                format = format,
                generateIndexPages = generateIndexPages,
                cacheRoot = cacheRoot,
                impliedPlatforms = impliedPlatforms,
                passesConfigurations = passesConfigurations.map { it() },
                pluginsClasspath = pluginsClasspath.map { File(it) }
            )
    }

    data class PassBuilder(
        val moduleName: String = "",
        val classpath: List<String> = emptyList(),
        val sourceRoots: List<String> = emptyList(),
        val samples: List<String> = emptyList(),
        val includes: List<String> = emptyList(),
        val includeNonPublic: Boolean = true,
        val includeRootPackage: Boolean = true,
        val reportUndocumented: Boolean = false,
        val skipEmptyPackages: Boolean = false,
        val skipDeprecated: Boolean = false,
        val jdkVersion: Int = 6,
        val languageVersion: String? = null,
        val apiVersion: String? = null,
        val noStdlibLink: Boolean = false,
        val noJdkLink: Boolean = false,
        val suppressedFiles: List<String> = emptyList(),
        val collectInheritedExtensionsFromLibraries: Boolean = true,
        val analysisPlatform: String = "",
        val targets: List<String> = emptyList(),
        val sinceKotlin: String? = null
    ) {
        operator fun invoke() =
            PassConfigurationImpl(
                moduleName = moduleName,
                classpath = classpath,
                sourceRoots = sourceRoots.map{ SourceRootImpl(it) },
                samples = samples,
                includes = includes,
                includeNonPublic = includeNonPublic,
                includeRootPackage = includeRootPackage,
                reportUndocumented = reportUndocumented,
                skipEmptyPackages = skipEmptyPackages,
                skipDeprecated = skipDeprecated,
                jdkVersion = jdkVersion,
                languageVersion = languageVersion,
                apiVersion = apiVersion,
                noStdlibLink = noStdlibLink,
                noJdkLink = noJdkLink,
                suppressedFiles = suppressedFiles,
                collectInheritedExtensionsFromLibraries = collectInheritedExtensionsFromLibraries,
                analysisPlatform = Platform.fromString(analysisPlatform),
                targets = targets,
                sinceKotlin = sinceKotlin
            )
    }


}