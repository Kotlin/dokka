package org.jetbrains.dokka.tests

import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.DocumentationReference
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

public class JavaTest {
    @Test fun function() {
        verifyJavaPackageMember("testdata/java/member.java") { cls ->
            assertEquals("Test", cls.name)
            assertEquals(DocumentationNode.Kind.Class, cls.kind)
            with(cls.members(DocumentationNode.Kind.Function).single()) {
                assertEquals("fn", name)
                assertEquals("Summary for Function", content.summary.toTestString().trimEnd())
                assertEquals(3, content.sections.size)
                with(content.sections[0]) {
                    assertEquals("Parameters", tag)
                    assertEquals("name", subjectName)
                    assertEquals("is String parameter ", toTestString())
                }
                with(content.sections[1]) {
                    assertEquals("Parameters", tag)
                    assertEquals("value", subjectName)
                    assertEquals("is int parameter ", toTestString())
                }
                with(content.sections[2]) {
                    assertEquals("Author", tag)
                    assertEquals("yole", toTestString())
                }
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

    @Test fun memberWithModifiers() {
        verifyJavaPackageMember("testdata/java/memberWithModifiers.java") { cls ->
            val modifiers = cls.details(DocumentationNode.Kind.Modifier).map { it.name }
            assertTrue("abstract" in modifiers)
            with(cls.members.single { it.name == "fn" }) {
                assertEquals("protected", details[0].name)
            }
            with(cls.members.single { it.name == "openFn" }) {
                assertEquals("open", details[1].name)
            }
        }
    }

    @Test fun superClass() {
        verifyJavaPackageMember("testdata/java/superClass.java") { cls ->
            val superTypes = cls.details(DocumentationNode.Kind.Supertype)
            assertEquals(2, superTypes.size)
            assertEquals("Exception", superTypes[0].name)
            assertEquals("Cloneable", superTypes[1].name)
        }
    }

    @Test fun arrayType() {
        verifyJavaPackageMember("testdata/java/arrayType.java") { cls ->
            with(cls.members(DocumentationNode.Kind.Function).single()) {
                val type = detail(DocumentationNode.Kind.Type)
                assertEquals("Array", type.name)
                assertEquals("String", type.detail(DocumentationNode.Kind.Type).name)
                with(details(DocumentationNode.Kind.Parameter).single()) {
                    val parameterType = detail(DocumentationNode.Kind.Type)
                    assertEquals("IntArray", parameterType.name)
                }
            }
        }
    }

    @Test fun typeParameter() {
        verifyJavaPackageMember("testdata/java/typeParameter.java") { cls ->
            val typeParameters = cls.details(DocumentationNode.Kind.TypeParameter)
            with(typeParameters.single()) {
                assertEquals("T", name)
                with(detail(DocumentationNode.Kind.UpperBound)) {
                    assertEquals("Comparable", name)
                    assertEquals("T", detail(DocumentationNode.Kind.Type).name)
                }
            }
            with(cls.members(DocumentationNode.Kind.Function).single()) {
                val methodTypeParameters = details(DocumentationNode.Kind.TypeParameter)
                with(methodTypeParameters.single()) {
                    assertEquals("E", name)
                }
            }
        }
    }

    @Test fun constructors() {
        verifyJavaPackageMember("testdata/java/constructors.java") { cls ->
            val constructors = cls.members(DocumentationNode.Kind.Constructor)
            assertEquals(2, constructors.size)
            with(constructors[0]) {
                assertEquals("<init>", name)
            }
        }
    }

    @Test fun innerClass() {
        verifyJavaPackageMember("testdata/java/innerClass.java") { cls ->
            val innerClass = cls.members(DocumentationNode.Kind.Class).single()
            assertEquals("D", innerClass.name)
        }
    }

    @Test fun varargs() {
        verifyJavaPackageMember("testdata/java/varargs.java") { cls ->
            val fn = cls.members(DocumentationNode.Kind.Function).single()
            val param = fn.detail(DocumentationNode.Kind.Parameter)
            assertEquals("vararg", param.details(DocumentationNode.Kind.Modifier).first().name)
            val psiType = param.detail(DocumentationNode.Kind.Type)
            assertEquals("String", psiType.name)
            assertTrue(psiType.details(DocumentationNode.Kind.Type).isEmpty())
        }
    }

    @Test fun fields() {
        verifyJavaPackageMember("testdata/java/field.java") { cls ->
            val i = cls.members(DocumentationNode.Kind.Property).single { it.name == "i" }
            assertEquals("Int", i.detail(DocumentationNode.Kind.Type).name)
            assertTrue("var" in i.details(DocumentationNode.Kind.Modifier).map { it.name })

            val s = cls.members(DocumentationNode.Kind.Property).single { it.name == "s" }
            assertEquals("String", s.detail(DocumentationNode.Kind.Type).name)
            assertFalse("var" in s.details(DocumentationNode.Kind.Modifier).map { it.name })
            assertTrue("static" in s.details(DocumentationNode.Kind.Modifier).map { it.name })
        }
    }

    @Test fun staticMethod() {
        verifyJavaPackageMember("testdata/java/staticMethod.java") { cls ->
            val m = cls.members(DocumentationNode.Kind.Function).single { it.name == "foo" }
            assertTrue("static" in m.details(DocumentationNode.Kind.Modifier).map { it.name })
        }
    }

    @Test fun annotatedAnnotation() {
        verifyJavaPackageMember("testdata/java/annotatedAnnotation.java") { cls ->
            assertEquals(1, cls.annotations.size)
            with(cls.annotations[0]) {
                assertEquals(1, details.count())
                with(details[0]) {
                    assertEquals(DocumentationNode.Kind.Parameter, kind)
                    assertEquals(1, details.count())
                    with(details[0]) {
                        assertEquals(DocumentationNode.Kind.Value, kind)
                        assertEquals("[AnnotationTarget.FIELD, AnnotationTarget.CLASS, AnnotationTarget.FILE, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER]", name)
                    }
                }
            }
        }
    }

    @Test fun deprecation() {
        verifyJavaPackageMember("testdata/java/deprecation.java") { cls ->
            val fn = cls.members(DocumentationNode.Kind.Function).single()
            assertEquals("This should no longer be used", fn.deprecation!!.content.toTestString())
        }
    }

    @Test fun javaLangObject() {
        verifyJavaPackageMember("testdata/java/javaLangObject.java") { cls ->
            val fn = cls.members(DocumentationNode.Kind.Function).single()
            assertEquals("Any", fn.detail(DocumentationNode.Kind.Type).name)
        }
    }

    @Test fun enumValues() {
        verifyJavaPackageMember("testdata/java/enumValues.java") { cls ->
            val superTypes = cls.details(DocumentationNode.Kind.Supertype)
            assertEquals(0, superTypes.size)
            assertEquals(1, cls.members(DocumentationNode.Kind.EnumItem).size)
        }
    }

    @Test fun inheritorLinks() {
        verifyJavaPackageMember("testdata/java/inheritorLinks.java") { cls ->
            val fooClass = cls.members.single { it.name == "Foo" }
            val inheritors = fooClass.references(DocumentationReference.Kind.Inheritor)
            assertEquals(1, inheritors.size)
        }
    }
}
