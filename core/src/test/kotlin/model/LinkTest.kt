package org.jetbrains.dokka.tests

import org.jetbrains.dokka.NodeKind
import org.junit.Test
import kotlin.test.assertEquals

class LinkTest {
    @Test fun linkToSelf() {
        verifyModel("testdata/links/linkToSelf.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to [Foo -> Class:Foo]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToMember() {
        verifyModel("testdata/links/linkToMember.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to [member -> Function:member]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToConstantWithUnderscores() {
        verifyModel("testdata/links/linkToConstantWithUnderscores.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to [MY_CONSTANT_VALUE -> CompanionObjectProperty:MY_CONSTANT_VALUE]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToQualifiedMember() {
        verifyModel("testdata/links/linkToQualifiedMember.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Class, kind)
                assertEquals("This is link to [Foo.member -> Function:member]", content.summary.toTestString())
            }
        }
    }

    @Test fun linkToParam() {
        verifyModel("testdata/links/linkToParam.kt") { model ->
            with(model.members.single().members.single()) {
                assertEquals("Foo", name)
                assertEquals(NodeKind.Function, kind)
                assertEquals("This is link to [param -> Parameter:param]", content.summary.toTestString())
            }
        }
    }

}