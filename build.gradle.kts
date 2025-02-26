/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.base")
    idea
}

val publishedIncludedBuilds = listOf("runner-cli", "dokka-gradle-plugin", "runner-maven-plugin")
val gradlePluginIncludedBuilds = listOf("dokka-gradle-plugin")

addDependencyOnSameTasksOfIncludedBuilds("assemble", "build", "clean", "check")

registerParentGroupTasks(
    "publishing", taskNames = listOf(
        "publishAllPublicationsToMavenCentralRepository",
        "publishAllPublicationsToProjectLocalRepository",
        "publishAllPublicationsToSnapshotRepository",
        "publishAllPublicationsToSpaceDevRepository",
        "publishAllPublicationsToSpaceTestRepository",
        "publishToMavenLocal",
    )
) {
    it.name in publishedIncludedBuilds
}

registerParentGroupTasks(
    "gradle plugin", taskNames = listOf(
        "publishPlugins",
        "validatePlugins",
    )
) {
    it.name in gradlePluginIncludedBuilds
}

registerParentGroupTasks(
    "bcv", taskNames = listOf(
        "apiDump",
        "apiCheck",
        "apiBuild",
    )
) {
    it.name in publishedIncludedBuilds
}

registerParentGroupTasks(
    "verification", taskNames = listOf(
        "test",
    )
)

tasks.register("integrationTest") {
    group = "verification"
    description = "Runs integration tests of this project. Might take a while and require additional setup."

    dependsOn(includedBuildTasks("integrationTest") {
        it.name == "dokka-integration-tests"
    })
}

fun addDependencyOnSameTasksOfIncludedBuilds(vararg taskNames: String) {
    taskNames.forEach { taskName ->
        tasks.named(taskName) {
            dependsOn(includedBuildTasks(taskName))
        }
    }
}

fun registerParentGroupTasks(
    groupName: String,
    taskNames: List<String>,
    includedBuildFilter: (IncludedBuild) -> Boolean = { true }
) = taskNames.forEach { taskName ->
    tasks.register(taskName) {
        group = groupName
        description = "A parent task that calls tasks with the same name in all subprojects and included builds"

        dependsOn(subprojectTasks(taskName), includedBuildTasks(taskName, includedBuildFilter))
    }
}

fun subprojectTasks(taskName: String): List<String> =
    subprojects
        .filter { it.getTasksByName(taskName, false).isNotEmpty() }
        .map { ":${it.path}:$taskName" }


fun includedBuildTasks(taskName: String, filter: (IncludedBuild) -> Boolean = { true }): List<TaskReference> =
    gradle.includedBuilds
        .filter { it.name != "build-logic" && it.name != "build-settings-logic" }
        .filter(filter)
        .mapNotNull { it.task(":$taskName") }


tasks.wrapper {
    doLast {
        // Manually update the distribution URL to use cache-redirector.
        // (Workaround for https://github.com/gradle/gradle/issues/17515)
        propertiesFile.writeText(
            propertiesFile.readText()
                .replace(
                    "https\\://services.gradle.org/",
                    "https\\://cache-redirector.jetbrains.com/services.gradle.org/",
                )
        )
    }
}

idea {
    module {
        // Mark directories as excluded so that they don't appear in IntelliJ's global search.
        excludeDirs.addAll(
            files(
                ".idea",
                ".husky",
                ".kotlin",
                "dokka-integration-tests/.kotlin",
                "dokka-runners/dokka-gradle-plugin/.kotlin",
                "dokka-runners/runner-cli/.kotlin",
                "dokka-runners/runner-maven-plugin/.kotlin",
                "dokka-runners/dokka-gradle-plugin/src/testFunctional/resources/KotlinDslAccessorsTest/",

                "dokka-integration-tests/gradle/src/testExampleProjects/expectedData",
                "dokka-integration-tests/gradle/projects/it-android/expectedData",
                "dokka-integration-tests/gradle/projects/it-multiplatform-android-jvm/expectedData",
                "dokka-integration-tests/gradle/projects/it-android-compose/expectedData",
                "dokka-integration-tests/gradle/projects/it-kotlin-multiplatform/expectedData",
                "dokka-integration-tests/gradle/projects/it-android-kotlin-mp-builtin/expectedData",

                "examples/gradle/dokka-versioning-multimodule-example/previousDocVersions/",
                "examples/gradle-v2/versioning-multimodule-example/docs/previousDocVersions/",
            )
        )
    }
}

subprojects {
// !! infrastructure for builds as a Kotlin user project
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach kotlinCompilationTask@ {
        // ("unconditionally pass the -Xreport-all-warnings CLI option to the Kotlin compiler")
        // ("unconditionally pass the -Xrender-internal-diagnostic-names CLI option to the Kotlin compiler")
        compilerOptions {
            // output reported warnings even in the presence of reported errors
            freeCompilerArgs.add("-Xreport-all-warnings")
            logger.info("<KUP> ${this@kotlinCompilationTask.path} : added -Xreport-all-warnings")
            // output kotlin.git-searchable names of reported diagnostics
            freeCompilerArgs.add("-Xrender-internal-diagnostic-names")
            logger.info("<KUP> ${this@kotlinCompilationTask.path} : added -Xrender-internal-diagnostic-names")
        }
    }

// !! infrastructure for builds as a Kotlin user project
// ("implement a Gradle property for passing additional CLI options to the Kotlin compiler")
    val kotlinAdditionalCliOptions = providers.gradleProperty("kotlin_additional_cli_options")
        .map { if (it.isNotBlank()) it.split(" ") else emptyList() } // i sure hope that this level of parsing is in fact enough
        .orNull
    if (kotlinAdditionalCliOptions != null) {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach kotlinCompilationTask@ {
            compilerOptions {
                freeCompilerArgs.addAll(kotlinAdditionalCliOptions)
                logger.info("<KUP> ${this@kotlinCompilationTask.path} : added ${kotlinAdditionalCliOptions.joinToString(" ")}")
            }
        }
    }

// !! infrastructure for builds as a Kotlin user project
// ("unconditionally disable the -Werror compiler CLI option")
    afterEvaluate {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach kotlinCompilationTask@ {
            compilerOptions {
                allWarningsAsErrors.set(false)
                freeCompilerArgs.set(
                    freeCompilerArgs.getOrElse(emptyList()).filterNot { it == "-Werror" }
                )
                logger.info("<KUP> ${this@kotlinCompilationTask.path} : disabled -Werror")
            }
        }
    }

// !! infrastructure for builds as a Kotlin user project
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach kotlinCompilationTask@ {
        // ("pass the -Wextra CLI option to the Kotlin compiler when the -Werror compiler CLI option is disabled")
        // ("pass the -Xuse-fir-experimental-checkers CLI option to the Kotlin compiler when the -Werror compiler CLI option is disabled")
        compilerOptions {
            // opt into additional warning-reporting compilation checks
            freeCompilerArgs.add("-Wextra")
            logger.info("<KUP> ${this@kotlinCompilationTask.path} : added -Wextra")
            // opt into additional compilation checks hidden from users
            freeCompilerArgs.add("-Xuse-fir-experimental-checkers")
            logger.info("<KUP> ${this@kotlinCompilationTask.path} : added -Xuse-fir-experimental-checkers")
        }
    }
}