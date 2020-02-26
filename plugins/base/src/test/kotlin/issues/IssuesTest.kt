package issues

import org.jetbrains.dokka.model.Class
import org.jetbrains.dokka.model.Function
import org.junit.Test
import utils.AbstractModelTest

class IssuesTest : AbstractModelTest("/src/main/kotlin/issues/Test.kt", "issues") {

    @Test
    fun errorClasses() {
        inlineModelTest(
            """
            |class Test(var value: String) {
            |    fun test(): List<String> = emptyList()
            |    fun brokenApply(v: String) = apply { value = v }
            |
            |    fun brokenRun(v: String) = run {
            |        value = v
            |        this
            |    }
            |
            |    fun brokenLet(v: String) = let {
            |        it.value = v
            |        it
            |    }
            |
            |    fun brokenGenerics() = listOf("a", "b", "c")
            |
            |    fun working(v: String) = doSomething()
            |
            |    fun doSomething(): String = "Hello"
            |}
        """
        ) {
            with((this / "issues" / "Test").cast<Class>()) {
                // passes
                (this / "working").cast<Function>().type.constructorFqName equals "kotlin.String"
                (this / "doSomething").cast<Function>().type.constructorFqName equals "kotlin.String"

                // fails
                (this / "brokenGenerics").cast<Function>().type.constructorFqName equals "kotlin.collections.List"
                (this / "brokenApply").cast<Function>().type.constructorFqName equals "issues.Test"
                (this / "brokenRun").cast<Function>().type.constructorFqName equals "issues.Test"
                (this / "brokenLet").cast<Function>().type.constructorFqName equals "issues.Test"
            }
        }
    }

    //@Test
    //    fun errorClasses() {
    //        checkSourceExistsAndVerifyModel("testdata/issues/errorClasses.kt",
    //            modelConfig = ModelConfig(analysisPlatform = analysisPlatform, withJdk = true, withKotlinRuntime = true)) { model ->
    //            val cls = model.members.single().members.single()
    //
    //            fun DocumentationNode.returnType() = this.details.find { it.kind == NodeKind.Type }?.name
    //            assertEquals("Test", cls.members[1].returnType())
    //            assertEquals("Test", cls.members[2].returnType())
    //            assertEquals("Test", cls.members[3].returnType())
    //            assertEquals("List", cls.members[4].returnType())
    //            assertEquals("String", cls.members[5].returnType())
    //            assertEquals("String", cls.members[6].returnType())
    //            assertEquals("String", cls.members[7].returnType())
    //        }
    //    }

}