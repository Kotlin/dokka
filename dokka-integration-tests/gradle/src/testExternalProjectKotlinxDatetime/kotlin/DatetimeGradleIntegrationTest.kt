/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.kotlin

import io.kotest.matchers.file.shouldContainFile
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.dokka.it.TestOutputCopier
import org.jetbrains.dokka.it.copyAndApplyGitDiff
import org.jetbrains.dokka.it.gradle.AbstractGradleIntegrationTest
import org.jetbrains.dokka.it.gradle.BuildVersions
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import java.util.stream.Stream
import kotlin.test.BeforeTest
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatetimeBuildVersionsArgumentsProvider : ArgumentsProvider {
    private val buildVersions = BuildVersions.permutations(
        gradleVersions = listOf("8.5"), // should be consistent with Gradle version used in project gradle-wrapper.properties
        kotlinVersions = listOf("2.1.20") // not used, as we don't override it in external (git-based) projects
    )

    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return buildVersions.stream().map { Arguments.of(it) }
    }
}

class DatetimeGradleIntegrationTest : AbstractGradleIntegrationTest(), TestOutputCopier {

    override val projectOutputLocation: File by lazy { File(projectDir, "core/build/dokka/html") }

    @BeforeTest
    override fun beforeEachTest() {
        prepareProjectFiles()
        copyAndApplyGitDiff(
            projectDir.toPath(),
            templateProjectDir.parent.resolve("datetime.diff"),
        )
        projectDir.toPath().updateProjectLocalMavenDir()
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(DatetimeBuildVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions,
            ":kotlinx-datetime:dokkaGenerate",
            "-Pdokka_it_failOnWarning=true"
        ).buildRelaxed()

        assertContains(
            setOf(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE),
            assertNotNull(result.task(":kotlinx-datetime:dokkaGeneratePublicationHtml")).outcome
        )

        assertTrue(projectOutputLocation.isDirectory, "Missing dokka output directory")

        projectOutputLocation.shouldContainFile("index.html")

        projectOutputLocation.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
        }

