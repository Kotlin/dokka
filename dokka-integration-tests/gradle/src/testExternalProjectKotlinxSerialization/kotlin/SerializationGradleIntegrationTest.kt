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

class SerializationBuildVersionsArgumentsProvider : ArgumentsProvider {
    private val buildVersions = BuildVersions.permutations(
        gradleVersions = listOf("8.7"), // should be consistent with Gradle version used in project gradle-wrapper.properties
        kotlinVersions = listOf("2.3.0") // not used, as we don't override it in external (git-based) projects
    )

    override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
        return buildVersions.stream().map { Arguments.of(it) }
    }
}

class SerializationGradleIntegrationTest : AbstractGradleIntegrationTest(), TestOutputCopier {

    override val projectOutputLocation: File by lazy { File(projectDir, "build/dokka/html") }

    @BeforeTest
    override fun beforeEachTest() {
        prepareProjectFiles()
        copyAndApplyGitDiff(
            projectDir.toPath(),
            templateProjectDir.parent.resolve("serialization.diff"),
        )
        projectDir.toPath().updateProjectLocalMavenDir()
    }

    @ParameterizedTest(name = "{0}")
    @ArgumentsSource(SerializationBuildVersionsArgumentsProvider::class)
    fun execute(buildVersions: BuildVersions) {
        val result = createGradleRunner(
            buildVersions,
            ":dokkaGenerate",
            "-Pdokka_it_failOnWarning=true"
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
        }

        // skipped deprecated declarations - https://github.com/Kotlin/dokka/issues/4448
        // protected declarations are not included, like in JsonTransformingSerializer.transformDeserialize
        // constructor parameters (--root--.html) - https://github.com/Kotlin/dokka/issues/4328
        // internal declarations in annotations - https://github.com/Kotlin/dokka/issues/4448
        assertHrefMissing(
            output = projectOutputLocation,
            expected = mapOf(
                "kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/index.html" to setOf(
                    "../../kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/--root--.html" to "kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/--root--.html",
                ),
                "kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/index.html" to setOf(
                    "../../../kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/--root--.html" to "kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/--root--.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/-json-array/index.html" to setOf(
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-array-serializer/index.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-array-serializer/index.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/index.html" to setOf(
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/select-deserializer.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/select-deserializer.html",
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/base-class.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/base-class.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/descriptor.html" to setOf(
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/base-class.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/base-class.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/index.html" to setOf(
                    "../../kotlinx-serialization-json/kotlinx.serialization.json/-json-array-serializer/index.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-array-serializer/index.html",
                    "../../kotlinx-serialization-json/kotlinx.serialization.json/-json-element-serializer/index.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-element-serializer/index.html",
                    "../../kotlinx-serialization-json/kotlinx.serialization.json/-json-null-serializer/index.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-null-serializer/index.html",
                    "../../kotlinx-serialization-json/kotlinx.serialization.json/-json-object-serializer/index.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-object-serializer/index.html",
                    "../../kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive-serializer/index.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive-serializer/index.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/-json-object/index.html" to setOf(
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-object-serializer/index.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-object-serializer/index.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/-json-decoder/index.html" to setOf(
                    "../../../kotlinx-serialization-core/kotlinx.serialization/-k-serializer/serialize.html" to "kotlinx-serialization-core/kotlinx.serialization/-k-serializer/serialize.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/-json-decoder/decode-json-element.html" to setOf(
                    "../../../kotlinx-serialization-core/kotlinx.serialization/-k-serializer/serialize.html" to "kotlinx-serialization-core/kotlinx.serialization/-k-serializer/serialize.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/-json-element/index.html" to setOf(
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-element-serializer/index.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-element-serializer/index.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/-json-null/index.html" to setOf(
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-null-serializer/index.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-null-serializer/index.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive/index.html" to setOf(
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive-serializer/index.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive-serializer/index.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/index.html" to setOf(
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/transform-serialize.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/transform-serialize.html",
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/transform-deserialize.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/transform-deserialize.html",
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/t-serializer.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/t-serializer.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/descriptor.html" to setOf(
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/t-serializer.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/t-serializer.html",
                ),
                "kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/-json-transforming-serializer.html" to setOf(
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/transform-serialize.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/transform-serialize.html",
                    "../../../kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/transform-deserialize.html" to "kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/transform-deserialize.html",
                ),
                "kotlinx-serialization-cbor/kotlinx.serialization.cbor/index.html" to setOf(
                    "../../kotlinx-serialization-core/kotlinx.serialization/-k-serializer/deserialize.html" to "kotlinx-serialization-core/kotlinx.serialization/-k-serializer/deserialize.html",
                    "../../kotlinx-serialization-core/kotlinx.serialization/-k-serializer/serialize.html" to "kotlinx-serialization-core/kotlinx.serialization/-k-serializer/serialize.html",
                ),
                "kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-encoder/index.html" to setOf(
                    "../../../kotlinx-serialization-core/kotlinx.serialization/-k-serializer/serialize.html" to "kotlinx-serialization-core/kotlinx.serialization/-k-serializer/serialize.html",
                ),
                "kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-decoder/index.html" to setOf(
                    "../../../kotlinx-serialization-core/kotlinx.serialization/-k-serializer/deserialize.html" to "kotlinx-serialization-core/kotlinx.serialization/-k-serializer/deserialize.html",
                ),
                "kotlinx-serialization-core/kotlinx.serialization.modules/index.html" to setOf(
                    "../../kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/base-class.html" to "kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/base-class.html",
                    "../../kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/base-serializer.html" to "kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/base-serializer.html",
                ),
                "kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/default-deserializer.html" to setOf(
                    "../../../kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/base-class.html" to "kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/base-class.html",
                ),
                "kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/index.html" to setOf(
                    "../../../kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/base-class.html" to "kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/base-class.html",
                    "../../../kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/base-serializer.html" to "kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/base-serializer.html",
                ),
                "kotlinx-serialization-core/kotlinx.serialization.modules/plus.html" to setOf(
                    "../../kotlinx-serialization-core/kotlinx.serialization.modules/-serializer-already-registered-exception/index.html" to "kotlinx-serialization-core/kotlinx.serialization.modules/-serializer-already-registered-exception/index.html",
                ),
                "kotlinx-serialization-core/kotlinx.serialization/-serialization-exception/index.html" to setOf(
                    "../../../kotlinx-serialization-core/kotlinx.serialization/-serialization-exception/--root--.html" to "kotlinx-serialization-core/kotlinx.serialization/-serialization-exception/--root--.html",
                ),
                "kotlinx-serialization-core/kotlinx.serialization/-serialization-exception/-serialization-exception.html" to setOf(
                    "../../../kotlinx-serialization-core/kotlinx.serialization/-serialization-exception/--root--.html" to "kotlinx-serialization-core/kotlinx.serialization/-serialization-exception/--root--.html",
                ),
                "kotlinx-serialization-core/kotlinx.serialization/-k-serializer/index.html" to setOf(
                    "../../../kotlinx-serialization-core/kotlinx.serialization/-k-serializer/serialize.html" to "kotlinx-serialization-core/kotlinx.serialization/-k-serializer/serialize.html",
                    "../../../kotlinx-serialization-core/kotlinx.serialization/-k-serializer/deserialize.html" to "kotlinx-serialization-core/kotlinx.serialization/-k-serializer/deserialize.html",
                ),
                "kotlinx-serialization-core/kotlinx.serialization/-missing-field-exception/index.html" to setOf(
                    "../../../kotlinx-serialization-core/kotlinx.serialization/-missing-field-exception/--root--.html" to "kotlinx-serialization-core/kotlinx.serialization/-missing-field-exception/--root--.html",
                ),
                "kotlinx-serialization-core/kotlinx.serialization/-missing-field-exception/-missing-field-exception.html" to setOf(
                    "../../../kotlinx-serialization-core/kotlinx.serialization/-missing-field-exception/--root--.html" to "kotlinx-serialization-core/kotlinx.serialization/-missing-field-exception/--root--.html",
                ),
            )
        )
    }
}
