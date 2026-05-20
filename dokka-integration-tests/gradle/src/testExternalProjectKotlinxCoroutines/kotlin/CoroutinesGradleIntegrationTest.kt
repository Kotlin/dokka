/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle.kotlin

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

class CoroutinesBuildVersionsArgumentsProvider : ArgumentsProvider {
    private val buildVersions = BuildVersions.permutations(
        gradleVersions = listOf("8.13"), // should be consistent with Gradle version used in project gradle-wrapper.properties
        kotlinVersions = listOf("2.2.20") // not used, as we don't override it in external (git-based) projects
    )

    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return buildVersions.stream().map { Arguments.of(it) }
    }
}

class CoroutinesGradleIntegrationTest : AbstractGradleIntegrationTest(), TestOutputCopier {

    override val projectOutputLocation: File by lazy { File(projectDir, "build/dokka/html") }

    @BeforeTest
    override fun beforeEachTest() {
        prepareProjectFiles()
        copyAndApplyGitDiff(
            projectDir.toPath(),
            templateProjectDir.parent.resolve("coroutines.diff"),
        )
        projectDir.toPath().updateProjectLocalMavenDir()
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(CoroutinesBuildVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions,
            ":dokkaGenerate",
            // disabled because:
            // #4482:
            //  Unknown annotation value `ExperimentalWasmInterop::class` in kotlinx-coroutines-core/wasmWasi/src/internal/CoroutineExceptionHandlerImpl.kt:14:8
            //  Unknown annotation value `ExperimentalWasmInterop::class` in kotlinx-coroutines-core/wasmWasi/src/internal/CoroutineExceptionHandlerImpl.kt:51:8
            // https://github.com/Kotlin/kotlinx.coroutines/pull/4641:
            //  there are a lot of unresolved links in coroutines - which will be potentially fixed in this PR
            "-Pdokka_it_failOnWarning=false"
        ).buildRelaxed()

        assertContains(
            setOf(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE),
            assertNotNull(result.task(":dokkaGeneratePublicationHtml")).outcome
        )

        assertTrue(projectOutputLocation.isDirectory, "Missing dokka output directory")

        projectOutputLocation.allHtmlFiles().forEach { file ->
            assertContainsNoErrorClass(file)
            assertNoUnresolvedLinks(file)
            assertNoEmptyLinks(file)
            assertNoEmptySpans(file)
            assertNoUnsubstitutedTemplatesInHtml(file)
        }

        assertHrefMissing(
            projectOutputLocation,
            mapOf(
                "kotlinx-coroutines-rx3/kotlinx.coroutines.rx3/index.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                ),
                "kotlinx-coroutines-rx3/kotlinx.coroutines.rx3/-scheduler-coroutine-dispatcher/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                ),
                "kotlinx-coroutines-rx2/kotlinx.coroutines.rx2/index.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                ),
                "kotlinx-coroutines-rx2/kotlinx.coroutines.rx2/-scheduler-coroutine-dispatcher/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                ),
                "kotlinx-coroutines-swing/kotlinx.coroutines.swing/index.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                ),
                "kotlinx-coroutines-swing/kotlinx.coroutines.swing/-swing-dispatcher/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                ),
                "kotlinx-coroutines-guava/kotlinx.coroutines.guava/as-listenable-future.html" to setOf(
                    "../../kotlinx-coroutines-guava/kotlinx.coroutines.guava/-job-listenable-future/cancel.html" to "kotlinx-coroutines-guava/kotlinx.coroutines.guava/-job-listenable-future/cancel.html",
                ),
                "kotlinx-coroutines-slf4j/kotlinx.coroutines.slf4j/-m-d-c-context/index.html" to setOf(
                    "../../../kotlinx-coroutines-slf4j/kotlinx.coroutines.slf4j/-m-d-c-context/--root--.html" to "kotlinx-coroutines-slf4j/kotlinx.coroutines.slf4j/-m-d-c-context/--root--.html",
                ),
                "kotlinx-coroutines-debug/kotlinx.coroutines.debug.junit5/index.html" to setOf(
                    "../../kotlinx-coroutines-debug/kotlinx.coroutines.debug.junit5/-coroutines-timeout-extension/index.html" to "kotlinx-coroutines-debug/kotlinx.coroutines.debug.junit5/-coroutines-timeout-extension/index.html",
                ),
                "kotlinx-coroutines-debug/kotlinx.coroutines.debug.junit5/-coroutines-timeout/index.html" to setOf(
                    "../../../kotlinx-coroutines-debug/kotlinx.coroutines.debug.junit5/-coroutines-timeout-extension/index.html" to "kotlinx-coroutines-debug/kotlinx.coroutines.debug.junit5/-coroutines-timeout-extension/index.html",
                ),
                "kotlinx-coroutines-debug/kotlinx.coroutines.debug.junit4/-coroutines-timeout/index.html" to setOf(
                    "../../../kotlinx-coroutines-debug/kotlinx.coroutines.debug.junit4/-coroutines-timeout/cancel-on-timeout.html" to "kotlinx-coroutines-debug/kotlinx.coroutines.debug.junit4/-coroutines-timeout/cancel-on-timeout.html",
                    "../../../kotlinx-coroutines-debug/kotlinx.coroutines.debug.junit4/-coroutines-timeout/enable-coroutine-creation-stack-traces.html" to "kotlinx-coroutines-debug/kotlinx.coroutines.debug.junit4/-coroutines-timeout/enable-coroutine-creation-stack-traces.html",
                ),
                "kotlinx-coroutines-javafx/kotlinx.coroutines.javafx/index.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                ),
                "kotlinx-coroutines-javafx/kotlinx.coroutines.javafx/-java-fx-dispatcher/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                ),
                "kotlinx-coroutines-reactor/kotlinx.coroutines.reactor/index.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                    "../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/send.html",
                ),
                "kotlinx-coroutines-reactor/kotlinx.coroutines.reactor/flux.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/send.html",
                ),
                "kotlinx-coroutines-reactor/kotlinx.coroutines.reactor/-scheduler-coroutine-dispatcher/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                ),
                "kotlinx-coroutines-jdk9/kotlinx.coroutines.jdk9/flow-publish.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/send.html",
                ),
                "kotlinx-coroutines-reactive/kotlinx.coroutines.reactive/publish.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/send.html",
                ),
                "kotlinx-coroutines-android/kotlinx.coroutines.android/index.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                ),
                "kotlinx-coroutines-android/kotlinx.coroutines.android/-handler-dispatcher/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                ),
                "kotlinx-coroutines-test/kotlinx.coroutines.test/index.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                    "../../kotlinx-coroutines-core/kotlinx.coroutines/-delay-with-timeout-diagnostics/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay-with-timeout-diagnostics/index.html",
                ),
                "kotlinx-coroutines-test/kotlinx.coroutines.test/-test-dispatcher/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay/index.html",
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines/-delay-with-timeout-diagnostics/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-delay-with-timeout-diagnostics/index.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/-factory/-c-o-n-f-l-a-t-e-d.html" to setOf(
                    "../../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/send.html",
                    "../../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/try-send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/try-send.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/-factory/-r-e-n-d-e-z-v-o-u-s.html" to setOf(
                    "../../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/send.html",
                    "../../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/receive.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/receive.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.channels/consume-each.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/index.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.channels/index.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/index.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.channels/-actor-scope/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-actor-scope/receive.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-actor-scope/receive.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.channels/-actor-scope/channel.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-actor-scope/receive.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-actor-scope/receive.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/send.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/channel.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-producer-scope/send.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel-result/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/try-send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/try-send.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.channels/try-send-blocking.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/send.html",
                    "../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel-result/-companion/failed.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel-result/-companion/failed.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.channels/consume.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/index.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.flow/index.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines.flow/-cancellable-flow/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines.flow/-cancellable-flow/index.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-conflated-broadcast-channel/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-conflated-broadcast-channel/index.html",
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-conflated-broadcast-channel/send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-conflated-broadcast-channel/send.html",
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-conflated-broadcast-channel/try-send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-conflated-broadcast-channel/try-send.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-shared-flow/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-shared-flow/replay-cache.html" to "kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-shared-flow/replay-cache.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-shared-flow/reset-replay-cache.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-shared-flow/replay-cache.html" to "kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-shared-flow/replay-cache.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.flow/receive-as-flow.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/receive.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/receive.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.flow/-shared-flow/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/index.html",
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/send.html",
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/try-send.html" to "kotlinx-coroutines-core/kotlinx.coroutines.channels/-broadcast-channel/try-send.html",
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-state-flow/emit.html" to "kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-state-flow/emit.html",
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-state-flow/try-emit.html" to "kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-state-flow/try-emit.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-state-flow/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-state-flow/emit.html" to "kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-state-flow/emit.html",
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-state-flow/try-emit.html" to "kotlinx-coroutines-core/kotlinx.coroutines.flow/-mutable-state-flow/try-emit.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.flow/-abstract-flow/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.flow/-cancellable-flow/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines.flow/-cancellable-flow/index.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines/-completion-handler/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines/-completion-handler-exception/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-completion-handler-exception/index.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines/-copyable-thread-context-element/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines/-copyable-thread-context-element/restore-thread-context.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-copyable-thread-context-element/restore-thread-context.html",
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines/-copyable-thread-context-element/update-thread-context.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-copyable-thread-context-element/update-thread-context.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines/-job/invoke-on-completion.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines/-completion-handler-exception/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines/-completion-handler-exception/index.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.selects/index.html" to setOf(
                    "../../kotlinx-coroutines-core/kotlinx.coroutines.selects/-select-clause/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines.selects/-select-clause/index.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.selects/-select-clause1/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.selects/-select-clause/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines.selects/-select-clause/index.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.selects/-select-clause0/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.selects/-select-clause/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines.selects/-select-clause/index.html",
                ),
                "kotlinx-coroutines-core/kotlinx.coroutines.selects/-select-clause2/index.html" to setOf(
                    "../../../kotlinx-coroutines-core/kotlinx.coroutines.selects/-select-clause/index.html" to "kotlinx-coroutines-core/kotlinx.coroutines.selects/-select-clause/index.html",
                ),
            )
        )
    }
}
