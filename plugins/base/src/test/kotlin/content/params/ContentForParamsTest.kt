package content.params

import matchers.content.*
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
import utils.*

class ContentForParamsTest : AbstractCoreTest() {
    private val testConfiguration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                analysisPlatform = "jvm"
            }
        }
    }

    @Test
    fun `undocumented function`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            |
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(), "", "", emptySet(), "function", null, "abc" to ParamAttributes(
                                        emptyMap(),
                                        emptySet(),
                                        "String"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `undocumented parameter`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                group { pWrapped("comment to function") }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `undocumented parameter and other tags without function comment`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @author Kordyjan
            |  * @author Woolfy
            |  * @since 0.11
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                unnamedTag("Author") {
                                    comment {
                                        +"Kordyjan"
                                    }
                                    comment {
                                        +"Woolfy"
                                    }
                                }
                                unnamedTag("Since") { comment {  +"0.11" } }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `multiple authors`() {
        testInline(
            """
            |/src/main/java/sample/DocGenProcessor.java
            |package sample;
            |/**
            | * Annotation processor which visits all classes.
            | *
            | * @author googler1@google.com (Googler 1)
            | * @author googler2@google.com (Googler 2)
            | */
            | public class DocGenProcessor { }
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val classPage = module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
                classPage.content.assertNode {
                    group {
                        header { +"DocGenProcessor" }
                        platformHinted {
                            group {
                                skipAllNotMatching() //Signature
                            }
                            group {
                                group {
                                    group {
                                        +"Annotation processor which visits all classes."
                                    }
                                }
                            }
                            group {
                                header(4) { +"Author" }
                                comment { +"googler1@google.com (Googler 1)" }
                                comment { +"googler2@google.com (Googler 2)" }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `author delimetered by space`() {
        testInline(
            """
            |/src/main/java/sample/DocGenProcessor.java
            |package sample;
            |/**
            | * Annotation processor which visits all classes.
            | *
            | * @author Marcin Aman Senior
            | */
            | public class DocGenProcessor { }
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val classPage = module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
                classPage.content.assertNode {
                    group {
                        header { +"DocGenProcessor" }
                        platformHinted {
                            group {
                                skipAllNotMatching() //Signature
                            }
                            group {
                                group {
                                    group {
                                        +"Annotation processor which visits all classes."
                                    }
                                }
                            }
                            group {
                                header(4) { +"Author" }
                                comment { +"Marcin Aman Senior" }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `deprecated with multiple links inside`() {
        testInline(
            """
            |/src/main/java/sample/DocGenProcessor.java
            |package sample;
            |/**
            | * Return the target fragment set by {@link #setTargetFragment}.
            | *
            | * @deprecated Instead of using a target fragment to pass results, the fragment requesting a
            | *              result should use
            | * {@link java.util.HashMap#containsKey(java.lang.Object) FragmentManager#setFragmentResult(String, Bundle)} to deliver results to
            | * {@link java.util.HashMap#containsKey(java.lang.Object) FragmentResultListener} instances registered by other fragments via
            | * {@link java.util.HashMap#containsKey(java.lang.Object) FragmentManager#setFragmentResultListener(String, LifecycleOwner,
            | * FragmentResultListener)}.
            | */
            | public class DocGenProcessor { 
            |    public String setTargetFragment(){
            |            return "";
            |    }
            |}
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val classPage = module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
                classPage.content.assertNode {
                    group {
                        header { +"DocGenProcessor" }
                        platformHinted {
                            group {
                                skipAllNotMatching() //Signature
                            }
                            group {
                                comment {
                                    +"Return the target fragment set by "
                                    link { +"setTargetFragment" }
                                    +"."
                                }
                            }
                            group {
                                header(4) { +"Deprecated" }
                                comment {
                                    +"Instead of using a target fragment to pass results, the fragment requesting a result should use "
                                    link { +"FragmentManager#setFragmentResult(String, Bundle)" }
                                    +" to deliver results to "
                                    link { +"FragmentResultListener" }
                                    +" instances registered by other fragments via "
                                    link { +"FragmentManager#setFragmentResultListener(String, LifecycleOwner, FragmentResultListener)" }
                                    +"."
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `deprecated with an html link in multiple lines`() {
        testInline(
            """
            |/src/main/java/sample/DocGenProcessor.java
            |package sample;
            |/**
            | * @deprecated Use
            | * <a href="https://developer.android.com/guide/navigation/navigation-swipe-view ">
            | *    TabLayout and ViewPager</a> instead.
            | */
            | public class DocGenProcessor { }
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val classPage = module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
                classPage.content.assertNode {
                    group {
                        header { +"DocGenProcessor" }
                        platformHinted {
                            group {
                                skipAllNotMatching() //Signature
                            }
                            group {
                                header(4) { +"Deprecated" }
                                comment {
                                    +"Use "
                                    link { +"TabLayout and ViewPager" }
                                    +" instead."
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }

    @Test
    fun `deprecated with an multiple inline links`() {
        testInline(
            """
            |/src/main/java/sample/DocGenProcessor.java
            |package sample;
            |/**
            | * FragmentManagerNonConfig stores the retained instance fragments across
            | * activity recreation events.
            | *
            | * <p>Apps should treat objects of this type as opaque, returned by
            | * and passed to the state save and restore process for fragments in
            | * {@link java.util.HashMap#containsKey(java.lang.Object) FragmentController#retainNestedNonConfig()} and
            | * {@link java.util.HashMap#containsKey(java.lang.Object) FragmentController#restoreAllState(Parcelable, FragmentManagerNonConfig)}.</p>
            | *
            | * @deprecated Have your {@link java.util.HashMap FragmentHostCallback} implement
            | * {@link java.util.HashMap } to automatically retain the Fragment's
            | * non configuration state.
            | */
            | public class DocGenProcessor { }
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val classPage = module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
                classPage.content.assertNode {
                    group {
                        header { +"DocGenProcessor" }
                        platformHinted {
                            group {
                                skipAllNotMatching() //Signature
                            }
                            group {
                                comment {
                                    group {
                                        +"FragmentManagerNonConfig stores the retained instance fragments across activity recreation events."
                                    }
                                    group {
                                        +"Apps should treat objects of this type as opaque, returned by and passed to the state save and restore process for fragments in "
                                        link { +"FragmentController#retainNestedNonConfig()" }
                                        +" and "
                                        link { +"FragmentController#restoreAllState(Parcelable, FragmentManagerNonConfig)"}
                                        +"."
                                    }
                                }
                            }
                            group {
                                header(4) { +"Deprecated" }
                                comment {
                                    +"Have your "
                                    link { +"FragmentHostCallback" }
                                    +" implement "
                                    link { +"java.util.HashMap" }
                                    +" to automatically retain the Fragment's non configuration state."
                                }
                            }
                        }
                    }
                    skipAllNotMatching()
                }
            }
        }
    }



    @Test
    fun `undocumented parameter and other tags`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @author Kordyjan
            |  * @since 0.11
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                group { pWrapped("comment to function") }
                                unnamedTag("Author") { comment { +"Kordyjan" } }
                                unnamedTag("Since") { comment { +"0.11" } }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `single parameter`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @param abc comment to param
            |  */
            |fun function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                group { pWrapped("comment to function") }
                                header(2) { +"Parameters" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                +"abc"
                                                group { group { +"comment to param" } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `multiple parameters`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @param first comment to first param
            |  * @param second comment to second param
            |  * @param[third] comment to third param
            |  */
            |fun function(first: String, second: Int, third: Double) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(), "", "", emptySet(), "function", null,
                                    "first" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    "second" to ParamAttributes(emptyMap(), emptySet(), "Int"),
                                    "third" to ParamAttributes(emptyMap(), emptySet(), "Double")
                                )
                            }
                            after {
                                group { group { group { +"comment to function" } } }
                                header(2) { +"Parameters" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                +"first"
                                                group { group { +"comment to first param" } }
                                            }
                                            group {
                                                +"second"
                                                group { group { +"comment to second param" } }
                                            }
                                            group {
                                                +"third"
                                                group { group { +"comment to third param" } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `multiple parameters without function description`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * @param first comment to first param
            |  * @param second comment to second param
            |  * @param[third] comment to third param
            |  */
            |fun function(first: String, second: Int, third: Double) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(), "", "", emptySet(), "function", null,
                                    "first" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    "second" to ParamAttributes(emptyMap(), emptySet(), "Int"),
                                    "third" to ParamAttributes(emptyMap(), emptySet(), "Double")
                                )
                            }
                            after {
                                header(2) { +"Parameters" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                +"first"
                                                group { group { +"comment to first param" } }
                                            }
                                            group {
                                                +"second"
                                                group { group { +"comment to second param" } }
                                            }
                                            group {
                                                +"third"
                                                group { group { +"comment to third param" } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `function with receiver`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @param abc comment to param
            |  * @receiver comment to receiver
            |  */
            |fun String.function(abc: String) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignatureWithReceiver(
                                    emptyMap(),
                                    "",
                                    "",
                                    emptySet(),
                                    "String",
                                    "function",
                                    null,
                                    "abc" to ParamAttributes(emptyMap(), emptySet(), "String")
                                )
                            }
                            after {
                                group { pWrapped("comment to function") }
                                header(2) { +"Parameters" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                +"<receiver>"
                                                group { group { +"comment to receiver" } }
                                            }
                                            group {
                                                +"abc"
                                                group { group { +"comment to param" } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `missing parameter documentation`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @param first comment to first param
            |  * @param[third] comment to third param
            |  */
            |fun function(first: String, second: Int, third: Double) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(), "", "", emptySet(), "function", null,
                                    "first" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    "second" to ParamAttributes(emptyMap(), emptySet(), "Int"),
                                    "third" to ParamAttributes(emptyMap(), emptySet(), "Double")
                                )
                            }
                            after {
                                group { group { group { +"comment to function" } } }
                                header(2) { +"Parameters" }
                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                +"first"
                                                group { group { +"comment to first param" } }
                                            }
                                            group {
                                                +"third"
                                                group { group { +"comment to third param" } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `parameters mixed with other tags`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @param first comment to first param
            |  * @author Kordyjan
            |  * @param second comment to second param
            |  * @since 0.11
            |  * @param[third] comment to third param
            |  */
            |fun function(first: String, second: Int, third: Double) {
            |    println(abc)
            |}
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val page = module.children.single { it.name == "test" }
                    .children.single { it.name == "function" } as ContentPage
                page.content.assertNode {
                    group {
                        header(1) { +"function" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                bareSignature(
                                    emptyMap(), "", "", emptySet(), "function", null,
                                    "first" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    "second" to ParamAttributes(emptyMap(), emptySet(), "Int"),
                                    "third" to ParamAttributes(emptyMap(), emptySet(), "Double")
                                )
                            }
                            after {
                                group { pWrapped("comment to function") }
                                unnamedTag("Author") { comment {  +"Kordyjan" } }
                                unnamedTag("Since") { comment {  +"0.11" } }
                                header(2) { +"Parameters" }

                                group {
                                    platformHinted {
                                        table {
                                            group {
                                                +"first"
                                                group { group { +"comment to first param" } }
                                            }
                                            group {
                                                +"second"
                                                group { group { +"comment to second param" } }
                                            }
                                            group {
                                                +"third"
                                                group { group { +"comment to third param" } }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun javaDocCommentWithDocumentedParameters() {
        testInline(
            """
            |/src/main/java/test/Main.java
            |package test
            | public class Main {
            |
            | /**
            |  * comment to function
            |  * @param first comment to first param
            |  * @param second comment to second param
            |  */
            |   public void sample(String first, String second) {
            |  
            |   }
            | }
        """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val sampleFunction = module.dfs {
                    it is MemberPageNode && it.dri.first()
                        .toString() == "test/Main/sample/#java.lang.String#java.lang.String/PointingToDeclaration/"
                } as MemberPageNode
                val forJvm = (sampleFunction.documentable as DFunction).parameters.mapNotNull {
                    val jvm = it.documentation.keys.first { it.analysisPlatform == Platform.jvm }
                    it.documentation[jvm]
                }

                assert(forJvm.size == 2)
                val (first, second) = forJvm.map { it.paramsDescription() }
                assert(first == "comment to first param")
                assert(second == "comment to second param")
            }
        }
    }

    private fun DocumentationNode.paramsDescription(): String =
        children.firstIsInstanceOrNull<Param>()?.root?.children?.first()?.children?.firstIsInstanceOrNull<Text>()?.body.orEmpty()

}
