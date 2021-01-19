package model.annotations

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DFunction
import org.junit.jupiter.api.Test
import utils.AbstractModelTest
import kotlin.test.assertEquals

class JavaAnnotationsForParametersTest : AbstractModelTest("/src/main/kotlin/java/Test.java", "java") {

    @Test
    fun `function with deprecated parameter`() {
        inlineModelTest(
            """
            |class Test {
            |    public void fn(@Deprecated String name) {}
            |}
        """.trimIndent()
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                with((this / "fn").cast<DFunction>()) {
                    val dri =
                        parameters.first().extra[Annotations]?.directAnnotations?.flatMap { it.value }?.map { it.dri }
                    assertEquals(listOf(DRI("java.lang", "Deprecated")), dri)
                }
            }
        }
    }

    @Test
    fun `function with parameter that has custom annotation`() {
        inlineModelTest(
            """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.PARAMETER)
            |public @interface Hello {
            |    public String bar() default "";
            |}
            |class Test {
            |   public void foo(@Hello(bar = "baz") String arg){ }
            |}
        """.trimIndent()
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                with((this / "foo").cast<DFunction>()) {
                    val annotations =
                        parameters.first().extra[Annotations]?.directAnnotations?.flatMap { it.value }
                    val driOfHello = DRI("java", "Hello")
                    val annotationsValues = annotations?.flatMap { it.params.values }?.map { it.toString() }?.toList()

                    assertEquals(listOf(driOfHello), annotations?.map { it.dri })
                    assertEquals(listOf("baz"), annotationsValues)
                }
            }
        }
    }

    @Test
    fun `function with annotated generic parameter`() {
        inlineModelTest(
            """
            |@Retention(RetentionPolicy.RUNTIME)
            |@Target(ElementType.TYPE_PARAMETER)
            |@interface Hello {
            |    public String bar() default "";
            |}
            |class Test {
            |   public <@Hello(bar = "baz") T> List<T> foo() {
            |        return null;
            |   }
            |}
        """.trimIndent()
        ) {
            with((this / "java" / "Test").cast<DClass>()) {
                with((this / "foo").cast<DFunction>()) {
                    val annotations = generics.first().extra[Annotations]?.directAnnotations?.flatMap { it.value }
                    val driOfHello = DRI("java", "Hello")
                    val annotationsValues = annotations?.flatMap { it.params.values }?.map { it.toString() }?.toList()

                    assertEquals(listOf(driOfHello), annotations?.map { it.dri })
                    assertEquals(listOf("baz"), annotationsValues)
                }
            }
        }
    }
}