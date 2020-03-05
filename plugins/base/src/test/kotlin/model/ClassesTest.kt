package model

import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.KotlinModifier.*
import org.jetbrains.dokka.model.Function
import org.junit.Test
import utils.AbstractModelTest
import utils.assertNotNull
import utils.supers


class ClassesTest : AbstractModelTest("/src/main/kotlin/classes/Test.kt", "classes") {

    @Test
    fun emptyClass() {
        inlineModelTest(
            """
            |class Klass {}"""
        ) {
            with((this / "classes" / "Klass").cast<Class>()) {
                name equals "Klass"
                children counts 4
            }
        }
    }

    @Test
    fun emptyObject() {
        inlineModelTest(
            """
            |object Obj {}
            """
        ) {
            with((this / "classes" / "Obj").cast<Object>()) {
                name equals "Obj"
                children counts 3
            }
        }
    }

    @Test
    fun classWithConstructor() {
        inlineModelTest(
            """
            |class Klass(name: String)
        """
        ) {
            with((this / "classes" / "Klass").cast<Class>()) {
                name equals "Klass"
                children counts 4

                with(constructors.firstOrNull().assertNotNull("Constructor")) {
                    visibility.values allEquals KotlinVisibility.Public
                    parameters counts 1
                    with(parameters.firstOrNull().assertNotNull("Constructor parameter")) {
                        name equals "name"
                        type.constructorFqName equals "kotlin.String"
                    }
                }

            }
        }
    }

    @Test
    fun classWithFunction() {
        inlineModelTest(
            """
            |class Klass {
            |   fun fn() {}
            |}
            """
        ) {
            with((this / "classes" / "Klass").cast<Class>()) {
                name equals "Klass"
                children counts 5

                with((this / "fn").cast<Function>()) {
                    type.constructorFqName equals "kotlin.Unit"
                    parameters counts 0
                    visibility.values allEquals KotlinVisibility.Public
                }
            }
        }
    }

    @Test
    fun classWithProperty() {
        inlineModelTest(
            """
            |class Klass {
            |   val name: String = ""
            |}
            """
        ) {
            with((this / "classes" / "Klass").cast<Class>()) {
                name equals "Klass"
                children counts 5

                with((this / "name").cast<Property>()) {
                    name equals "name"
                    // TODO property name
                }
            }
        }
    }

    @Test
    fun classWithCompanionObject() {
        inlineModelTest(
            """
            |class Klass() {
            |   companion object {
            |        val x = 1
            |        fun foo() {}
            |    }
            |}
            """
        ) {
            with((this / "classes" / "Klass").cast<Class>()) {
                name equals "Klass"
                children counts 5

                with((this / "Companion").cast<Object>()) {
                    name equals "Companion"
                    children counts 5

                    with((this / "x").cast<Property>()) {
                        name equals "x"
                    }

                    with((this / "foo").cast<Function>()) {
                        name equals "foo"
                        parameters counts 0
                        type.constructorFqName equals "kotlin.Unit"
                    }
                }
            }
        }
    }

    @Test
    fun dataClass() {
        inlineModelTest(
            """
                |data class Klass() {}
                """
        ) {
            with((this / "classes" / "Klass").cast<Class>()) {
                name equals "Klass"
                visibility.values allEquals KotlinVisibility.Public
                with(extra[AdditionalModifiers.AdditionalKey].assertNotNull("Extras")) {
                    content.find{it == ExtraModifiers.DATA}.assertNotNull("data modifier")
                }
            }
        }
    }

//    @Test fun dataClass() {
//        verifyPackageMember("testdata/classes/dataClass.kt", defaultModelConfig) { cls ->
//            val modifiers = cls.details(NodeKind.Modifier).map { it.name }
//            assertTrue("data" in modifiers)
//        }
//    }

    @Test
    fun sealedClass() {
        inlineModelTest(
            """
                |sealed class Klass() {}
                """
        ) {
            with((this / "classes" / "Klass").cast<Class>()) {
                name equals "Klass"
                modifier equals KotlinModifier.Sealed
            }
        }
    }

//                // TODO modifiers
//    @Test fun annotatedClassWithAnnotationParameters() {
//        checkSourceExistsAndVerifyModel(
//            "testdata/classes/annotatedClassWithAnnotationParameters.kt",
//            defaultModelConfig
//        ) { model ->
//            with(model.members.single().members.single()) {
//                with(deprecation!!) {
//                    assertEquals("Deprecated", name)
//                    assertEquals(Content.Empty, content)
//                    assertEquals(NodeKind.Annotation, kind)
//                    assertEquals(1, details.count())
//                    with(details[0]) {
//                        assertEquals(NodeKind.Parameter, kind)
//                        assertEquals(1, details.count())
//                        with(details[0]) {
//                            assertEquals(NodeKind.Value, kind)
//                            assertEquals("\"should no longer be used\"", name)
//                        }
//                    }
//                }
//            }
//        }
//    }

    @Test
    fun notOpenClass() {
        inlineModelTest(
            """
                |open class C() {
                |    open fun f() {}
                |}
                |
                |class D() : C() {
                |    override fun f() {}
                |}
                """
        ) {
            val C = (this / "classes" / "C").cast<Class>()
            val D = (this / "classes" / "D").cast<Class>()

            with(C) {
                modifier equals Open
                with((this / "f").cast<Function>()) {
                    modifier equals Open
                }
            }
            with(D) {
                modifier equals Final
                with((this / "f").cast<Function>()) {
                    modifier equals Open
                }
                D.supertypes.flatMap { it.component2() }.firstOrNull() equals C.dri
            }
        }
    }

