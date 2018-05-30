package org.jetbrains.dokka.tests

import org.jetbrains.dokka.*
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import java.util.function.Consumer
import java.util.stream.Collectors

public class JavaTest {
    @Test fun function() {
        verifyJavaPackageMember("testdata/java/member.java") { cls ->
            assertEquals("Test", cls.name)
            assertEquals(NodeKind.Class, cls.kind)
            with(cls.members(NodeKind.Function).single()) {
                assertEquals("fn", name)
                assertEquals("Summary for Function", content.summary.toTestString().trimEnd())
                assertEquals(3, content.sections.size)
                with(content.sections[0]) {
                    assertEquals("Parameters", tag)
                    assertEquals("name", subjectName)
                    assertEquals("is String parameter", toTestString())
                }
                with(content.sections[1]) {
                    assertEquals("Parameters", tag)
                    assertEquals("value", subjectName)
                    assertEquals("is int parameter", toTestString())
                }
                with(content.sections[2]) {
                    assertEquals("Author", tag)
                    assertEquals("yole", toTestString())
                }
                assertEquals("Unit", detail(NodeKind.Type).name)
                assertTrue(members.none())
                assertTrue(links.none())
                with(details.first { it.name == "name" }) {
                    assertEquals(NodeKind.Parameter, kind)
                    assertEquals("String", detail(NodeKind.Type).name)
                }
                with(details.first { it.name == "value" }) {
                    assertEquals(NodeKind.Parameter, kind)
                    assertEquals("Int", detail(NodeKind.Type).name)
                }
            }
        }
    }

    @Test fun memberWithModifiers() {
        verifyJavaPackageMember("testdata/java/memberWithModifiers.java") { cls ->
            val modifiers = cls.details(NodeKind.Modifier).map { it.name }
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
            val superTypes = cls.details(NodeKind.Supertype)
            assertEquals(2, superTypes.size)
            assertEquals("Exception", superTypes[0].name)
            assertEquals("Cloneable", superTypes[1].name)
        }
    }

    @Test fun arrayType() {
        verifyJavaPackageMember("testdata/java/arrayType.java") { cls ->
            with(cls.members(NodeKind.Function).single()) {
                val type = detail(NodeKind.Type)
                assertEquals("Array", type.name)
                assertEquals("String", type.detail(NodeKind.Type).name)
                with(details(NodeKind.Parameter).single()) {
                    val parameterType = detail(NodeKind.Type)
                    assertEquals("IntArray", parameterType.name)
                }
            }
        }
    }

    @Test fun typeParameter() {
        verifyJavaPackageMember("testdata/java/typeParameter.java") { cls ->
            val typeParameters = cls.details(NodeKind.TypeParameter)
            with(typeParameters.single()) {
                assertEquals("T", name)
                with(detail(NodeKind.UpperBound)) {
                    assertEquals("Comparable", name)
                    assertEquals("T", detail(NodeKind.Type).name)
                }
            }
            with(cls.members(NodeKind.Function).single()) {
                val methodTypeParameters = details(NodeKind.TypeParameter)
                with(methodTypeParameters.single()) {
                    assertEquals("E", name)
                }
            }
        }
    }

    @Test fun constructors() {
        verifyJavaPackageMember("testdata/java/constructors.java") { cls ->
            val constructors = cls.members(NodeKind.Constructor)
            assertEquals(2, constructors.size)
            with(constructors[0]) {
                assertEquals("<init>", name)
            }
        }
    }

    @Test fun innerClass() {
        verifyJavaPackageMember("testdata/java/InnerClass.java") { cls ->
            val innerClass = cls.members(NodeKind.Class).single()
            assertEquals("D", innerClass.name)
        }
    }

    @Test fun varargs() {
        verifyJavaPackageMember("testdata/java/varargs.java") { cls ->
            val fn = cls.members(NodeKind.Function).single()
            val param = fn.detail(NodeKind.Parameter)
            assertEquals("vararg", param.details(NodeKind.Modifier).first().name)
            val psiType = param.detail(NodeKind.Type)
            assertEquals("String", psiType.name)
            assertTrue(psiType.details(NodeKind.Type).isEmpty())
        }
    }

