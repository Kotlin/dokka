package com.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import com.jetbrains.dokka.*

public class PackageTest {
    Test fun rootPackage() {
        verifyModel("test/data/packages/rootPackage.kt") { model ->
            with(model.nodes.single()) {
                assertEquals(DocumentationNodeKind.Package, kind)
                assertEquals("", name)
                assertEquals("", doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun simpleNamePackage() {
        verifyModel("test/data/packages/simpleNamePackage.kt") { model ->
            with(model.nodes.single()) {
                assertEquals(DocumentationNodeKind.Package, kind)
                assertEquals("simple", name)
                assertEquals("", doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun dottedNamePackage() {
        verifyModel("test/data/packages/dottedNamePackage.kt") { model ->
            with(model.nodes.single()) {
                assertEquals(DocumentationNodeKind.Package, kind)
                assertEquals("dot.name", name)
                assertEquals("", doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }
}