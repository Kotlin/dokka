package org.jetbrains.dokka.tests

import org.junit.Test
import kotlin.test.*
import org.jetbrains.dokka.*

public class LinkTest {
    @Test fun linkToSelf() {
        verifyModel("testdata/links/linkToSelf.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(DocumentationNode.Kind.Class, kind)
                assertEquals("This is link to [Foo -> Class:Foo]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToMember() {
        verifyModel("testdata/links/linkToMember.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(DocumentationNode.Kind.Class, kind)
                assertEquals("This is link to [member -> Function:member]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToQualifiedMember() {
        verifyModel("testdata/links/linkToQualifiedMember.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(DocumentationNode.Kind.Class, kind)
                assertEquals("This is link to [Foo.member -> Function:member]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToParam() {
        verifyModel("testdata/links/linkToParam.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(DocumentationNode.Kind.Function, kind)
                assertEquals("This is link to [param -> Parameter:param]", content.summary.toTestString())
            }
        }
    }

}