    @Test
    fun indirectOverride() {
        inlineModelTest(
            """
                |abstract class C() {
                |    abstract fun foo()
                |}
                |
                |abstract class D(): C()
                |
                |class E(): D() {
                |    override fun foo() {}
                |}
                """
        ) {
            val C = (this / "classes" / "C").cast<Class>()
            val D = (this / "classes" / "D").cast<Class>()
            val E = (this / "classes" / "E").cast<Class>()

            with(C) {
                modifier equals Abstract
                ((this / "foo").cast<Function>()).modifier equals Abstract
            }

            with(D) {
                modifier equals Abstract
            }

            with(E) {
                modifier equals Final

            }
            D.supers.firstOrNull() equals C.dri
            E.supers.firstOrNull() equals D.dri
        }
    }

    @Test // todo inner class
    fun innerClass() {
        inlineModelTest(
            """
                |class C {
                |    inner class D {}
                |}
                """
        ) {
            with((this / "classes" / "C").cast<Class>()) {

                with((this / "D").cast<Class>()) {
                }
            }
        }
    }

//                // TODO modifiers
//    @Test fun innerClass() {
//        verifyPackageMember("testdata/classes/innerClass.kt", defaultModelConfig) { cls ->
//            val innerClass = cls.members.single { it.name == "D" }
//            val modifiers = innerClass.details(NodeKind.Modifier)
//            assertEquals(3, modifiers.size)
//            assertEquals("inner", modifiers[2].name)
//        }
//    }

    @Test
    fun companionObjectExtension() {
        inlineModelTest(
            """
                |class Klass {
                |    companion object Default {}
                |}
                |
                |/**
                | * The def
                | */
                |val Klass.Default.x: Int get() = 1
                """
        ) {
            with((this / "classes" / "Klass").cast<Class>()) {
                name equals "Klass"

                with((this / "Default").cast<Object>()) {
                    name equals "Default"
                    // TODO extensions
                }
            }
        }
    }

//    @Test fun companionObjectExtension() {
//        checkSourceExistsAndVerifyModel("testdata/classes/companionObjectExtension.kt", defaultModelConfig) { model ->
//            val pkg = model.members.single()
//            val cls = pkg.members.single { it.name == "Foo" }
//            val extensions = cls.extensions.filter { it.kind == NodeKind.CompanionObjectProperty }
//            assertEquals(1, extensions.size)
//        }
//    }

    @Test
    fun secondaryConstructor() {
        inlineModelTest(
            """
                |class C() {
                |    /** This is a secondary constructor. */
                |    constructor(s: String): this() {}
                |}
                """
        ) {
            with((this / "classes" / "C").cast<Class>()) {
                name equals "C"
                constructors counts 2

                constructors.map { it.name } allEquals "<init>"

                with(constructors.find { it.parameters.isNullOrEmpty() } notNull "C()") {
                    parameters counts 0
                }

                with(constructors.find { it.parameters.isNotEmpty() } notNull "C(String)") {
                    parameters counts 1
                    with(parameters.firstOrNull() notNull "Constructor parameter") {
                        name equals "s"
                        type.constructorFqName equals "kotlin.String"
                    }
                }
            }
        }
    }

    // TODO modifiers
//    @Test fun sinceKotlin() {
//        checkSourceExistsAndVerifyModel("testdata/classes/sinceKotlin.kt", defaultModelConfig) { model ->
//            with(model.members.single().members.single()) {
//                assertEquals("1.1", sinceKotlin)
//            }
//        }
//    }

    @Test
    fun privateCompanionObject() {
        inlineModelTest(
            """
                |class Klass {
                |    private companion object {
                |        fun fn() {}
                |        val a = 0
                |    }
                |}
                """
        ) {
            with((this / "classes" / "Klass").cast<Class>()) {
                name equals "Klass"

                with((this / "Companion").cast<Object>()) {
                    name equals "Companion"
                    visibility.values allEquals KotlinVisibility.Private

                    with((this / "fn").cast<Function>()) {
                        name equals "fn"
                        parameters counts 0
                        receiver equals null
                    }
                }
            }
        }
    }

    // TODO annotations
//    @Test
//    fun annotatedClass() {
//        verifyPackageMember("testdata/classes/annotatedClass.kt", ModelConfig(
//            analysisPlatform = analysisPlatform,
//            withKotlinRuntime = true
//        )
//        ) { cls ->
//            Assert.assertEquals(1, cls.annotations.count())
//            with(cls.annotations[0]) {
//                Assert.assertEquals("Strictfp", name)
//                Assert.assertEquals(Content.Empty, content)
//                Assert.assertEquals(NodeKind.Annotation, kind)
//            }
//        }
//    }


// TODO annotations

//    @Test fun javaAnnotationClass() {
//        checkSourceExistsAndVerifyModel(
//            "testdata/classes/javaAnnotationClass.kt",
//            modelConfig = ModelConfig(analysisPlatform = analysisPlatform, withJdk = true)
//        ) { model ->
//            with(model.members.single().members.single()) {
//                Assert.assertEquals(1, annotations.count())
//                with(annotations[0]) {
//                    Assert.assertEquals("Retention", name)
//                    Assert.assertEquals(Content.Empty, content)
//                    Assert.assertEquals(NodeKind.Annotation, kind)
//                    with(details[0]) {
//                        Assert.assertEquals(NodeKind.Parameter, kind)
//                        Assert.assertEquals(1, details.count())
//                        with(details[0]) {
//                            Assert.assertEquals(NodeKind.Value, kind)
//                            Assert.assertEquals("RetentionPolicy.SOURCE", name)
//                        }
//                    }
//                }
//            }
//        }
//    }

}