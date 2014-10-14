package org.jetbrains.dokka.tests

import kotlin.test.*
import org.jetbrains.dokka.*
import org.junit.*

public class JavaTest {
    Ignore Test fun function() {
        verifyModel("test/data/java/") { model ->
            val pkg = model.members.single()
            with(pkg.members.single()) {
                assertEquals("fn", name)
                assertEquals(DocumentationNode.Kind.Function, kind)
                assertEquals("Function fn", content.summary)
                assertEquals("Unit", detail(DocumentationNode.Kind.Type).name)
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

}

