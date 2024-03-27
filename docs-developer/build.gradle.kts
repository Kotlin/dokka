/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import com.pswidersk.gradle.python.VenvTask
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs

plugins {
    id("com.pswidersk.python-plugin") version "2.4.0"
}

val dokkaVersion = provider { project.version.toString() }
val isDokkaSnapshotVersion = dokkaVersion.map { it.endsWith("-SNAPSHOT") }

/** Directory containing generated docs. */
val currentMkDocsDir: Provider<Directory> = layout.buildDirectory.dir("mkdocs-current")

/**
 * Directory containing a fully built MkDocs site, ready for upload to GitHub Pages.
 *
 * The [currentMkDocsDir] must be placed in a [dokkaVersion] subdirectory.
 *
 * ```.
 * └── build/
 *     └── mkdocs/
 *         └── 1.2.3-SNAPSHOT/
 *             └── <content>
 * ```
 *
 * If [dokkaVersion] is a release version (non-SNAPSHOT) then the dir must contain an index.html
 * that redirects to the current version.
 *
 * ```
 * └── build/
 *     ├── mkdocs/
 *     │   └── 1.2.3/
 *     │       └── <content>
 *     └── index.html (redirects to `1.2.3/index.html`)
 * ```
 */
val mkDocsSiteDir: Provider<Directory> = layout.buildDirectory.dir("mkdocs")

val mkDocsSetup: TaskProvider<VenvTask> by tasks.registering(VenvTask::class) {
    description = "Install required Python libraries"
    group = project.name
    venvExec = "pip"
    args = parseSpaceSeparatedArgs("install -r requirements.txt")

    inputs.file("requirements.txt")
        .withPropertyName("requirements.txt")
        .withPathSensitivity(RELATIVE)
        .normalizeLineEndings()
}

val pipPrintRequirements by tasks.registering(VenvTask::class) {
    description = "Log the current requirements (useful util for manually updating dependencies & requirements.txt)"
    group = project.name
    venvExec = "pip"
    args = parseSpaceSeparatedArgs("freeze")
}

val mkDocsBuild: TaskProvider<VenvTask> by tasks.registering(VenvTask::class) {
    description = "Compile Dokka Developer Documentation site using MkDocs"
    group = project.name

    dependsOn(mkDocsSetup)
    finalizedBy(logMkDocsLink)

    venvExec = "mkdocs"
    args = parseSpaceSeparatedArgs("build")

    inputs.dir("docs")
        .withPropertyName("docs")
        .withPathSensitivity(RELATIVE)
        .normalizeLineEndings()

    inputs.file("mkdocs.yml")
        .withPropertyName("mkdocs.yml")
        .withPathSensitivity(RELATIVE)
        .normalizeLineEndings()

    // output directory is also specified in mkdocs.yml as `site_dir`
    outputs.dir(currentMkDocsDir)
        .withPropertyName("mkDocsOutputDir")

    outputs.cacheIf("always cache - task has defined output directory") { true }
}

val generateMkDocsSiteIndexHtml by tasks.registering(Sync::class) {
    description = "Generate the root index.html"
    group = project.name

    val dokkaVersion = dokkaVersion
    inputs.property("dokkaVersion", dokkaVersion)

    val indexHtml = layout.projectDirectory.file("index.html")
    inputs.file(indexHtml)
        .withPropertyName("indexHtml")
        .withPathSensitivity(RELATIVE)
        .normalizeLineEndings()

    from(indexHtml)

    filter<ReplaceTokens>("tokens" to mapOf("dokkaVersion" to dokkaVersion.get()))

    into(temporaryDir)
}


val buildMkDocsSite by tasks.registering(Sync::class) {
    description = "Generate a complete MkDocs site, with versioned subdirectories"
    group = project.name

    val dokkaVersion = dokkaVersion
    inputs.property("dokkaVersion", dokkaVersion)

// only generate a root index.html on non-snapshot versions
    if (!isDokkaSnapshotVersion.get()) {
        from(generateMkDocsSiteIndexHtml)
    }
    from(mkDocsBuild) {
        eachFile {
            relativePath = relativePath.prepend(dokkaVersion.get())
        }
    }

    includeEmptyDirs = false

    into(mkDocsSiteDir)
}

val logMkDocsLink: TaskProvider<Task> by tasks.registering {
    description = "Prints a link to the generated docs"
    group = project.name

    dependsOn(buildMkDocsSite)
    doNotTrackState("this task performs no work and should always log the link")

    // redefine currentMkDocsDir for config-cache compliance
    val mkDocsDir = buildMkDocsSite.map { it.destinationDir }

    val ideaActive = System.getProperty("idea.active").toBoolean()

    val dokkaProjectDir = project.rootDir.parentFile

    val dokkaVersion = dokkaVersion
    val isDokkaSnapshotVersion = isDokkaSnapshotVersion

    doLast {
        val indexHtml = if (isDokkaSnapshotVersion.get()) {
            mkDocsDir.get().resolve("${dokkaVersion.get()}/index.html")
        } else {
            mkDocsDir.get().resolve("index.html")
        }

        val link: String =
            if (ideaActive) {
                // The built-in IntelliJ server requires a specific path to index.html:
                // a _relative_ path starting with the project's directory (dokka)
                val relativeOutputPath = indexHtml.relativeTo(dokkaProjectDir).invariantSeparatorsPath
                "http://localhost:63342/$relativeOutputPath"
            } else {
                indexHtml.toURI().toString()
            }

        logger.quiet(
            """
            |
            |***************************************************************************************************
            |    
            |    Dokka Developer Docs: $link
            |
            |***************************************************************************************************
            |
            """.trimMargin()
        )
    }
}
