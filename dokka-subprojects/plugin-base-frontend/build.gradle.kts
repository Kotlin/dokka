/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs

@Suppress("DSL_SCOPE_VIOLATION") // fixed in Gradle 8.1 https://github.com/gradle/gradle/pull/23639
plugins {
    id("dokkabuild.setup-html-frontend-files")
    alias(libs.plugins.gradleNode)
}

node {
    version.set(libs.versions.node)

    // https://github.com/node-gradle/gradle-node-plugin/blob/3.5.1/docs/faq.md#is-this-plugin-compatible-with-centralized-repositories-declaration
    download.set(true)
    distBaseUrl.set(null as String?) // Strange cast to avoid overload ambiguity
}

val distributionDirectory = layout.projectDirectory.dir("dist")

tasks.npmInstall {
    // enable caching - workaround for https://github.com/node-gradle/gradle-node-plugin/issues/81
    outputs.file(layout.projectDirectory.file("node_modules/.package-lock.json"))
        .withPropertyName("nodeModulesPackageLock")
    outputs.cacheIf { true }
}

val npmRunBuild by tasks.registering(NpmTask::class) {
    dependsOn(tasks.npmInstall)

    npmCommand.set(parseSpaceSeparatedArgs("run build"))

    inputs.dir("src/main")
        .withPropertyName("mainSources")
        .withPathSensitivity(RELATIVE)

    inputs.files(
        "package.json",
        "webpack.config.js",
    )
        .withPropertyName("javascriptConfigFiles")
        .withPathSensitivity(RELATIVE)

    inputs.dir(layout.projectDirectory.dir("node_modules"))
        .withPathSensitivity(RELATIVE)
        .withPropertyName("nodeModulesDir")

    outputs.dir(distributionDirectory)
        .withPropertyName("distributionDirectory")

    outputs.cacheIf { true }
}

configurations.dokkaHtmlFrontendFilesElements.configure {
    outgoing {
        artifact(distributionDirectory) {
            builtBy(npmRunBuild)
        }
    }
}

tasks.clean {
    delete(
        file("node_modules"),
        file("dist"),
    )
}
