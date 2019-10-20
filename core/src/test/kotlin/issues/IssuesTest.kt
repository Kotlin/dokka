package issues

import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.tests.ModelConfig
import org.jetbrains.dokka.tests.checkSourceExistsAndVerifyModel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

abstract class BaseIssuesTest(val analysisPlatform: Platform) {
    val defaultModelConfig = ModelConfig(analysisPlatform = analysisPlatform)

    @Test
    fun errorClasses() {
        checkSourceExistsAndVerifyModel("testdata/issues/errorClasses.kt",
            modelConfig = ModelConfig(analysisPlatform = analysisPlatform, withJdk = true, withKotlinRuntime = true)) { model ->
            val cls = model.members.single().members.single()

            fun DocumentationNode.returnType() = this.details.find { it.kind == NodeKind.Type }?.name
            assertEquals("Test", cls.members[1].returnType())
            assertEquals("Test", cls.members[2].returnType())
            assertEquals("Test", cls.members[3].returnType())
            assertEquals("List", cls.members[4].returnType())
            assertEquals("String", cls.members[5].returnType())
            assertEquals("String", cls.members[6].returnType())
            assertEquals("String", cls.members[7].returnType())
        }
    }

    /**
     * Tests that types not available on the classpath can still be displayed as simple types (instead of `<ERROR CLASS>`)
     */
    @Test
    fun missingClasses() {
        checkSourceExistsAndVerifyModel("testdata/issues/missingClasses.kt",
            modelConfig = ModelConfig(analysisPlatform = analysisPlatform, withJdk = true, withKotlinRuntime = true)) { model ->
            val cls = model.members.single().members.single()

            assertEquals(
                "MissingClassInConstructor",
                cls
                    .members[0]                       // constructor
                    .detailOrNull(NodeKind.Parameter) // constructor parameter
                    ?.detailOrNull(NodeKind.Type)     // parameter type
                    ?.name
            )
            assertEquals(
                "MissingClassInDeclaredReturnType",
                cls
                    .members[1]                  // method
                    .detailOrNull(NodeKind.Type) // method return type
                    ?.name
            )
            assertEquals(
                "MissingClassInInferredReturnType().getSomeOtherProperty() + \"appended expression value\"",
                cls
                    .members[2]                  // method
                    .detailOrNull(NodeKind.Type) // method return type
                    ?.name
            )
            assertEquals(
                "MissingClassInMethodParameter",
                cls
                    .members[3]
                    .detailOrNull(NodeKind.Parameter) // method parameter
                    ?.detailOrNull(NodeKind.Type)     // parameter type
                    ?.name
            )
            cls
                .members[4]
                .detailOrNull(NodeKind.TypeParameter)
                ?.let {
                    assertNotNull(it)
                    assertEquals(0, it.details.size) // should be non-empty with upper bound of MissingClassInTypeParameter
                }
            assertEquals(
                "MissingClassAsGeneric",
                cls
                    .members[5]
                    .detailOrNull(NodeKind.Type)  // method return type
                    ?.detailOrNull(NodeKind.Type) // List's generic return type
                    ?.name
            )

            assertEquals(
                "MissingClassReceiver",
                cls
                    .members[6]
                    .detailOrNull(NodeKind.Receiver) // method receiver
                    ?.detailOrNull(NodeKind.Type)    // parameter type
                    ?.name
            )
            assertEquals(
                "MissingClassLambdaReceiver",
                cls
                    .members[7]
                    .detailOrNull(NodeKind.Parameter)                   // method parameter
                    ?.detailOrNull(NodeKind.Type)                       // parameter type
                    ?.details?.firstOrNull { it.kind == NodeKind.Type } // lambda receiver type
                    ?.name
            )
            assertEquals(
                "MissingClassLambdaParameter",
                cls
                    .members[8]
                    .detailOrNull(NodeKind.Parameter)                   // method parameter
                    ?.detailOrNull(NodeKind.Type)                       // parameter type
                    ?.details?.firstOrNull { it.kind == NodeKind.Type } // lambda parameter type
                    ?.name
            )
            assertEquals(
                "MissingClassLambdaReturnType",
                cls
                    .members[9]
                    .detailOrNull(NodeKind.Parameter) // method parameter
                    ?.detailOrNull(NodeKind.Type)     // parameter type
                    ?.detailOrNull(NodeKind.Type)     // lambda receiver type
                    ?.name
            )
            assertEquals(
                "MissingClassInDeclaredPropertyType",
                cls
                    .members[10]
                    .detailOrNull(NodeKind.Type)     // property type
                    ?.name
            )
            assertEquals(
                "MissingClassInInferredPropertyType()",
                cls
                    .members[11]
                    .detailOrNull(NodeKind.Type)     // property type
                    ?.name
            )
            assertEquals(
                "get() = MissingClassInInferredPropertyGetterType()",
                cls
                    .members[12]
                    .detailOrNull(NodeKind.Type)     // property type
                    ?.name
            )
        }
    }
}

class JSIssuesTest: BaseIssuesTest(Platform.js)
class JVMIssuesTest: BaseIssuesTest(Platform.jvm)
class CommonIssuesTest: BaseIssuesTest(Platform.common)
