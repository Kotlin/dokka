package signatures

import matchers.content.assertNode
import matchers.content.hasExactText
import org.jetbrains.dokka.model.firstMemberOfType
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.testApi.logger.TestLogger
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.utilities.DokkaConsoleLogger
import org.jetbrains.dokka.utilities.LoggingLevel
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.reflect.KClass

class ObviousTypeSkippingTest : BaseAbstractTest(
    logger = TestLogger(DokkaConsoleLogger(LoggingLevel.WARN))
) {

    private fun source(signature: String) =
        """
            |/src/test.kt
            |package example
            |
            | $signature
            """.trimIndent()

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src")
                classpath = listOfNotNull(jvmStdlibPath)
            }
        }
    }

    companion object TestDataSources {
        @JvmStatic
        fun `run tests for obvious types omitting`() = listOf(
            forFunction("fun underTest(): Int = 5", "fun underTest(): Int"),
            forFunction("fun underTest() = 5", "fun underTest(): Int"),
            forFunction("fun underTest() {}", "fun underTest()"),
            forFunction("fun underTest() = println(6)", "fun underTest()"),
            forFunction("fun underTest(): Unit = println(6)", "fun underTest()"),
            forFunction("fun underTest(): Unit? = if (true) println(6) else null", "fun underTest(): Unit?"),
            forFunction("fun underTest() = if (true) println(6) else null", "fun underTest(): Unit?"),
            forFunction("fun underTest(): Any = if (true) 7 else true", "fun underTest(): Any"),
            forFunction("fun underTest() = if (true) 7 else true", "fun underTest(): Any"),
            forFunction("fun underTest(): Any? = if (true) 7 else (null as String?)", "fun underTest(): Any?"),
            forFunction("fun underTest() = if (true) 7 else (null as String?)", "fun underTest(): Any?"),
            forFunction("fun underTest(arg: Int) {}", "fun underTest(arg: Int)"),
            forFunction("fun underTest(arg: Unit) {}", "fun underTest(arg: Unit)"),
            forFunction("fun <T: Iterable<Any>> underTest(arg: T) {}", "fun <T : Iterable<Any>> underTest(arg: T)"),
            forFunction("fun <T: Iterable<Any?>> underTest(arg: T) {}", "fun <T : Iterable<Any?>> underTest(arg: T)"),
            forFunction("fun <T> underTest(arg: T) {}", "fun <T> underTest(arg: T)"),
            forFunction("fun <T: Any> underTest(arg: T) {}", "fun <T : Any> underTest(arg: T)"),
            forFunction("fun <T: Any?> underTest(arg: T) {}", "fun <T> underTest(arg: T)"),
            forProperty("val underTest: Int = 5", "val underTest: Int = 5"),
            forProperty("val underTest = 5", "val underTest: Int = 5"),
            forProperty("val underTest: Unit = println(5)", "val underTest: Unit"),
            forProperty("val underTest = println(5)", "val underTest: Unit"),
            forProperty("val underTest: Unit? = if (true) println(5) else null", "val underTest: Unit?"),
            forProperty("val underTest = if (true) println(5) else null", "val underTest: Unit?"),
            forProperty("val underTest: Any = if (true) println(5) else 5", "val underTest: Any"),
            forProperty("val underTest = if (true) println(5) else 5", "val underTest: Any"),
            forFunction("fun <T: Iterable<Any>> T.underTest() {}", "fun <T : Iterable<Any>> T.underTest()"),
            forFunction("fun <T: Iterable<Any?>> T.underTest() {}", "fun <T : Iterable<Any?>> T.underTest()"),
            forFunction("fun <T: Iterable<Any?>?> T.underTest() {}", "fun <T : Iterable<Any?>?> T.underTest()"),
            forFunction("fun <T: Any> T.underTest() {}", "fun <T : Any> T.underTest()"),
            forFunction("fun <T: Any?> T.underTest() {}", "fun <T> T.underTest()"),
            forFunction("fun <T> T.underTest() {}", "fun <T> T.underTest()"),
            forClass("class Testable<T: Any>", "class Testable<T : Any>"),
            forClass("class Testable<T: Any?>", "class Testable<T>"),
            forClass("class Testable<T: Any?>(t: T)", "class Testable<T>(t: T)"),
            forClass("class Testable<T>", "class Testable<T>"),
            forClass("class Testable(butWhy: Unit)", "class Testable(butWhy: Unit)"),
            forMethod("class Testable { fun underTest(): Int = 5 }", "fun underTest(): Int"),
            forMethod("class Testable { fun underTest() = 5 }", "fun underTest(): Int"),
            forMethod("class Testable { fun underTest() {} }", "fun underTest()"),
            forMethod("class Testable { fun underTest() = println(6) }", "fun underTest()"),
            forMethod("class Testable { fun underTest(): Unit = println(6) }", "fun underTest()"),
            forMethod(
                "class Testable { fun underTest(): Unit? = if (true) println(6) else null }",
                "fun underTest(): Unit?"
            ),
            forClassProperty("class Testable { val underTest: Unit = println(5) }", "val underTest: Unit"),
            forClassProperty("class Testable { val underTest = println(5) }", "val underTest: Unit"),
            forClassProperty(
                "class Testable { val underTest: Unit? = if (true) println(5) else null }",
                "val underTest: Unit?"
            ),
            forClassProperty(
                "class Testable { val underTest = if (true) println(5) else null }",
                "val underTest: Unit?"
            ),
            forClassProperty(
                "class Testable { val underTest: Any = if (true) println(5) else 5 }",
                "val underTest: Any"
            ),
            forClassProperty("class Testable { val underTest = if (true) println(5) else 5 }", "val underTest: Any"),
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    fun `run tests for obvious types omitting`(testData: TestData) {
        val (codeFragment, expectedSignature, placesToTest) = testData
        testInline(
            query = source(codeFragment),
            configuration = configuration
        ) {
            pagesTransformationStage = { root ->
                placesToTest.forEach { place ->
                    try {
                        when (place) {
                            is OnOwnPage ->
                                root.firstMemberOfType<ContentPage> { it.name == place.name }.content
                                    .firstMemberOfType<ContentGroup> { it.dci.kind == ContentKind.Symbol }
                                    .assertNode { hasExactText(expectedSignature) }
                            is OnParentPage ->
                                root.firstMemberOfType<ContentPage> {
                                    place.pageType.isInstance(it) && (place.parentName.isNullOrBlank() || place.parentName == it.name)
                                }
                                    .content
                                    .firstMemberOfType<ContentGroup> {
                                        it.dci.kind == place.section && (place.selfName.isNullOrBlank() ||
                                                it.dci.dri.toString().contains(place.selfName))
                                    }
                                    .firstMemberOfType<ContentGroup> { it.dci.kind == ContentKind.Symbol }
                                    .assertNode { hasExactText(expectedSignature) }
                        }
                    } catch (e: Throwable) {
                        logger.warn("$testData") // Because gradle has serious problem rendering custom test names
                        throw e
                    }
                }
            }
        }
    }

}

sealed class Place
data class OnOwnPage(val name: String) : Place()
data class OnParentPage(
    val pageType: KClass<out ContentPage>,
    val section: Kind,
    val parentName: String? = null,
    val selfName: String? = null
) : Place()

data class TestData(
    val codeFragment: String,
    val expectedSignature: String,
    val placesToTest: Iterable<Place>
) {
    constructor(codeFragment: String, expectedSignature: String, vararg placesToTest: Place)
            : this(codeFragment, expectedSignature, placesToTest.asIterable())

    override fun toString() = "[code = \"$codeFragment\"]"
}

private fun forFunction(codeFragment: String, expectedSignature: String, functionName: String = "underTest") =
    TestData(
        codeFragment,
        expectedSignature,
        OnParentPage(PackagePageNode::class, ContentKind.Functions),
        OnOwnPage(functionName)
    )

private fun forMethod(
    codeFragment: String,
    expectedSignature: String,
    functionName: String = "underTest",
    className: String = "Testable"
) =
    TestData(
        codeFragment,
        expectedSignature,
        OnParentPage(ClasslikePageNode::class, ContentKind.Functions, className, functionName),
        OnOwnPage(functionName)
    )

private fun forProperty(codeFragment: String, expectedSignature: String) =
    TestData(codeFragment, expectedSignature, OnParentPage(PackagePageNode::class, ContentKind.Properties))

private fun forClassProperty(codeFragment: String, expectedSignature: String, className: String = "Testable") =
    TestData(codeFragment, expectedSignature, OnParentPage(ClasslikePageNode::class, ContentKind.Properties, className))

private fun forClass(codeFragment: String, expectedSignature: String, className: String = "Testable") =
    TestData(
        codeFragment,
        expectedSignature,
        OnParentPage(PackagePageNode::class, ContentKind.Classlikes),
        OnOwnPage(className)
    )