        // skipped deprecated declarations - https://github.com/Kotlin/dokka/issues/4448
        // constructor parameters (--root--.html) -
        // internal declarations in annotations - https://github.com/Kotlin/dokka/issues/4448
        assertHrefMissing(
            projectOutputLocation,
            mapOf(
                "kotlinx-datetime/kotlinx.datetime.format/-date-time-components/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html" to "kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime.format/-date-time-components/to-instant-using-offset.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html" to "kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-instant-serializer/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-formatted-instant-serializer/format.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-instant-serializer/format.html",
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-formatted-instant-serializer/--root--.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-instant-serializer/--root--.html",
                ),
                "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-local-date-time-serializer/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-formatted-local-date-time-serializer/--root--.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-local-date-time-serializer/--root--.html",
                ),
                "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-year-month-serializer/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-formatted-year-month-serializer/--root--.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-year-month-serializer/--root--.html",
                ),
                "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-utc-offset-serializer/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-formatted-utc-offset-serializer/--root--.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-utc-offset-serializer/--root--.html",
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-utc-offset-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-utc-offset-serializer/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-local-time-serializer/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-formatted-local-time-serializer/--root--.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-local-time-serializer/--root--.html",
                ),
                "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-local-date-serializer/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-formatted-local-date-serializer/--root--.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-formatted-local-date-serializer/--root--.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/to-j-s-date.html" to setOf(
                    "../../kotlinx-datetime/kotlinx.datetime/-instant/index.html" to "kotlinx-datetime/kotlinx.datetime/-instant/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/at-start-of-day-in.html" to setOf(
                    "../../kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html" to "kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/to-deprecated-clock.html" to setOf(
                    "../../kotlinx-datetime/kotlinx.datetime/-clock/index.html" to "kotlinx-datetime/kotlinx.datetime/-clock/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-date-time-period/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-date-time-period-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-date-time-period-serializer/index.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-instant/plus.html" to "kotlinx-datetime/kotlinx.datetime/-instant/plus.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-date-period/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-date-period-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-date-period-serializer/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-date-period/-date-period.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-date-period/--root--.html" to "kotlinx-datetime/kotlinx.datetime/-date-period/--root--.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/to-stdlib-instant.html" to setOf(
                    "../../kotlinx-datetime/kotlinx.datetime/-instant/index.html" to "kotlinx-datetime/kotlinx.datetime/-instant/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/index.html" to setOf(
                    "../../kotlinx-datetime/kotlinx.datetime.serializers/-date-period-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-date-period-serializer/index.html",
                    "../../kotlinx-datetime/kotlinx.datetime.serializers/-date-time-period-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-date-time-period-serializer/index.html",
                    "../../kotlinx-datetime/kotlinx.datetime.serializers/-local-date-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-local-date-serializer/index.html",
                    "../../kotlinx-datetime/kotlinx.datetime.serializers/-local-date-time-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-local-date-time-serializer/index.html",
                    "../../kotlinx-datetime/kotlinx.datetime.serializers/-local-time-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-local-time-serializer/index.html",
                    "../../kotlinx-datetime/kotlinx.datetime.serializers/-utc-offset-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-utc-offset-serializer/index.html",
                    "../../kotlinx-datetime/kotlinx.datetime.serializers/-year-month-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-year-month-serializer/index.html",
                    "../../kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html" to "kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html",
                    "../../kotlinx-datetime/kotlinx.datetime/-clock/index.html" to "kotlinx-datetime/kotlinx.datetime/-clock/index.html",
                    "../../kotlinx-datetime/kotlinx.datetime/-instant/index.html" to "kotlinx-datetime/kotlinx.datetime/-instant/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/to-stdlib-clock.html" to setOf(
                    "../../kotlinx-datetime/kotlinx.datetime/-clock/index.html" to "kotlinx-datetime/kotlinx.datetime/-clock/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-local-date/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-local-date-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-local-date-serializer/index.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html" to "kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-local-date/-local-date.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-local-date/--root--.html" to "kotlinx-datetime/kotlinx.datetime/-local-date/--root--.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-utc-offset/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-utc-offset-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-utc-offset-serializer/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-year-month/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-year-month-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-year-month-serializer/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-year-month/-year-month.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-year-month/--root--.html" to "kotlinx-datetime/kotlinx.datetime/-year-month/--root--.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-local-iso-week-date/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-p-a-s-t.html" to "kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-p-a-s-t.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-f-u-t-u-r-e.html" to "kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-f-u-t-u-r-e.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-local-iso-week-date/iso-week-number.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-local-iso-week-date/--root--.html" to "kotlinx-datetime/kotlinx.datetime/-local-iso-week-date/--root--.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-local-iso-week-date/-local-iso-week-date.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-local-iso-week-date/--root--.html" to "kotlinx-datetime/kotlinx.datetime/-local-iso-week-date/--root--.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-p-a-s-t.html" to "kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-p-a-s-t.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-f-u-t-u-r-e.html" to "kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-f-u-t-u-r-e.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/to-deprecated-instant.html" to setOf(
                    "../../kotlinx-datetime/kotlinx.datetime/-instant/index.html" to "kotlinx-datetime/kotlinx.datetime/-instant/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-local-date-time/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-local-date-time-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-local-date-time-serializer/index.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-p-a-s-t.html" to "kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-p-a-s-t.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-f-u-t-u-r-e.html" to "kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-f-u-t-u-r-e.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-local-date-time/--root--.html" to "kotlinx-datetime/kotlinx.datetime/-local-date-time/--root--.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html" to "kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-local-date-time/-local-date-time.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-local-date-time/--root--.html" to "kotlinx-datetime/kotlinx.datetime/-local-date-time/--root--.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-p-a-s-t.html" to "kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-p-a-s-t.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-f-u-t-u-r-e.html" to "kotlinx-datetime/kotlinx.datetime/-instant/-companion/-d-i-s-t-a-n-t_-f-u-t-u-r-e.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-local-date-time/month-number.html" to "kotlinx-datetime/kotlinx.datetime/-local-date-time/month-number.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/to-instant.html" to setOf(
                    "../../kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html" to "kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-time-zone/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html" to "kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-time-zone/to-instant.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html" to "kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-fixed-offset-time-zone/-fixed-offset-time-zone.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-fixed-offset-time-zone/--root--.html" to "kotlinx-datetime/kotlinx.datetime/-fixed-offset-time-zone/--root--.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-fixed-offset-time-zone/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-fixed-offset-time-zone/--root--.html" to "kotlinx-datetime/kotlinx.datetime/-fixed-offset-time-zone/--root--.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/to-kotlin-instant.html" to setOf(
                    "../../kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html" to "kotlinx-datetime/kotlinx.datetime/-overload-marker/index.html",
                    "../../kotlinx-datetime/kotlinx.datetime/-instant/index.html" to "kotlinx-datetime/kotlinx.datetime/-instant/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-local-time/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime.serializers/-local-time-serializer/index.html" to "kotlinx-datetime/kotlinx.datetime.serializers/-local-time-serializer/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-local-time/-local-time.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-local-time/--root--.html" to "kotlinx-datetime/kotlinx.datetime/-local-time/--root--.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-date-time-unit/index.html" to setOf(
                    "../../../kotlinx-datetime/kotlinx.datetime/-instant/plus.html" to "kotlinx-datetime/kotlinx.datetime/-instant/plus.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-instant/minus.html" to "kotlinx-datetime/kotlinx.datetime/-instant/minus.html",
                    "../../../kotlinx-datetime/kotlinx.datetime/-instant/index.html" to "kotlinx-datetime/kotlinx.datetime/-instant/index.html",
                ),
                "kotlinx-datetime/kotlinx.datetime/-date-time-unit/-date-based/index.html" to setOf(
                    "../../../../kotlinx-datetime/kotlinx.datetime/-instant/index.html" to "kotlinx-datetime/kotlinx.datetime/-instant/index.html",
                ),
            )
        )
    }
}
