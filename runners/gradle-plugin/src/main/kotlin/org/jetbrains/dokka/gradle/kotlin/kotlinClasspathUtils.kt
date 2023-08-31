package org.jetbrains.dokka.gradle.kotlin

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.jetbrains.dokka.gradle.isAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileTool
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_COMMON_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_KLIB_DIR
import org.jetbrains.kotlin.konan.library.KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal fun Project.classpathOf(sourceSet: KotlinSourceSet): FileCollection {
    val compilations = compilationsOf(sourceSet)
    return if (compilations.isNotEmpty()) {
        compilations
            .map { compilation -> compilation.compileClasspathOf(project = this) }
            .reduce(FileCollection::plus)
    } else {
        // Dokka suppresses source sets that do no have compilations
        // since such configuration is invalid, it reports a warning or an error
        sourceSet.withAllDependentSourceSets()
            .map { it.kotlin.sourceDirectories }
            .reduce(FileCollection::plus)
    }
}

private fun KotlinCompilation.compileClasspathOf(project: Project): FileCollection {
    val kgpVersion = project.getKgpVersion()

    // if KGP version < 1.9 or org.jetbrains.dokka.classpath.useOldResolution=true
    // we will use old (pre 1.9) resolution of classpath
    if (kgpVersion == null ||
        kgpVersion < KotlinGradlePluginVersion(1, 9, 0) ||
        project.classpathProperty("useOldResolution", default = false)
    ) {
        return oldCompileClasspathOf(project)
    }

    return newCompileClasspathOf(project)
}

private fun KotlinCompilation.newCompileClasspathOf(project: Project): FileCollection {
    val compilationClasspath = (compileTaskProvider.get() as? KotlinCompileTool)?.libraries ?: project.files()
    return compilationClasspath + platformDependencyFiles(project)
}

private fun KotlinCompilation.oldCompileClasspathOf(project: Project): FileCollection {
    if (this.target.isAndroidTarget()) { // Workaround for https://youtrack.jetbrains.com/issue/KT-33893
        return this.classpathOf(project)
    }

    return this.compileDependencyFiles + platformDependencyFiles(project) + this.classpathOf(project)
}

private fun KotlinCompilation.classpathOf(project: Project): FileCollection {
    val kgpVersion = project.getKgpVersion()
    val kotlinCompile = this.getKotlinCompileTask(kgpVersion) ?: return project.files()

    val shouldKeepBackwardsCompatibility = (kgpVersion != null && kgpVersion < KotlinGradlePluginVersion(1, 7, 0))
    return if (shouldKeepBackwardsCompatibility) {
        // removed since 1.9.0, left for compatibility with < Kotlin 1.7
        val classpathGetter = kotlinCompile::class.members
            .first { it.name == "getClasspath" }
        classpathGetter.call(kotlinCompile) as FileCollection
    } else {
        kotlinCompile.libraries // introduced in 1.7.0
    }
}

private fun KotlinCompilation.getKotlinCompileTask(kgpVersion: KotlinGradlePluginVersion? = null): KotlinCompile? {
    val shouldKeepBackwardsCompatibility = (kgpVersion != null && kgpVersion < KotlinGradlePluginVersion(1, 8, 0))
    return if (shouldKeepBackwardsCompatibility) {
        @Suppress("DEPRECATION") // for `compileKotlinTask` property, deprecated with warning since 1.8.0
        this.compileKotlinTask as? KotlinCompile
    } else {
        this.compileTaskProvider.get() as? KotlinCompile // introduced in 1.8.0
    }
}

