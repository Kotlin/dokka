package org.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import org.jetbrains.dokka.*

public class PackageTest {
    Test fun rootPackage() {
        verifyModel("test/data/packages/rootPackage.kt") { model ->
            with(model.members.single()) {
                assertEquals(DocumentationNodeKind.Package, kind)
                assertEquals("", name)
                assertEquals(DocumentationContent.Empty, doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun simpleNamePackage() {
        verifyModel("test/data/packages/simpleNamePackage.kt") { model ->
            with(model.members.single()) {
                assertEquals(DocumentationNodeKind.Package, kind)
                assertEquals("simple", name)
                assertEquals(DocumentationContent.Empty, doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun dottedNamePackage() {
        verifyModel("test/data/packages/dottedNamePackage.kt") { model ->
            with(model.members.single()) {
                assertEquals(DocumentationNodeKind.Package, kind)
                assertEquals("dot.name", name)
                assertEquals(DocumentationContent.Empty, doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }

    Test fun multipleFiles() {
        verifyModel("test/data/packages/dottedNamePackage.kt", "test/data/packages/simpleNamePackage.kt") { model ->
            assertEquals(2, model.members.count())
            with(model.members.elementAt(0)) {
                assertEquals(DocumentationNodeKind.Package, kind)
                assertEquals("simple", name)
                assertEquals(DocumentationContent.Empty, doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
            with(model.members.elementAt(1)) {
                assertEquals(DocumentationNodeKind.Package, kind)
                assertEquals("dot.name", name)
                assertEquals(DocumentationContent.Empty, doc)
                assertTrue(details.none())
                assertTrue(members.none())
                assertTrue(links.none())
            }
        }
    }
}