/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

import com.github.gradle.node.npm.task.NpmTask
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs

plugins {
    id("dokkabuild.setup-html-frontend-files")
    alias(libs.plugins.gradleNode)
}

node {
    version = libs.versions.node

    // https://github.com/node-gradle/gradle-node-plugin/blob/3.5.1/docs/faq.md#is-this-plugin-compatible-with-centralized-repositories-declaration
    download = true
    distBaseUrl = null as String? // Strange cast to avoid overload ambiguity

    // Stop Gradle from monitoring node_modules dir; it will be managed by NPM. This helps performance and task-avoidance.
    fastNpmInstall = true
}

val distributionDirectory = layout.projectDirectory.dir("dist")

val npmRunBuild by tasks.registering(NpmTask::class) {
    dependsOn(tasks.npmInstall)

    npmCommand = parseSpaceSeparatedArgs("run build")

    inputs.dir("src/main")
        .withPropertyName("mainSources")
        .withPathSensitivity(RELATIVE)

    inputs.files(
        layout.projectDirectory.asFileTree.matching {
            include(
                "package.json",
                "tsconfig.json",
                "*.config.js",
            )
        }
    )
        .withPropertyName("javascriptConfigFiles")
        .withPathSensitivity(RELATIVE)

    outputs.cacheIf("always cache, because this task has a defined output directory") { true }

    outputs.dir(distributionDirectory)
        .withPropertyName("distributionDirectory")
}

configurations.dokkaHtmlFrontendFilesElements.configure {
    outgoing {
        artifact(distributionDirectory) {
            builtBy(npmRunBuild)
        }
    }
}

tasks.clean {
    delete(distributionDirectory)
}