private fun KotlinCompilation.platformDependencyFiles(project: Project): FileCollection {
    val excludePlatformDependencyFiles = project.classpathProperty("excludePlatformDependencyFiles", default = false)
    if (excludePlatformDependencyFiles) return project.files()

    val useNativeDistributionAccessor = project.classpathProperty("useNativeDistributionAccessor", default = false)
    if (useNativeDistributionAccessor) return this.getPlatformDependenciesFromNativeDistributionAccessor(project)

    val useKonanDistribution = project.classpathProperty("useKonanDistribution", default = false)
    if (useKonanDistribution) return this.getPlatfromDependenciesFromKonanDistribution(project)

    return (this as? AbstractKotlinNativeCompilation)
        ?.target?.project?.configurations
        ?.findByName(@Suppress("DEPRECATION") this.defaultSourceSet.implementationMetadataConfigurationName) // KT-58640
        ?: project.files()
}

// https://github.com/Kotlin/dokka/pull/3147
private fun KotlinCompilation.getPlatformDependenciesFromNativeDistributionAccessor(project: Project): FileCollection {
    return if (this is AbstractKotlinNativeCompilation) {
        val result = project.objects.fileCollection()
        val kotlinNativeDistributionAccessor = KotlinNativeDistributionAccessor(project)
        result.from(kotlinNativeDistributionAccessor.stdlibDir)
        result.from(kotlinNativeDistributionAccessor.platformDependencies(konanTarget))
        result
    } else {
        project.files()
    }
}

// -------- The hack for platform dependencies from compiler ------------------
// adapted from https://github.com/jetbrains/kotlin/blob/b6a215d681695a2fe0cc798308966c5675de447f/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/internal/KotlinNativePlatformDependencies.kt#L39
// https://github.com/Kotlin/dokka/pull/3145
private fun KotlinCompilation.getPlatfromDependenciesFromKonanDistribution(project: Project): FileCollection {
    return if (this is AbstractKotlinNativeCompilation)
        project.files(project.getStdLibFromKonanDistribution()) + project.files(project.getPlatformLibsFromKonanDistribution(this.konanTarget))
    else
        project.files()
}

private fun Project.getStdLibFromKonanDistribution(): File {
    val root = file(this.konanHome())
    val konanCommonLibraries = root.resolve(KONAN_DISTRIBUTION_KLIB_DIR).resolve(KONAN_DISTRIBUTION_COMMON_LIBS_DIR)
    val stdlib = konanCommonLibraries.resolve(KONAN_STDLIB_NAME)
    return stdlib
}

// copy-pasted from https://github.com/jetbrains/kotlin/blob/c9aeadd31f763646237faffab38a57923c520fa1/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/compilerRunner/nativeToolRunners.kt#L29
private fun Project.konanHome(): String {
    return nativeHome()?.let { file(it).absolutePath }
        ?: NativeCompilerDownloader(project).compilerDirectory.absolutePath
}

private fun Project.nativeHome(): String? =
    this.findProperty("org.jetbrains.kotlin.native.home") as? String

//copy-pasted from https://github.com/jetbrains/kotlin/blob/05a6d89151e6a7230faf733e51161b5f07ae10a7/native/commonizer/src/org/jetbrains/kotlin/commonizer/repository/KonanDistributionRepository.kt#L20
// https://github.com/jetbrains/kotlin/blob/b6a215d681695a2fe0cc798308966c5675de447f/libraries/tools/kotlin-gradle-plugin/src/common/kotlin/org/jetbrains/kotlin/gradle/targets/native/internal/KotlinNativePlatformDependencies.kt#L83
private fun Project.getPlatformLibsFromKonanDistribution(target: KonanTarget): Array<out File>? {
    val root = file(this.konanHome())
    val platformLibsDir = root.resolve(KONAN_DISTRIBUTION_KLIB_DIR).resolve(KONAN_DISTRIBUTION_PLATFORM_LIBS_DIR)
    val libs = platformLibsDir.resolve(target.name).takeIf { it.isDirectory }?.listFiles()
    return libs
}

private fun Project.classpathProperty(name: String, default: Boolean): Boolean =
    (findProperty("org.jetbrains.dokka.classpath.$name") as? String)?.toBoolean() ?: default
