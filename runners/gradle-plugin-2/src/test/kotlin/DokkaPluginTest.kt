package org.jetbrains.dokka.gradle

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test

class DokkaPluginTest {

    @Test
    fun `expect plugin can be applied to project successfully`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.dokka2")
    }
}
