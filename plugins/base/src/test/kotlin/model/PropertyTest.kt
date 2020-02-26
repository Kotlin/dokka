package model

import org.jetbrains.dokka.model.KotlinVisibility
import org.jetbrains.dokka.model.Package
import org.jetbrains.dokka.model.Property
import org.junit.Test
import utils.AbstractModelTest
import utils.assertNotNull

class PropertyTest : AbstractModelTest("/src/main/kotlin/property/Test.kt", "property") {

    @Test
    fun valueProperty() {
        inlineModelTest(
            """
            |val property = "test""""
        ) {
            with((this / "property" / "property").cast<Property>()) {
                name equals "property"
                children counts 0
                with(getter.assertNotNull("Getter")) {
                    type.constructorFqName equals "kotlin.String"
                }
                 type.constructorFqName equals "kotlin.String"
            }
        }
    }

    @Test
    fun variableProperty() {
        inlineModelTest(
            """
            |var property = "test"
            """
        ) {
            with((this / "property" / "property").cast<Property>()) {
                name equals "property"
                children counts 0
                setter.assertNotNull("Setter")
                with(getter.assertNotNull("Getter")) {
                    type.constructorFqName equals "kotlin.String"
                }
                 type.constructorFqName equals "kotlin.String"
            }
        }
    }

    @Test
    fun valuePropertyWithGetter() {
        inlineModelTest(
            """
            |val property: String
            |    get() = "test"
            """
        ) {
            with((this / "property" / "property").cast<Property>()) {
                name equals "property"
                children counts 0
                with(getter.assertNotNull("Getter")) {
                    type.constructorFqName equals "kotlin.String"
                }
                 type.constructorFqName equals "kotlin.String"
            }
        }
    }

    @Test
    fun variablePropertyWithAccessors() {
        inlineModelTest(
            """
            |var property: String
            |    get() = "test"
            |    set(value) {}
            """
        ) {
            with((this / "property" / "property").cast<Property>()) {
                name equals "property"
                children counts 0
                setter.assertNotNull("Setter")
                with(getter.assertNotNull("Getter")) {
                    type.constructorFqName equals "kotlin.String"
                }
                visibility.values allEquals KotlinVisibility.Public
            }
        }
    }

    @Test
    fun propertyWithReceiver() {
        inlineModelTest(
            """
            |val String.property: Int
            |    get() = size() * 2
            """
        ) {
            with((this / "property" / "property").cast<Property>()) {
                name equals "property"
                children counts 0
                with(receiver.assertNotNull("property receiver")) {
                    name equals null
                    type.constructorFqName equals "kotlin.String"
                }
                with(getter.assertNotNull("Getter")) {
                    type.constructorFqName equals "kotlin.Int"
                }
                visibility.values allEquals KotlinVisibility.Public
            }
        }
    }

    @Test
    fun propertyOverride() {
        inlineModelTest(
            """
            |open class Foo() {
            |    open val property: Int get() = 0
            |}
            |class Bar(): Foo() {
            |    override val property: Int get() = 1
            |}
            """
        ) {
            with((this / "property").cast<Package>()) {
                with((this / "Foo" / "property").cast<Property>()) {
                    name equals "property"
                    children counts 0
                    with(getter.assertNotNull("Getter")) {
                        type.constructorFqName equals "kotlin.Int"
                    }
                }
                with((this / "Bar" / "property").cast<Property>()) {
                    name equals "property"
                    children counts 0
                    with(getter.assertNotNull("Getter")) {
                        type.constructorFqName equals "kotlin.Int"
                    }
                }
            }
        }
    }

    // todo
//    @Test fun sinceKotlin() {
//        checkSourceExistsAndVerifyModel("testdata/properties/sinceKotlin.kt", defaultModelConfig) { model ->
//            with(model.members.single().members.single()) {
//                assertEquals("1.1", sinceKotlin)
//            }
//        }
//    }
//}
//
//class JSPropertyTest: BasePropertyTest(Platform.js) {}
//
//class JVMPropertyTest : BasePropertyTest(Platform.jvm) {
//    @Test
//    fun annotatedProperty() {
//        checkSourceExistsAndVerifyModel(
//            "testdata/properties/annotatedProperty.kt",
//            modelConfig = ModelConfig(
//                analysisPlatform = analysisPlatform,
//                withKotlinRuntime = true
//            )
//        ) { model ->
//            with(model.members.single().members.single()) {
//                Assert.assertEquals(1, annotations.count())
//                with(annotations[0]) {
//                    Assert.assertEquals("Strictfp", name)
//                    Assert.assertEquals(Content.Empty, content)
//                    Assert.assertEquals(NodeKind.Annotation, kind)
//                }
//            }
//        }
//    }
//
//}
}