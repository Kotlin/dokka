package org.jetbrains.dokka.tests

import kotlin.test.*
import org.jetbrains.dokka.*
import org.junit.*

public class JavaTest {
    Test fun function() {
        verifyPackageMember("test/data/java/member.java") { cls ->
            assertEquals("Test", cls.name)
            assertEquals(DocumentationNode.Kind.Class, cls.kind)
            with(cls.members.single()) {
                assertEquals("fn", name)
                assertEquals(DocumentationNode.Kind.Function, kind)
                assertEquals("Summary for Function", content.summary.toTestString())
                assertEquals(2, content.sections.size())
                with(content.sections[0]) {
                    assertEquals("Parameters", tag)
                    assertEquals("name", subjectName)
                    assertEquals("is String parameter ", toTestString())
                }
                with(content.sections[1]) {
                    assertEquals("Parameters", tag)
                    assertEquals("value", subjectName)
                    assertEquals("is int parameter", toTestString())
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

    Test fun memberWithModifiers() {
        verifyPackageMember("test/data/java/memberWithModifiers.java") { cls ->
            assertEquals("abstract", cls.details[0].name)
            with(cls.members.single()) {
                assertEquals("protected", details[0].name)
            }
        }
    }

    Test fun superClass() {
        verifyPackageMember("test/data/java/superClass.java") { cls ->
            val superTypes = cls.details(DocumentationNode.Kind.Supertype)
            assertEquals(2, superTypes.size())
            assertEquals("Exception", superTypes[0].name)
            assertEquals("Cloneable", superTypes[1].name)
        }
    }

    Test fun arrayType() {
        verifyPackageMember("test/data/java/arrayType.java") { cls ->
            with(cls.members.single()) {
                assertEquals("Array<String>", detail(DocumentationNode.Kind.Type).name)
                with(details(DocumentationNode.Kind.Parameter).single()) {
                    assertEquals("Array<Int>", detail(DocumentationNode.Kind.Type).name)
                }
            }
        }
    }

    Test fun typeParameter() {
        verifyPackageMember("test/data/java/typeParameter.java") { cls ->
            val typeParameters = cls.details(DocumentationNode.Kind.TypeParameter)
            with(typeParameters.single()) {
                assertEquals("T", name)
                with(detail(DocumentationNode.Kind.UpperBound)) {
                    assertEquals("Comparable", name)
                    assertEquals("T", detail(DocumentationNode.Kind.TypeParameter).name)
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

    Test fun constructors() {
        verifyPackageMember("test/data/java/constructors.java") { cls ->
            val constructors = cls.members(DocumentationNode.Kind.Constructor)
            assertEquals(2, constructors.size())
            with(constructors[0]) {
                assertEquals("<init>", name)
            }
        }
    }

    Test fun innerClass() {
        verifyPackageMember("test/data/java/innerClass.java") { cls ->
            val innerClass = cls.members(DocumentationNode.Kind.Class).single()
            assertEquals("D", innerClass.name)
        }
    }
}
