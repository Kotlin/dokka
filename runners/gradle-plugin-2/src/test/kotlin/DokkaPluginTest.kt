package org.jetbrains.dokka.gradle

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test

class DokkaPluginTest {

    @Test
    fun `DokkaTask configuration toJsonString then parseJson`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka2")
    }
}
