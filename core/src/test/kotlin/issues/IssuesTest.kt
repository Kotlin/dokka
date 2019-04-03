package issues

import org.jetbrains.dokka.DocumentationNode
import org.jetbrains.dokka.NodeKind
import org.jetbrains.dokka.tests.toTestString
import org.jetbrains.dokka.tests.verifyModel
import org.junit.Test
import kotlin.test.assertEquals


class IssuesTest {

    @Test
    fun errorClasses() {
        verifyModel("testdata/issues/errorClasses.kt", withJdk = true, withKotlinRuntime = true) { model ->
            val cls = model.members.single().members.single()

            fun DocumentationNode.returnType() = this.details.find { it.kind == NodeKind.Type }?.name
            assertEquals("Test", cls.members[1].returnType())
            assertEquals("List", cls.members[2].returnType())
            assertEquals("Test", cls.members[3].returnType())
            assertEquals("Test", cls.members[4].returnType())
            assertEquals("String", cls.members[5].returnType())
            assertEquals("String", cls.members[6].returnType())
            assertEquals("String", cls.members[7].returnType())
        }
    }
}
