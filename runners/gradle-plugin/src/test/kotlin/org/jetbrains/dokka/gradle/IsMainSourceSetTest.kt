package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.gradle.kotlin.isMainSourceSet
import kotlin.test.Test
import kotlin.test.assertTrue

class IsMainSourceSetTest {
    @Test
    fun `missing compilations will return true`() {
        assertTrue(
            isMainSourceSet(emptyList()),
            "Expected 'isMainSourceSet' to return 'true' when no compilations are found"
        )
    }
}
