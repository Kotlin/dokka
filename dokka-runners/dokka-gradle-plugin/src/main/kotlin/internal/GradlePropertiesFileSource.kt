/*
 * Copyright 2014-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.internal

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.util.*

/**
 * Read `gradle.properties` files from the project directory.
 *
 * Workaround for [org.gradle.testfixtures.ProjectBuilder] not reading `gradle.properties` files.
 */
// TODO remove when updating Gradle to 9.4+
//      https://github.com/gradle/gradle/issues/17638#issuecomment-4030001290
internal abstract class GradlePropertiesFileSource :
    ValueSource<Map<String, String?>, GradlePropertiesFileSource.Params> {

    interface Params : ValueSourceParameters {
        val projectDirectory: RegularFileProperty
    }

    override fun obtain(): Map<String, String?> {
        val gpFile = parameters.projectDirectory.get().asFile
            .resolve("gradle.properties")
            .takeIf { it.exists() }
            ?: return emptyMap()

        val props = Properties()

        gpFile.inputStream().use { reader ->
            props.load(reader)
        }

        return props.entries.associate { it.key.toString() to it.value?.toString() }
    }
}
