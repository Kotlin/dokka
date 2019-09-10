package org.jetbrains.dokka.javadoc

import com.sun.javadoc.Tag
import com.sun.javadoc.Type
import org.jetbrains.dokka.DokkaConsoleLogger
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.tests.ModelConfig
import org.jetbrains.dokka.tests.assertEqualsIgnoringSeparators
import org.jetbrains.dokka.tests.checkSourceExistsAndVerifyModel
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Modifier.*

class JavadocTest {
    val defaultModelConfig = ModelConfig(analysisPlatform = Platform.jvm)

    @Test fun testTypes() {
        verifyJavadoc("testdata/javadoc/types.kt", ModelConfig(analysisPlatform = Platform.jvm, withJdk = true)) { doc ->
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
        verifyJavadoc("testdata/javadoc/obj.kt", defaultModelConfig) { doc ->
            val classDoc = doc.classNamed("foo.O")
            assertNotNull(classDoc)

            val companionDoc = doc.classNamed("foo.O.Companion")
            assertNotNull(companionDoc)

            val pkgDoc = doc.packageNamed("foo")!!
            assertEquals(2, pkgDoc.allClasses().size)
        }
    }

    @Test fun testException() {
        verifyJavadoc(
            "testdata/javadoc/exception.kt",
            ModelConfig(analysisPlatform = Platform.jvm, withKotlinRuntime = true)
        ) { doc ->
            val classDoc = doc.classNamed("foo.MyException")!!
            val member = classDoc.methods().find { it.name() == "foo" }
            assertEquals(classDoc, member!!.containingClass())
        }
    }

    @Test fun testByteArray() {
        verifyJavadoc(
            "testdata/javadoc/bytearr.kt",
            ModelConfig(analysisPlatform = Platform.jvm, withKotlinRuntime = true)
        ) { doc ->
            val classDoc = doc.classNamed("foo.ByteArray")!!
            assertNotNull(classDoc.asClassDoc())

            val member = classDoc.methods().find { it.name() == "foo" }!!
            assertEquals("[]", member.returnType().dimension())
        }
    }

    @Test fun testStringArray() {
        verifyJavadoc(
            "testdata/javadoc/stringarr.kt",
            ModelConfig(analysisPlatform = Platform.jvm, withKotlinRuntime = true)
        ) { doc ->
            val classDoc = doc.classNamed("foo.Foo")!!
            assertNotNull(classDoc.asClassDoc())

            val member = classDoc.methods().find { it.name() == "main" }!!
            val paramType = member.parameters()[0].type()
            assertNull(paramType.asParameterizedType())
            assertEquals("String[]", paramType.typeName())
            assertEquals("String", paramType.asClassDoc().name())
        }
    }

    @Test fun testJvmName() {
        verifyJavadoc(
            "testdata/javadoc/jvmname.kt",
            ModelConfig(analysisPlatform = Platform.jvm, withKotlinRuntime = true)
        ) { doc ->
            val classDoc = doc.classNamed("foo.Apple")!!
            assertNotNull(classDoc.asClassDoc())

            val member = classDoc.methods().find { it.name() == "_tree" }
            assertNotNull(member)
        }
    }

    @Test fun testLinkWithParam() {
        verifyJavadoc(
            "testdata/javadoc/paramlink.kt",
            ModelConfig(analysisPlatform = Platform.jvm, withKotlinRuntime = true)
        ) { doc ->
            val classDoc = doc.classNamed("demo.Apple")!!
            assertNotNull(classDoc.asClassDoc())
            val tags = classDoc.inlineTags().filterIsInstance<SeeTagAdapter>()
            assertEquals(2, tags.size)
            val linkTag = tags[1] as SeeMethodTagAdapter
            assertEquals("cutIntoPieces", linkTag.method.name())
        }
    }

    @Test fun testInternalVisibility() {
        verifyJavadoc(
            "testdata/javadoc/internal.kt",
            ModelConfig(analysisPlatform = Platform.jvm, withKotlinRuntime = true, includeNonPublic = false)
        ) { doc ->
            val classDoc = doc.classNamed("foo.Person")!!
            val constructors = classDoc.constructors()
            assertEquals(1, constructors.size)
            assertEquals(1, constructors.single().parameters().size)
        }
    }

    @Test fun testSuppress() {
        verifyJavadoc(
            "testdata/javadoc/suppress.kt",
            ModelConfig(analysisPlatform = Platform.jvm, withKotlinRuntime = true)
        ) { doc ->
            assertNull(doc.classNamed("Some"))
            assertNull(doc.classNamed("SomeAgain"))
            assertNull(doc.classNamed("Interface"))
            val classSame = doc.classNamed("Same")!!
            assertTrue(classSame.fields().isEmpty())
            assertTrue(classSame.methods().isEmpty())
        }
    }

    @Test fun testTypeAliases() {
        verifyJavadoc(
            "testdata/javadoc/typealiases.kt",
            ModelConfig(analysisPlatform = Platform.jvm, withKotlinRuntime = true)
        ) { doc ->
            assertNull(doc.classNamed("B"))
            assertNull(doc.classNamed("D"))

            assertEquals("A", doc.classNamed("C")!!.superclass().name())
            val methodParamType = doc.classNamed("TypealiasesKt")!!.methods()
                    .find { it.name() == "some" }!!.parameters().first()
                    .type()
            assertEquals("kotlin.jvm.functions.Function1", methodParamType.qualifiedTypeName())
            assertEquals("? super A, C",
                methodParamType.asParameterizedType().typeArguments().joinToString(transform = Type::qualifiedTypeName)
            )
        }
    }

    @Test fun testKDocKeywordsOnMethod() {
        verifyJavadoc(
            "testdata/javadoc/kdocKeywordsOnMethod.kt",
            ModelConfig(analysisPlatform = Platform.jvm, withKotlinRuntime = true)
        ) { doc ->
            val method = doc.classNamed("KdocKeywordsOnMethodKt")!!.methods()[0]
            assertEquals("@return [ContentText(text=value of a)]", method.tags("return").first().text())
            assertEquals("@param a [ContentText(text=Some string)]", method.paramTags().first().text())
            assertEquals("@throws FireException [ContentText(text=in case of fire)]", method.throwsTags().first().text())
        }
    }

    @Test
    fun testBlankLineInsideCodeBlock() {
        verifyJavadoc(
            "testdata/javadoc/blankLineInsideCodeBlock.kt",
            ModelConfig(analysisPlatform = Platform.jvm, withKotlinRuntime = true)
        ) { doc ->
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
        verifyJavadoc("testdata/javadoc/companionMethodReference.kt", defaultModelConfig) { doc ->
            val classDoc = doc.classNamed("foo.TestClass")!!
            val tag = classDoc.inlineTags().filterIsInstance<SeeMethodTagAdapter>().first()
            assertEquals("TestClass.Companion", tag.referencedClassName())
            assertEquals("test", tag.referencedMemberName())
        }
    }

    @Test
    fun testVararg() {
        verifyJavadoc("testdata/javadoc/vararg.kt") { doc ->
            val classDoc = doc.classNamed("VarargKt")!!
            val methods = classDoc.methods()
            methods.single { it.name() == "vararg" }.let {  method ->
                assertTrue(method.isVarArgs)
                assertEquals("int", method.parameters().last().typeName())
            }
            methods.single { it.name() == "varargInMiddle" }.let {  method ->
                assertFalse(method.isVarArgs)
                assertEquals("int[]", method.parameters()[1].typeName())
            }
        }
    }

    @Test
    fun shouldHaveValidVisibilityModifiers() {
        verifyJavadoc("testdata/javadoc/visibilityModifiers.kt", ModelConfig(analysisPlatform = Platform.jvm, withKotlinRuntime = true)) { doc ->
            val classDoc = doc.classNamed("foo.Apple")!!
            val methods = classDoc.methods()

            val getName = methods[0]
            val setName = methods[1]
            val getWeight = methods[2]
            val setWeight = methods[3]
            val getRating = methods[4]
            val setRating = methods[5]
            val getCode = methods[6]
            val color = classDoc.fields()[3]
            val code = classDoc.fields()[4]

            assertTrue(getName.isProtected)
            assertEquals(PROTECTED, getName.modifierSpecifier())
            assertTrue(setName.isProtected)
            assertEquals(PROTECTED, setName.modifierSpecifier())

            assertTrue(getWeight.isPublic)
            assertEquals(PUBLIC, getWeight.modifierSpecifier())
            assertTrue(setWeight.isPublic)
            assertEquals(PUBLIC, setWeight.modifierSpecifier())

            assertTrue(getRating.isPublic)
            assertEquals(PUBLIC, getRating.modifierSpecifier())
            assertTrue(setRating.isPublic)
            assertEquals(PUBLIC, setRating.modifierSpecifier())

            assertTrue(getCode.isPublic)
            assertEquals(PUBLIC or STATIC, getCode.modifierSpecifier())

            assertEquals(methods.size, 7)

            assertTrue(color.isPrivate)
            assertEquals(PRIVATE, color.modifierSpecifier())

            assertTrue(code.isPrivate)
            assertTrue(code.isStatic)
            assertEquals(PRIVATE or STATIC, code.modifierSpecifier())
        }
    }

    @Test
    fun shouldNotHaveDuplicatedConstructorParameters() {
        verifyJavadoc("testdata/javadoc/constructorParameters.kt") { doc ->
            val classDoc = doc.classNamed("bar.Banana")!!
            val paramTags = classDoc.constructors()[0].paramTags()

            assertEquals(3, paramTags.size)
        }
    }

    @Test fun shouldHaveAllFunctionMarkedAsDeprecated() {
        verifyJavadoc("testdata/javadoc/deprecated.java") { doc ->
            val classDoc = doc.classNamed("bar.Banana")!!

            classDoc.methods().forEach { method ->
                assertTrue(method.tags().any { it.kind() == "deprecated" })
            }
        }
    }

    @Test
    fun testDefaultNoArgConstructor() {
        verifyJavadoc("testdata/javadoc/defaultNoArgConstructor.kt") { doc ->
            val classDoc = doc.classNamed("foo.Peach")!!
            assertTrue(classDoc.constructors()[0].tags()[2].text() == "print peach")
        }
    }

    @Test
    fun testNoArgConstructor() {
        verifyJavadoc("testdata/javadoc/noArgConstructor.kt") { doc ->
            val classDoc = doc.classNamed("foo.Plum")!!
            assertTrue(classDoc.constructors()[0].tags()[2].text() == "print plum")
        }
    }

    @Test
    fun testArgumentReference() {
        verifyJavadoc("testdata/javadoc/argumentReference.kt") { doc ->
            val classDoc = doc.classNamed("ArgumentReferenceKt")!!
            val method = classDoc.methods().first()
            val tag = method.seeTags().first()
            assertEquals("argNamedError", tag.referencedMemberName())
            assertEquals("error", tag.label())
        }
    }

    @Test
    fun functionParameters() {
        verifyJavadoc("testdata/javadoc/functionParameters.java") { doc ->
            val tags = doc.classNamed("bar.Foo")!!.methods().first().paramTags()
            assertEquals((tags.first() as ParamTagAdapter).content.size, 1)
            assertEquals((tags[1] as ParamTagAdapter).content.size, 1)
        }
    }

    private fun verifyJavadoc(name: String,
                              modelConfig: ModelConfig = ModelConfig(),
                              callback: (ModuleNodeAdapter) -> Unit) {

        checkSourceExistsAndVerifyModel(name,
            ModelConfig(
                analysisPlatform = Platform.jvm,
                format = "javadoc",
                withJdk = modelConfig.withJdk,
                withKotlinRuntime = modelConfig.withKotlinRuntime,
                includeNonPublic = modelConfig.includeNonPublic
            )) { model ->
            val doc = ModuleNodeAdapter(model, StandardReporter(DokkaConsoleLogger), "")
            callback(doc)
        }
    }
}
