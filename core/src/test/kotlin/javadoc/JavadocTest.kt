package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.DokkaConsoleLogger
import org.jetbrains.dokka.tests.verifyModel
import org.junit.Assert.*
import org.junit.Test

class JavadocTest {
    @Test fun testTypes() {
        verifyModel("testdata/javadoc/types.kt", format = "javadoc", withJdk = true) { model ->
            val doc = ModuleNodeAdapter(model, StandardReporter(DokkaConsoleLogger), "")
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
        verifyModel("testdata/javadoc/obj.kt", format = "javadoc") { model ->
            val doc = ModuleNodeAdapter(model, StandardReporter(DokkaConsoleLogger), "")

            val classDoc = doc.classNamed("foo.O")
            assertNotNull(classDoc)

            val companionDoc = doc.classNamed("foo.O.Companion")
            assertNotNull(companionDoc)

            val pkgDoc = doc.packageNamed("foo")!!
            assertEquals(2, pkgDoc.allClasses().size)
        }
    }

    @Test fun testException() {
        verifyModel("testdata/javadoc/exception.kt", format = "javadoc", withKotlinRuntime = true) { model ->
            val doc = ModuleNodeAdapter(model, StandardReporter(DokkaConsoleLogger), "")

            val classDoc = doc.classNamed("foo.MyException")!!
            val member = classDoc.methods().find { it.name() == "foo" }
            assertEquals(classDoc, member!!.containingClass())
        }
    }

    @Test fun testByteArray() {
        verifyModel("testdata/javadoc/bytearr.kt", format = "javadoc", withKotlinRuntime = true) { model ->
            val doc = ModuleNodeAdapter(model, StandardReporter(DokkaConsoleLogger), "")

            val classDoc = doc.classNamed("foo.ByteArray")!!
            assertNotNull(classDoc.asClassDoc())

            val member = classDoc.methods().find { it.name() == "foo" }!!
            assertEquals("[]", member.returnType().dimension())
        }
    }

    @Test fun testStringArray() {
        verifyModel("testdata/javadoc/stringarr.kt", format = "javadoc", withKotlinRuntime = true) { model ->
            val doc = ModuleNodeAdapter(model, StandardReporter(DokkaConsoleLogger), "")

            val classDoc = doc.classNamed("foo.Foo")!!
            assertNotNull(classDoc.asClassDoc())

            val member = classDoc.methods().find { it.name() == "main" }!!
            val paramType = member.parameters()[0].type()
            assertNull(paramType.asParameterizedType())
            assertEquals("String", paramType.typeName())
            assertEquals("String", paramType.asClassDoc().name())
        }
    }
}
