package issues

import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.tests.ModelConfig
import org.jetbrains.dokka.tests.checkSourceExistsAndVerifyModel
import org.junit.Test
import kotlin.test.assertEquals

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
}

class JSIssuesTest: BaseIssuesTest(Platform.js)
class JVMIssuesTest: BaseIssuesTest(Platform.jvm)
class CommonIssuesTest: BaseIssuesTest(Platform.common)