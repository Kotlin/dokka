package model

import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import utils.assertNotNull
import utils.name

class JavaTest : AbstractModelTest("/src/main/kotlin/java/Test.java", "java") {

    @Test //todo params in comments
    fun function() {
        inlineModelTest(
            """
            |class Test {
            |    /**
            |     * Summary for Function
            |     * @param name is String parameter
            |     * @param value is int parameter
            |     */
            |    public void fn(String name, int value) {}
            |}
            """
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                name equals "Test"
                children counts 1
                with((this / "fn").cast<DFunction>()) {
                    name equals "fn"
                    this
                }
            }
        }
    }

    //@Test fun function() {
    //        verifyJavaPackageMember("testdata/java/member.java", defaultModelConfig) { cls ->
    //            assertEquals("Test", cls.name)
    //            assertEquals(NodeKind.Class, cls.kind)
    //            with(cls.members(NodeKind.Function).single()) {
    //                assertEquals("fn", name)
    //                assertEquals("Summary for Function", content.summary.toTestString().trimEnd())
    //                assertEquals(3, content.sections.size)
    //                with(content.sections[0]) {
    //                    assertEquals("Parameters", tag)
    //                    assertEquals("name", subjectName)
    //                    assertEquals("render(Type:String,SUMMARY): is String parameter", toTestString())
    //                }
    //                with(content.sections[1]) {
    //                    assertEquals("Parameters", tag)
    //                    assertEquals("value", subjectName)
    //                    assertEquals("render(Type:Int,SUMMARY): is int parameter", toTestString())
    //                }
    //                assertEquals("Unit", detail(NodeKind.Type).name)
    //                assertTrue(members.none())
    //                assertTrue(links.none())
    //                with(details.first { it.name == "name" }) {
    //                    assertEquals(NodeKind.Parameter, kind)
    //                    assertEquals("String", detail(NodeKind.Type).name)
    //                }
    //                with(details.first { it.name == "value" }) {
    //                    assertEquals(NodeKind.Parameter, kind)
    //                    assertEquals("Int", detail(NodeKind.Type).name)
    //                }
    //            }
    //        }
    //    }

    @Test // todo
    fun memberWithModifiers() {
        inlineModelTest(
            """
            |class Test {
            |    /**
            |     * Summary for Function
            |     * @param name is String parameter
            |     * @param value is int parameter
            |     */
            |    public void fn(String name, int value) {}
            |}
            """
        ) {
            with((this / "java" / "Test" / "fn").cast<DFunction>()) {
                this
            }
        }
    }

    //    @Test fun memberWithModifiers() {
    //        verifyJavaPackageMember("testdata/java/memberWithModifiers.java", defaultModelConfig) { cls ->
    //            val modifiers = cls.details(NodeKind.Modifier).map { it.name }
    //            assertTrue("abstract" in modifiers)
    //            with(cls.members.single { it.name == "fn" }) {
    //                assertEquals("protected", details[0].name)
    //            }
    //            with(cls.members.single { it.name == "openFn" }) {
    //                assertEquals("open", details[1].name)
    //            }
    //        }
    //    }

    @Test
    fun superClass() {
        inlineModelTest(
            """
            |public class Foo extends Exception implements Cloneable {}
            """
        ) {
            with((this / "java" / "Foo").cast<DClass>()) {
                val sups = listOf("Exception", "Cloneable")
                assertTrue(
                    sups.all { s -> supertypes.values.flatten().any { it.classNames == s } })
                "Foo must extend ${sups.joinToString(", ")}"
            }
        }
    }

    @Test
    fun arrayType() {
        inlineModelTest(
            """
            |class Test {
            |    public String[] arrayToString(int[] data) {
            |      return null;
            |    }
            |}
            """
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                name equals "Test"
                children counts 1

                with((this / "arrayToString").cast<DFunction>()) {
                    name equals "arrayToString"
                    type.name equals "Array"
                    with(parameters.firstOrNull().assertNotNull("parameters")) {
                        name equals "data"
                        type.name equals "Array"
                    }
                }
            }
        }
    }

    @Test
    fun typeParameter() {
        inlineModelTest(
            """
            |class Foo<T extends Comparable<T>> {
            |     public <E> E foo();
            |}
            """
        ) {
            with((this / "java" / "Foo").cast<DClass>()) {
                generics counts 1
            }
        }
    }

    //    @Test fun typeParameter() {
    //        verifyJavaPackageMember("testdata/java/typeParameter.java", defaultModelConfig) { cls ->
    //            val typeParameters = cls.details(NodeKind.TypeParameter)
    //            with(typeParameters.single()) {
    //                assertEquals("T", name)
    //                with(detail(NodeKind.UpperBound)) {
    //                    assertEquals("Comparable", name)
    //                    assertEquals("T", detail(NodeKind.Type).name)
    //                }
    //            }
    //            with(cls.members(NodeKind.Function).single()) {
    //                val methodTypeParameters = details(NodeKind.TypeParameter)
    //                with(methodTypeParameters.single()) {
    //                    assertEquals("E", name)
    //                }
    //            }
    //        }
    //    }

    @Test
    fun constructors() {
        inlineModelTest(
            """
            |class Test {
            |  public Test() {}
            |
            |  public Test(String s) {}
            |}
            """
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                name equals "Test"

                constructors counts 2
                constructors.find { it.parameters.isNullOrEmpty() }.assertNotNull("Test()")

                with(constructors.find { it.parameters.isNotEmpty() }.assertNotNull("Test(String)")) {
                    parameters.firstOrNull()?.type?.name equals "String"
                }
            }
        }
    }

    @Test
    fun innerClass() {
        inlineModelTest(
            """
            |class InnerClass {
            |    public class D {}
            |}
            """
        ) {
            with((this / "java" / "InnerClass").cast<DClass>()) {
                children counts 1
                with((this / "D").cast<DClass>()) {
                    name equals "D"
                    children counts 0
                }
            }
        }
    }

    @Test
    fun varargs() {
        inlineModelTest(
            """
            |class Foo {
            |     public void bar(String... x);
            |}
            """
        ) {
            with((this / "java" / "Foo").cast<DClass>()) {
                name equals "Foo"
                children counts 1

                with((this / "bar").cast<DFunction>()) {
                    name equals "bar"
                    with(parameters.firstOrNull().assertNotNull("parameter")) {
                        name equals "x"
                        type.name equals "Array"
                    }
                }
            }
        }
    }

    @Test // todo
    fun fields() {
        inlineModelTest(
            """
            |class Test {
            |  public int i;
            |  public static final String s;
            |}
            """
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                children counts 2

                with((this / "i").cast<DProperty>()) {
                    getter equals null
                    setter equals null
                }

                with((this / "s").cast<DProperty>()) {
                    getter equals null
                    setter equals null
                }
            }
        }
    }

    //    @Test fun fields() {
    //        verifyJavaPackageMember("testdata/java/field.java", defaultModelConfig) { cls ->
    //            val i = cls.members(NodeKind.Property).single { it.name == "i" }
    //            assertEquals("Int", i.detail(NodeKind.Type).name)
    //            assertTrue("var" in i.details(NodeKind.Modifier).map { it.name })
    //
    //            val s = cls.members(NodeKind.Property).single { it.name == "s" }
    //            assertEquals("String", s.detail(NodeKind.Type).name)
    //            assertFalse("var" in s.details(NodeKind.Modifier).map { it.name })
    //            assertTrue("static" in s.details(NodeKind.Modifier).map { it.name })
    //        }
    //    }

    @Test
    fun staticMethod() {
        inlineModelTest(
            """
            |class C {
            |  public static void foo() {}
            |}
            """
        ) {
            with((this / "java" / "C" / "foo").cast<DFunction>()) {
                with(extra[AdditionalModifiers]!!.content.entries.single().value.assertNotNull("AdditionalModifiers")) {
                    this counts 1
                    first() equals ExtraModifiers.JavaOnlyModifiers.Static
                }
            }
        }
    }

    //    @Test fun staticMethod() { todo
    //        verifyJavaPackageMember("testdata/java/staticMethod.java", defaultModelConfig) { cls ->
    //            val m = cls.members(NodeKind.Function).single { it.name == "foo" }
    //            assertTrue("static" in m.details(NodeKind.Modifier).map { it.name })
    //        }
    //    }
    //
    //    /**
    //     *  `@suppress` not supported in Java!
    //     *
    //     *  [Proposed tags](https://www.oracle.com/technetwork/java/javase/documentation/proposed-tags-142378.html)
    //     *  Proposed tag `@exclude` for it, but not supported yet
    //     */
    //    @Ignore("@suppress not supported in Java!") @Test fun suppressTag() {
    //        verifyJavaPackageMember("testdata/java/suppressTag.java", defaultModelConfig) { cls ->
    //            assertEquals(1, cls.members(NodeKind.Function).size)
    //        }
    //    }

    @Test
    fun annotatedAnnotation() {
        inlineModelTest(
            """
            |import java.lang.annotation.*;
            |
            |@Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
            |public @interface Attribute {
            |  String value() default "";
            |}
            """
        ) {
            with((this / "java" / "Attribute").cast<DAnnotation>()) {
                with(extra[Annotations]!!.content.entries.single().value.assertNotNull("Annotations")) {
                    with(single()) {
                        dri.classNames equals "Target"
                        (params["value"].assertNotNull("value") as ArrayValue).value equals listOf(
                            EnumValue("ElementType.FIELD", DRI("java.lang.annotation", "ElementType")),
                            EnumValue("ElementType.TYPE", DRI("java.lang.annotation", "ElementType")),
                            EnumValue("ElementType.METHOD", DRI("java.lang.annotation", "ElementType"))
                        )
                    }
                }
            }
        }
    }

    //    @Test fun deprecation() {
    //        verifyJavaPackageMember("testdata/java/deprecation.java", defaultModelConfig) { cls ->
    //            val fn = cls.members(NodeKind.Function).single()
    //            assertEquals("This should no longer be used", fn.deprecation!!.content.toTestString())
    //        }
    //    }

    @Test
    fun javaLangObject() {
        inlineModelTest(
            """
            |class Test {
            |  public Object fn() { return null; }
            |}
            """
        ) {
            with((this / "java" / "Test" / "fn").cast<DFunction>()) {
                assertTrue(type is JavaObject)
            }
        }
    }

    //    @Test fun javaLangObject() {
    //        verifyJavaPackageMember("testdata/java/javaLangObject.java", defaultModelConfig) { cls ->
    //            val fn = cls.members(NodeKind.Function).single()
    //            assertEquals("Any", fn.detail(NodeKind.Type).name)
    //        }
    //    }

    @Test
    fun enumValues() {
        inlineModelTest(
            """
            |enum E {
            |  Foo
            |}
            """
        ) {
            with((this / "java" / "E").cast<DEnum>()) {
                name equals "E"
                entries counts 1

                with((this / "Foo").cast<DEnumEntry>()) {
                    name equals "Foo"
                }
            }
        }
    }

    @Test
    fun inheritorLinks() {
        inlineModelTest(
            """
            |public class InheritorLinks {
            |  public static class Foo {}
            |
            |  public static class Bar extends Foo {}
            |}
            """
        ) {
            with((this / "java" / "InheritorLinks").cast<DClass>()) {
                val dri = (this / "Bar").assertNotNull("Foo dri").dri
                with((this / "Foo").cast<DClass>()) {
                    with(extra[InheritorsInfo].assertNotNull("InheritorsInfo")) {
                        with(value.values.flatten().distinct()) {
                            this counts 1
                            first() equals dri
                        }
                    }
                }
            }
        }
    }

    //    todo
    //    @Test fun inheritorLinks() {
    //        verifyJavaPackageMember("testdata/java/InheritorLinks.java", defaultModelConfig) { cls ->
    //            val fooClass = cls.members.single { it.name == "Foo" }
    //            val inheritors = fooClass.references(RefKind.Inheritor)
    //            assertEquals(1, inheritors.size)
    //        }
    //    }
}