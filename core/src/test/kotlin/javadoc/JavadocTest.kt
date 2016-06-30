package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.DokkaConsoleLogger
import org.jetbrains.dokka.tests.verifyModel
import org.junit.Assert.*
import org.junit.Test

class JavadocTest {
    @Test fun testTypes() {
        verifyJavadoc("testdata/javadoc/types.kt", withJdk = true) { doc ->
            val classDoc = doc.classNamed("foo.TypesKt")!!
            val method = classDoc.methods().find { it.name() == "foo" }!!

            val type = method.returnType()
            assertFalse(type.asClassDoc().isIncluded)
            assertEquals("java.lang.String", type.qualifiedTypeName())
            assertEquals("java.lang.String", type.asClassDoc().qualifiedName())

            val params = method.parameters()
            assertTrue(params[0].type().isPrimitive)
            assertFalse(params[1].type().asClassDoc().isIncluded)
        }
    }

    @Test fun testObject() {
        verifyJavadoc("testdata/javadoc/obj.kt") { doc ->
            val classDoc = doc.classNamed("foo.O")
            assertNotNull(classDoc)

            val companionDoc = doc.classNamed("foo.O.Companion")
            assertNotNull(companionDoc)

            val pkgDoc = doc.packageNamed("foo")!!
            assertEquals(2, pkgDoc.allClasses().size)
        }
    }

    @Test fun testException() {
        verifyJavadoc("testdata/javadoc/exception.kt", withKotlinRuntime = true) { doc ->
            val classDoc = doc.classNamed("foo.MyException")!!
            val member = classDoc.methods().find { it.name() == "foo" }
            assertEquals(classDoc, member!!.containingClass())
        }
    }

    @Test fun testByteArray() {
        verifyJavadoc("testdata/javadoc/bytearr.kt", withKotlinRuntime = true) { doc ->
            val classDoc = doc.classNamed("foo.ByteArray")!!
            assertNotNull(classDoc.asClassDoc())

            val member = classDoc.methods().find { it.name() == "foo" }!!
            assertEquals("[]", member.returnType().dimension())
        }
    }

    @Test fun testStringArray() {
        verifyJavadoc("testdata/javadoc/stringarr.kt", withKotlinRuntime = true) { doc ->
            val classDoc = doc.classNamed("foo.Foo")!!
            assertNotNull(classDoc.asClassDoc())

            val member = classDoc.methods().find { it.name() == "main" }!!
            val paramType = member.parameters()[0].type()
            assertNull(paramType.asParameterizedType())
            assertEquals("String", paramType.typeName())
            assertEquals("String", paramType.asClassDoc().name())
        }
    }

    @Test fun testJvmName() {
        verifyJavadoc("testdata/javadoc/jvmName.kt", withKotlinRuntime = true) { doc ->
            val classDoc = doc.classNamed("foo.Apple")!!
            assertNotNull(classDoc.asClassDoc())

            val member = classDoc.methods().find { it.name() == "_tree" }
            assertNotNull(member)
        }
    }

    @Test fun testLinkWithParam() {
        verifyJavadoc("testdata/javadoc/paramlink.kt", withKotlinRuntime = true) { doc ->
            val classDoc = doc.classNamed("demo.Apple")!!
            assertNotNull(classDoc.asClassDoc())
            val tags = classDoc.inlineTags().filterIsInstance<SeeTagAdapter>()
            assertEquals(2, tags.size)
            val linkTag = tags[1] as SeeMethodTagAdapter
            assertEquals("cutIntoPieces", linkTag.method.name())
        }
    }

    private fun verifyJavadoc(name: String,
                              withJdk: Boolean = false,
                              withKotlinRuntime: Boolean = false,
                              callback: (ModuleNodeAdapter) -> Unit) {

        verifyModel(name, format = "javadoc", withJdk = withJdk, withKotlinRuntime = withKotlinRuntime) { model ->
            val doc = ModuleNodeAdapter(model, StandardReporter(DokkaConsoleLogger), "")
            callback(doc)
        }
    }
}
