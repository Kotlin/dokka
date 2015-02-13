package org.jetbrains.dokka.tests

import kotlin.test.*
import org.jetbrains.dokka.*
import org.junit.*

public class JavaTest {
    Test fun function() {
        verifyModel("test/data/java/") { model ->
            val pkg = model.members.single()
            with(pkg.members.single()) {
                assertEquals("Test", name)
                assertEquals(DocumentationNode.Kind.Class, kind)
                with(members.single()) {
                    assertEquals("fn", name)
                    assertEquals(DocumentationNode.Kind.Function, kind)
                    assertEquals("Summary for Function", content.summary.toTestString())
                    assertEquals("Unit", detail(DocumentationNode.Kind.Type).name)
                    assertTrue(members.none())
                    assertTrue(links.none())
                    with(details.first { it.name == "name" }) {
                        assertEquals(DocumentationNode.Kind.Parameter, kind)
                        assertEquals("String", detail(DocumentationNode.Kind.Type).name)
                    }
                    with(details.first { it.name == "value" }) {
                        assertEquals(DocumentationNode.Kind.Parameter, kind)
                        assertEquals("Int", detail(DocumentationNode.Kind.Type).name)
                    }
                }
            }
        }
    }
}
