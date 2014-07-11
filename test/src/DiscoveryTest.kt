package com.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*


public class FunctionTest {
    Test fun function() {
        verifyFiles("test/data/function.kt") { model ->
            val item = model.items.single()
            assertEquals("fn", item.name)
            assertEquals("doc", item.doc)
        }
    }
}