    @Test fun fields() {
        verifyJavaPackageMember("testdata/java/field.java") { cls ->
            val i = cls.members(NodeKind.Property).single { it.name == "i" }
            assertEquals("Int", i.detail(NodeKind.Type).name)
            assertTrue("var" in i.details(NodeKind.Modifier).map { it.name })

            val s = cls.members(NodeKind.Property).single { it.name == "s" }
            assertEquals("String", s.detail(NodeKind.Type).name)
            assertFalse("var" in s.details(NodeKind.Modifier).map { it.name })
            assertTrue("static" in s.details(NodeKind.Modifier).map { it.name })
        }
    }

    @Test fun staticMethod() {
        verifyJavaPackageMember("testdata/java/staticMethod.java") { cls ->
            val m = cls.members(NodeKind.Function).single { it.name == "foo" }
            assertTrue("static" in m.details(NodeKind.Modifier).map { it.name })
        }
    }

    /**
     *  `@suppress` not supported in Java!
     *
     *  [Proposed tags](http://www.oracle.com/technetwork/java/javase/documentation/proposed-tags-142378.html)
     *  Proposed tag `@exclude` for it, but not supported yet
     */
    @Ignore("@suppress not supported in Java!") @Test fun suppressTag() {
        verifyJavaPackageMember("testdata/java/suppressTag.java") { cls ->
            assertEquals(1, cls.members(NodeKind.Function).size)
        }
    }

    @Test fun annotatedAnnotation() {
        verifyJavaPackageMember("testdata/java/annotatedAnnotation.java") { cls ->
            assertEquals(1, cls.annotations.size)
            with(cls.annotations[0]) {
                assertEquals(1, details.count())
                with(details[0]) {
                    assertEquals(NodeKind.Parameter, kind)
                    assertEquals(1, details.count())
                    with(details[0]) {
                        assertEquals(NodeKind.Value, kind)
                        assertEquals("[AnnotationTarget.FIELD, AnnotationTarget.CLASS, AnnotationTarget.FILE, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER]", name)
                    }
                }
            }
        }
    }

    @Test fun deprecation() {
        verifyJavaPackageMember("testdata/java/deprecation.java") { cls ->
            val fn = cls.members(NodeKind.Function).single()
            assertEquals("This should no longer be used", fn.deprecation!!.content.toTestString())
        }
    }

    @Test fun javaLangObject() {
        verifyJavaPackageMember("testdata/java/javaLangObject.java") { cls ->
            val fn = cls.members(NodeKind.Function).single()
            assertEquals("Any", fn.detail(NodeKind.Type).name)
        }
    }

    @Test fun enumValues() {
        verifyJavaPackageMember("testdata/java/enumValues.java") { cls ->
            val superTypes = cls.details(NodeKind.Supertype)
            assertEquals(0, superTypes.size)
            assertEquals(1, cls.members(NodeKind.EnumItem).size)
        }
    }

    @Test fun inheritorLinks() {
        verifyJavaPackageMember("testdata/java/InheritorLinks.java") { cls ->
            val fooClass = cls.members.single { it.name == "Foo" }
            val inheritors = fooClass.references(RefKind.Inheritor)
            assertEquals(1, inheritors.size)
        }
    }

    @Test fun linksToMethodsWithNativeJavaObjectParameters() {
        verifyJavaPackageMember("testdata/java/javaLangParameters.java") { cls ->
            val lazyLinks = ((cls.content.summary as ContentParagraph).children.stream().map { (it as ContentCode).children }).collect(
                Collectors.toList()).flatten()
            lazyLinks.forEach(Consumer {
                val lazyLink = it as ContentNodeLazyLink
                assertNotNull(lazyLink.node)
            })
        }
    }
}
