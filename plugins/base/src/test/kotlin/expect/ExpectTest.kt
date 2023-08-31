/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package expect

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.Ignore

class ExpectTest : AbstractExpectTest() {
    private val ignores: List<String> = listOf(
        "images",
        "scripts",
        "images",
        "styles",
        "*.js",
        "*.css",
        "*.svg",
        "*.map"
    )
    
    @Ignore
    @TestFactory
    fun expectTest() = testDir?.dirsWithFormats(formats).orEmpty().map { (p, f) ->
        dynamicTest("${p.fileName}-$f") { testOutputWithExcludes(p, f, ignores) }
    }
}
