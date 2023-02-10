package org.jetbrains.dokka.gradle.kotlin

import kotlin.test.*

class KotlinGradlePluginVersionTest {

    @Test
    fun `should parse versions`() {
        assertParsedVersion(input = "1.7.22", expectedMajor = 1, expectedMinor = 7, expectedPatch = 22)
        assertParsedVersion(input = "2.0.0", expectedMajor = 2, expectedMinor = 0, expectedPatch = 0)
        assertParsedVersion(input = "1.8.22-RC2", expectedMajor = 1, expectedMinor = 8, expectedPatch = 22)
        assertParsedVersion(input = "1.5.22-Beta", expectedMajor = 1, expectedMinor = 5, expectedPatch = 22)
        assertParsedVersion(input = "2.7.22-mercury-500", expectedMajor = 2, expectedMinor = 7, expectedPatch = 22)
        assertParsedVersion(input = "1.7.100-dev-800", expectedMajor = 1, expectedMinor = 7, expectedPatch = 100)
    }

    @Test
    fun `should return null on non parsable string`() {
        assertNull(parse("1.7"))
        assertNull(parse("1"))
        assertNull(parse("6"))
        assertNull(parse("17.0"))
        assertNull(parse("1..7.0"))
        assertNull(parse("1.7-Beta"))
        assertNull(parse("1.7.0Beta"))
    }

    @Test
    fun `should compare simple versions`() {
        assertEquals(0, KotlinGradlePluginVersion(1, 7, 0).compareTo(KotlinGradlePluginVersion(1, 7, 0)))

        assertTrue(KotlinGradlePluginVersion(1, 6, 10) >= KotlinGradlePluginVersion(1, 6, 10))
        assertTrue(KotlinGradlePluginVersion(1, 6, 10) < KotlinGradlePluginVersion(1, 6, 20))
        assertTrue(KotlinGradlePluginVersion(1, 6, 10) > KotlinGradlePluginVersion(1, 6, 0))

        assertTrue(KotlinGradlePluginVersion(1, 4, 32) <= KotlinGradlePluginVersion(1, 4, 32))
        assertTrue(KotlinGradlePluginVersion(1, 4, 32) < KotlinGradlePluginVersion(1, 6, 20))
        assertTrue(KotlinGradlePluginVersion(1, 4, 32) > KotlinGradlePluginVersion(1, 3, 0))

        assertTrue(KotlinGradlePluginVersion(2, 1, 0) > KotlinGradlePluginVersion(1, 8, 0))
        assertTrue(KotlinGradlePluginVersion(1, 5, 31) < KotlinGradlePluginVersion(1, 7, 0))
    }

    @Test
    fun `should compare custom dev versions with trailing strings`() {
        assertEquals(0, KotlinGradlePluginVersion(1, 7, 0).compareTo(parseNotNull("1.7.0")))

        assertTrue(KotlinGradlePluginVersion(1, 6, 10) >= parseNotNull("1.6.10-Beta"))
        assertTrue(KotlinGradlePluginVersion(1, 6, 10) < parseNotNull("1.6.20"))
        assertTrue(KotlinGradlePluginVersion(1, 6, 10) > parseNotNull("1.6.0-RC2"))

        assertTrue(KotlinGradlePluginVersion(1, 4, 32) <= parseNotNull("1.4.32-dev-142"))
        assertTrue(KotlinGradlePluginVersion(1, 4, 32) < parseNotNull("1.6.20-Beta"))
        assertTrue(KotlinGradlePluginVersion(1, 4, 32) > parseNotNull("1.3.0-RC"))

        assertTrue(KotlinGradlePluginVersion(2, 1, 0) > parseNotNull("1.8.0-mercury-500"))
    }

    private fun assertParsedVersion(
        input: String,
        expectedMajor: Int,
        expectedMinor: Int,
        expectedPatch: Int
    ) {
        val kgpVersion = parseNotNull(input)
        assertEquals(expectedMajor, kgpVersion.major)
        assertEquals(expectedMinor, kgpVersion.minor)
        assertEquals(expectedPatch, kgpVersion.patch)
        assertEquals(KotlinGradlePluginVersion(expectedMajor, expectedMinor, expectedPatch), kgpVersion)
    }

    private fun parseNotNull(input: String): KotlinGradlePluginVersion = assertNotNull(parse(input))

    private fun parse(input: String): KotlinGradlePluginVersion? = parseKotlinVersion(input)
}
