package com.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import com.jetbrains.dokka.*


public class TopLevelFunctionTest {
    Test fun function() {
        verifyFiles("test/data/function.kt") { model ->
            val item = model.nodes.single()
            assertEquals(DocumentationNodeKind.Function, item.kind)
            assertEquals("fn", item.name)
            assertEquals("doc", item.doc)
            assertTrue(item.details.none())
            assertTrue(item.members.none())
            assertTrue(item.links.none())
        }
    }
}