/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it

import java.net.URL
import kotlin.test.Test

class StdLibDocumentationIntegrationTest {

    /**
     * Documentation for Enum's synthetic values() and valueOf() functions is only present in source code,
     * but not present in the descriptors. However, Dokka needs to generate documentation for these functions,
     * so it ships with hardcoded kdoc templates.
     *
     * This test exists to make sure documentation for these hardcoded synthetic functions does not change,
     * and fails if it does, indicating that it needs to be updated.
     */
    @Test
    fun shouldAssertEnumDocumentationHasNotChanged() {
        val sourcesLink = "https://raw.githubusercontent.com/JetBrains/kotlin/master/core/builtins/native/kotlin/Enum.kt"
        val sources = URL(sourcesLink).readText()

        val expectedValuesDoc =
            "    /**\n" +
            "     * Returns an array containing the constants of this enum type, in the order they're declared.\n" +
            "     * This method may be used to iterate over the constants.\n" +
            "     * @values\n" +
            "     */"
        check(sources.contains(expectedValuesDoc))

        val expectedValueOfDoc =
            "    /**\n" +
            "     * Returns the enum constant of this type with the specified name. The string must match exactly " +
            "an identifier used to declare an enum constant in this type. (Extraneous whitespace characters are not permitted.)\n" +
            "     * @throws IllegalArgumentException if this enum type has no constant with the specified name\n" +
            "     * @valueOf\n" +
            "     */"
        check(sources.contains(expectedValueOfDoc))
    }
}
