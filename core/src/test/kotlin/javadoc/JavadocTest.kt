package org.jetbrains.dokka.javadoc

import com.sun.javadoc.Tag
import com.sun.javadoc.Type
import org.jetbrains.dokka.DokkaConsoleLogger
import org.jetbrains.dokka.tests.assertEqualsIgnoringSeparators
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
        verifyJavadoc("testdata/javadoc/jvmname.kt", withKotlinRuntime = true) { doc ->
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

    @Test fun testInternalVisibility() {
        verifyJavadoc("testdata/javadoc/internal.kt", withKotlinRuntime = true, includeNonPublic = false) { doc ->
            val classDoc = doc.classNamed("foo.Person")!!
            val constructors = classDoc.constructors()
            assertEquals(1, constructors.size)
            assertEquals(1, constructors.single().parameters().size)
        }
    }

    @Test fun testSuppress() {
        verifyJavadoc("testdata/javadoc/suppress.kt", withKotlinRuntime = true) { doc ->
            assertNull(doc.classNamed("Some"))
            assertNull(doc.classNamed("SomeAgain"))
            assertNull(doc.classNamed("Interface"))
            val classSame = doc.classNamed("Same")!!
            assertTrue(classSame.fields().isEmpty())
            assertTrue(classSame.methods().isEmpty())
        }
    }

    @Test fun testTypeAliases() {
        verifyJavadoc("testdata/javadoc/typealiases.kt", withKotlinRuntime = true) { doc ->
            assertNull(doc.classNamed("B"))
            assertNull(doc.classNamed("D"))

            assertEquals("A", doc.classNamed("C")!!.superclass().name())
            val methodParamType = doc.classNamed("TypealiasesKt")!!.methods()
                    .find { it.name() == "some" }!!.parameters().first()
                    .type()
            assertEquals("kotlin.jvm.functions.Function1", methodParamType.qualifiedTypeName())
            assertEquals("? super A, C", methodParamType.asParameterizedType().typeArguments()
                    .map(Type::qualifiedTypeName).joinToString())
        }
    }

    @Test fun testKDocKeywordsOnMethod() {
        verifyJavadoc("testdata/javadoc/kdocKeywordsOnMethod.kt", withKotlinRuntime = true) { doc ->
            val method = doc.classNamed("KdocKeywordsOnMethodKt")!!.methods()[0]
            assertEquals("@return [ContentText(text=value of a)]", method.tags("return").first().text())
            assertEquals("@param a [ContentText(text=Some string)]", method.paramTags().first().text())
            assertEquals("@throws FireException [ContentText(text=in case of fire)]", method.throwsTags().first().text())
        }
    }

    @Test
    fun testBlankLineInsideCodeBlock() {
        verifyJavadoc("testdata/javadoc/blankLineInsideCodeBlock.kt", withKotlinRuntime = true) { doc ->
            val method = doc.classNamed("BlankLineInsideCodeBlockKt")!!.methods()[0]
            val text = method.inlineTags().joinToString(separator = "", transform = Tag::text)
            assertEqualsIgnoringSeparators("""
                <p><code><pre>
                This is a test
                    of Dokka's code blocks.
                Here is a blank line.

                The previous line was blank.
                </pre></code></p>
            """.trimIndent(), text)
        }
    }

    @Test
    fun testCompanionMethodReference() {
        verifyJavadoc("testdata/javadoc/companionMethodReference.kt") { doc ->
            val classDoc = doc.classNamed("foo.TestClass")!!
            val tag = classDoc.inlineTags().filterIsInstance<SeeMethodTagAdapter>().first()
            assertEquals("TestClass.Companion", tag.referencedClassName())
            assertEquals("test", tag.referencedMemberName())
        }
    }

    private fun verifyJavadoc(name: String,
                              withJdk: Boolean = false,
                              withKotlinRuntime: Boolean = false,
                              includeNonPublic: Boolean = true,
                              callback: (ModuleNodeAdapter) -> Unit) {

        verifyModel(name, format = "javadoc", withJdk = withJdk, withKotlinRuntime = withKotlinRuntime, includeNonPublic = includeNonPublic) { model ->
            val doc = ModuleNodeAdapter(model, StandardReporter(DokkaConsoleLogger), "")
            callback(doc)
        }
    }
}
