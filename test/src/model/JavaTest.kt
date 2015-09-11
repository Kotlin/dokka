package org.jetbrains.dokka.tests

import kotlin.test.*
import org.jetbrains.dokka.*
import org.junit.*

public class JavaTest {
    @Test fun function() {
        verifyPackageMember("test/data/java/member.java") { cls ->
            assertEquals("Test", cls.name)
            assertEquals(DocumentationNode.Kind.Class, cls.kind)
            with(cls.members.single()) {
                assertEquals("fn", name)
                assertEquals(DocumentationNode.Kind.Function, kind)
                assertEquals("Summary for Function", content.summary.toTestString().trimEnd())
                assertEquals(3, content.sections.size())
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
        verifyPackageMember("test/data/java/memberWithModifiers.java") { cls ->
            assertEquals("abstract", cls.details[0].name)
            with(cls.members.single { it.name == "fn" }) {
                assertEquals("protected", details[0].name)
            }
            with(cls.members.single { it.name == "openFn" }) {
                assertEquals("open", details[1].name)
            }
        }
    }

    @Test fun superClass() {
        verifyPackageMember("test/data/java/superClass.java") { cls ->
            val superTypes = cls.details(DocumentationNode.Kind.Supertype)
            assertEquals(2, superTypes.size())
            assertEquals("Exception", superTypes[0].name)
            assertEquals("Cloneable", superTypes[1].name)
        }
    }

    @Test fun arrayType() {
        verifyPackageMember("test/data/java/arrayType.java") { cls ->
            with(cls.members.single()) {
                val type = detail(DocumentationNode.Kind.Type)
                assertEquals("Array", type.name)
                assertEquals("String", type.detail(DocumentationNode.Kind.Type).name)
                with(details(DocumentationNode.Kind.Parameter).single()) {
                    val parameterType = detail(DocumentationNode.Kind.Type)
                    assertEquals("Array", parameterType.name)
                    assertEquals("Int", parameterType.detail(DocumentationNode.Kind.Type).name)
                }
            }
        }
    }

    @Test fun typeParameter() {
        verifyPackageMember("test/data/java/typeParameter.java") { cls ->
            val typeParameters = cls.details(DocumentationNode.Kind.TypeParameter)
            with(typeParameters.single()) {
                assertEquals("T", name)
                with(detail(DocumentationNode.Kind.UpperBound)) {
                    assertEquals("Comparable", name)
                    assertEquals("T", detail(DocumentationNode.Kind.Type).name)
                }
            }
            with(cls.members.single()) {
                val methodTypeParameters = details(DocumentationNode.Kind.TypeParameter)
                with(methodTypeParameters.single()) {
                    assertEquals("E", name)
                }
            }
        }
    }

    @Test fun constructors() {
        verifyPackageMember("test/data/java/constructors.java") { cls ->
            val constructors = cls.members(DocumentationNode.Kind.Constructor)
            assertEquals(2, constructors.size())
            with(constructors[0]) {
                assertEquals("<init>", name)
            }
        }
    }

    @Test fun innerClass() {
        verifyPackageMember("test/data/java/innerClass.java") { cls ->
            val innerClass = cls.members(DocumentationNode.Kind.Class).single()
            assertEquals("D", innerClass.name)
        }
    }

    @Test fun varargs() {
        verifyPackageMember("test/data/java/varargs.java") { cls ->
            val fn = cls.members(DocumentationNode.Kind.Function).single()
            val param = fn.detail(DocumentationNode.Kind.Parameter)
            assertEquals("vararg", param.annotations.first().name)
            val psiType = param.detail(DocumentationNode.Kind.Type)
            assertEquals("String", psiType.name)
            assertTrue(psiType.details(DocumentationNode.Kind.Type).isEmpty())
        }
    }

    @Test fun fields() {
        verifyPackageMember("test/data/java/field.java") { cls ->
            val i = cls.members(DocumentationNode.Kind.Property).single { it.name == "i" }
            assertEquals("Int", i.detail(DocumentationNode.Kind.Type).name)
            assertTrue("var" in i.details(DocumentationNode.Kind.Modifier).map { it.name })
            val s = cls.members(DocumentationNode.Kind.CompanionObjectProperty).single { it.name == "s" }
            assertEquals("String", s.detail(DocumentationNode.Kind.Type).name)
            assertFalse("var" in s.details(DocumentationNode.Kind.Modifier).map { it.name })
        }
    }

    @Test fun staticMethod() {
        verifyPackageMember("test/data/java/staticMethod.java") { cls ->
            val m = cls.members(DocumentationNode.Kind.CompanionObjectFunction).single { it.name == "foo" }
            assertFalse("static" in m.details(DocumentationNode.Kind.Modifier).map { it.name })
        }
    }

    @Test fun annotatedAnnotation() {
        verifyPackageMember("test/data/java/annotatedAnnotation.java") { cls ->
            assertEquals(2, cls.annotations.size())
            with(cls.annotations[0]) {
                assertEquals(1, details.count())
                with(details[0]) {
                    assertEquals(DocumentationNode.Kind.Parameter, kind)
                    assertEquals(1, details.count())
                    with(details[0]) {
                        assertEquals(DocumentationNode.Kind.Value, kind)
                        assertEquals("RetentionPolicy.RUNTIME", name)
                    }
                }
            }
        }
    }

    @Test fun deprecation() {
        verifyPackageMember("test/data/java/deprecation.java") { cls ->
            val fn = cls.members(DocumentationNode.Kind.Function).single()
            with(fn.deprecation!!) {
                assertEquals(1, details.count())
                with(details[0]) {
                    assertEquals(DocumentationNode.Kind.Parameter, kind)
                    assertEquals(1, details.count())
                    with(details[0]) {
                        assertEquals(DocumentationNode.Kind.Value, kind)
                        assertEquals("This should no longer be used", name)
                    }
                }
            }
        }
    }

    @Test fun javaLangObject() {
        verifyPackageMember("test/data/java/javaLangObject.java") { cls ->
            val fn = cls.members(DocumentationNode.Kind.Function).single()
            assertEquals("Any", fn.detail(DocumentationNode.Kind.Type).name)
        }
    }

    @Test fun enumValues() {
        verifyPackageMember("test/data/java/enumValues.java") { cls ->
            val superTypes = cls.details(DocumentationNode.Kind.Supertype)
            assertEquals(0, superTypes.size())
            assertEquals(1, cls.members(DocumentationNode.Kind.EnumItem).size())
        }
    }

    @Test fun inheritorLinks() {
        verifyPackageMember("test/data/java/inheritorLinks.java") { cls ->
            val fooClass = cls.members.single { it.name == "Foo" }
            val inheritors = fooClass.references(DocumentationReference.Kind.Inheritor)
            assertEquals(1, inheritors.size())
        }
    }
}
