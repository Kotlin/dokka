/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.it.gradle

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.dokka.gradle.utils.GradleProjectTest
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.walk

/**
 * Gradle creates an HTML report for each Configuration Cache result.
 * This report contains JSON data that we can parse to get fine-grained details about the
 * Configuration Cache result.
 *
 * Only the first CC report will be parsed, so make sure to clean the CC report directory
 * before running any Gradle tasks to ensure only one report is found.
 */
fun GradleProjectTest.loadConfigurationCacheReportData(): ConfigurationCacheReportData? {
    val ccReportFile = projectDir.resolve("build/reports/configuration-cache")
        .walk()
        .filter { it.isRegularFile() }
        .singleOrNull()
        ?: return null

    return parseCCReportData(ccReportFile)
}

@Serializable
data class ConfigurationCacheReportData(
    val diagnostics: List<DiagnosticsItem>,
    val totalProblemCount: Int,
    val buildName: String,
    val requestedTasks: String,
    val cacheAction: String,
    val documentationLink: String,
) {

    @Serializable
    data class DiagnosticsItem(
        val trace: List<Trace>,
        val input: List<Input>,
        val documentationLink: String? = null,
    )

    @Serializable
    data class Trace(
        val kind: String,
        val type: String? = null,
        val location: String? = null,
    )

    @Serializable
    data class Input(
        val text: String? = null,
        val name: String? = null,
    )
}

/**
 * Extract and parse the JSON data from [ccReport].
 */
private fun parseCCReportData(ccReport: Path): ConfigurationCacheReportData {
    val reportData = ccReport.readText()
        .substringAfter("// begin-report-data", "could not find 'begin-report-data'")
        .substringBefore("// end-report-data", "could not find 'end-report-data'")

    return Json.decodeFromString<ConfigurationCacheReportData>(reportData)
}
