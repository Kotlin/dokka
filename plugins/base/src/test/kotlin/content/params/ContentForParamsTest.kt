package content.params

import matchers.content.*
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Text
import org.jetbrains.dokka.pages.ContentDRILink
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.MemberPageNode
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
import utils.*
import kotlin.test.assertEquals

class ContentForParamsTest : BaseAbstractTest() {
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
                                unnamedTag("Since") { comment { +"0.11" } }
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
                val classPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
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
                val classPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
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
            | * Return the target fragment set by {@link #setTargetFragment} or {@link
            | * #setTargetFragment}.
            | *
            | * @deprecated Instead of using a target fragment to pass results, the fragment requesting a
            | *              result should use
            | * {@link java.util.HashMap#containsKey(java.lang.Object) FragmentManager#setFragmentResult(String, Bundle)} to deliver results to
            | * {@link java.util.HashMap#containsKey(java.lang.Object)
            | * FragmentResultListener} instances registered by other fragments via
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
                val classPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
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
                                    +" or "
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
                val classPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
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
                val classPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
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
                                        +"FragmentManagerNonConfig stores the retained instance fragments across activity recreation events. "
                                    }
                                    group {
                                        +"Apps should treat objects of this type as opaque, returned by and passed to the state save and restore process for fragments in "
                                        link { +"FragmentController#retainNestedNonConfig()" }
                                        +" and "
                                        link { +"FragmentController#restoreAllState(Parcelable, FragmentManagerNonConfig)" }
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
    fun `multiline throws with comment`() {
        testInline(
            """
            |/src/main/java/sample/DocGenProcessor.java
            |package sample;
            | public class DocGenProcessor {
            | /**
            | * a normal comment
            | *
            | * @throws java.lang.IllegalStateException if the Dialog has not yet been created (before
            | * onCreateDialog) or has been destroyed (after onDestroyView).
            | * @throws java.lang.RuntimeException when {@link java.util.HashMap#containsKey(java.lang.Object) Hash
            | *      Map} doesn't contain value.
            | */
            | public static void sample(){ }
            |}
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" }.children.single { it.name == "sample" } as ContentPage
                functionPage.content.assertNode {
                    group {
                        header(1) { +"sample" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                skipAllNotMatching() //Signature
                            }
                            after {
                                group { pWrapped("a normal comment") }
                                header(2) { +"Throws" }
                                table {
                                    group {
                                        group {
                                            link { +"java.lang.IllegalStateException" }
                                        }
                                        comment { +"if the Dialog has not yet been created (before onCreateDialog) or has been destroyed (after onDestroyView)." }
                                    }
                                    group {
                                        group {
                                            link { +"java.lang.RuntimeException" }
                                        }
                                        comment {
                                            +"when "
                                            link { +"Hash Map" }
                                            +" doesn't contain value."
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
    fun `multiline kotlin throws with comment`() {
        testInline(
            """
            |/src/main/kotlin/sample/sample.kt
            |package sample;
            | /**
            | * a normal comment
            | *
            | * @throws java.lang.IllegalStateException if the Dialog has not yet been created (before
            | * onCreateDialog) or has been destroyed (after onDestroyView).
            | * @exception RuntimeException when [Hash Map][java.util.HashMap.containsKey] doesn't contain value.
            | */
            | fun sample(){ }
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "sample" } as ContentPage
                functionPage.content.assertNode {
                    group {
                        header(1) { +"sample" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                skipAllNotMatching() //Signature
                            }
                            after {
                                group { pWrapped("a normal comment") }
                                header(2) { +"Throws" }
                                table {
                                    group {
                                        group {
                                            link {
                                                check {
                                                    assertEquals(
                                                        "java.lang/IllegalStateException///PointingToDeclaration/",
                                                        (this as ContentDRILink).address.toString()
                                                    )
                                                }
                                                +"java.lang.IllegalStateException"
                                            }
                                        }
                                        comment { +"if the Dialog has not yet been created (before onCreateDialog) or has been destroyed (after onDestroyView)." }
                                    }
                                    group {
                                        group {
                                            link {
                                                check {
                                                    assertEquals(
                                                        "java.lang/RuntimeException///PointingToDeclaration/",
                                                        (this as ContentDRILink).address.toString()
                                                    )
                                                }
                                                +"java.lang.RuntimeException"
                                            }
                                        }
                                        comment {
                                            +"when "
                                            link { +"Hash Map" }
                                            +" doesn't contain value."
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
    fun `multiline throws where exception is not in the same line as description`() {
        testInline(
            """
            |/src/main/java/sample/DocGenProcessor.java
            |package sample;
            | public class DocGenProcessor {
            | /**
            | * a normal comment
            | *
            | * @throws java.lang.IllegalStateException if the Dialog has not yet been created (before
            | * onCreateDialog) or has been destroyed (after onDestroyView).
            | * @throws java.lang.RuntimeException when
            | * {@link java.util.HashMap#containsKey(java.lang.Object) Hash
            | *      Map}
            | * doesn't contain value.
            | */
            | public static void sample(){ }
            |}
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" }.children.single { it.name == "sample" } as ContentPage
                functionPage.content.assertNode {
                    group {
                        header(1) { +"sample" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                skipAllNotMatching() //Signature
                            }
                            after {
                                group { pWrapped("a normal comment") }
                                header(2) { +"Throws" }
                                table {
                                    group {
                                        group {
                                            link {
                                                check {
                                                    assertEquals(
                                                        "java.lang/IllegalStateException///PointingToDeclaration/",
                                                        (this as ContentDRILink).address.toString()
                                                    )
                                                }
                                                +"java.lang.IllegalStateException"
                                            }
                                        }
                                        comment { +"if the Dialog has not yet been created (before onCreateDialog) or has been destroyed (after onDestroyView)." }
                                    }
                                    group {
                                        group {
                                            link {
                                                check {
                                                    assertEquals(
                                                        "java.lang/RuntimeException///PointingToDeclaration/",
                                                        (this as ContentDRILink).address.toString()
                                                    )
                                                }
                                                +"java.lang.RuntimeException"
                                            }
                                        }
                                        comment {
                                            +"when "
                                            link { +"Hash Map" }
                                            +" doesn't contain value."
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
    fun `documentation splitted in 2 using enters`() {
        testInline(
            """
            |/src/main/java/sample/DocGenProcessor.java
            |package sample;
            |/**
            | * Listener for handling fragment results.
            | *
            | * This object should be passed to
            | * {@link java.util.HashMap#containsKey(java.lang.Object) FragmentManager#setFragmentResultListener(String, LifecycleOwner, FragmentResultListener)}
            | *                  and it will listen for results with the same key that are passed into
            | * {@link java.util.HashMap#containsKey(java.lang.Object) FragmentManager#setFragmentResult(String, Bundle)}.
            | *
            | */
            | public class DocGenProcessor { }
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val classPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
                classPage.content.assertNode {
                    group {
                        header { +"DocGenProcessor" }
                        platformHinted {
                            group {
                                skipAllNotMatching() //Signature
                            }
                            group {
                                comment {
                                    +"Listener for handling fragment results. This object should be passed to "
                                    link { +"FragmentManager#setFragmentResultListener(String, LifecycleOwner, FragmentResultListener)" }
                                    +" and it will listen for results with the same key that are passed into "
                                    link { +"FragmentManager#setFragmentResult(String, Bundle)" }
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
    fun `multiline return tag with param`() {
        testInline(
            """
            |/src/main/java/sample/DocGenProcessor.java
            |package sample;
            | public class DocGenProcessor {
            | /**
            | * a normal comment
            | *
            | * @param testParam Sample description for test param that has a type of {@link java.lang.String String}
            | * @return empty string when
            | * {@link java.util.HashMap#containsKey(java.lang.Object) Hash
            | *      Map}
            | * doesn't contain value.
            | */
            | public static String sample(String testParam){
            |   return "";
            | }
            |}
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" }.children.single { it.name == "sample" } as ContentPage
                functionPage.content.assertNode {
                    group {
                        header(1) { +"sample" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                skipAllNotMatching() //Signature
                            }
                            after {
                                group { pWrapped("a normal comment") }
                                group {
                                    header(4) { +"Return" }
                                    comment {
                                        +"empty string when "
                                        link { +"Hash Map" }
                                        +" doesn't contain value."
                                    }
                                }
                                header(2) { +"Parameters" }
                                group {
                                    table {
                                        group {
                                            +"testParam"
                                            comment {
                                                +"Sample description for test param that has a type of "
                                                link { +"String" }
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
    fun `return tag in kotlin`() {
        testInline(
            """
            |/src/main/kotlin/sample/sample.kt
            |package sample;
            | /**
            | * a normal comment
            | *
            | * @return empty string when [Hash Map](java.util.HashMap.containsKey) doesn't contain value.
            | *
            | */
            |fun sample(): String {
            |   return ""
            | }
            |}
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val functionPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "sample" } as ContentPage
                functionPage.content.assertNode {
                    group {
                        header(1) { +"sample" }
                    }
                    divergentGroup {
                        divergentInstance {
                            divergent {
                                skipAllNotMatching() //Signature
                            }
                            after {
                                group { pWrapped("a normal comment") }
                                group {
                                    header(4) { +"Return" }
                                    comment {
                                        +"empty string when "
                                        link { +"Hash Map" }
                                        +" doesn't contain value."
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
    fun `list with links and description`() {
        testInline(
            """
            |/src/main/java/sample/DocGenProcessor.java
            |package sample;
            |/**
            | * Static library support version of the framework's {@link java.lang.String}.
            | * Used to write apps that run on platforms prior to Android 3.0. When running
            | * on Android 3.0 or above, this implementation is still used; it does not try
            | * to switch to the framework's implementation. See the framework {@link java.lang.String}
            | * documentation for a class overview.
            | *
            | * <p>The main differences when using this support version instead of the framework version are:
            | * <ul>
            | *  <li>Your activity must extend {@link java.lang.String FragmentActivity}
            | *  <li>You must call {@link java.util.HashMap#containsKey(java.lang.Object) FragmentActivity#getSupportFragmentManager} to get the
            | *  {@link java.util.HashMap FragmentManager}
            | * </ul>
            | *
            | */
            |public class DocGenProcessor { }
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val classPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
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
                                        +"Static library support version of the framework's "
                                        link { +"java.lang.String" }
                                        +". Used to write apps that run on platforms prior to Android 3.0."
                                        +" When running on Android 3.0 or above, this implementation is still used; it does not try to switch to the framework's implementation. See the framework "
                                        link { +"java.lang.String" }
                                        +" documentation for a class overview. " //TODO this probably shouldnt have a space but it is minor
                                    }
                                    group {
                                        +"The main differences when using this support version instead of the framework version are: "
                                    }
                                    list {
                                        group {
                                            +"Your activity must extend "
                                            link { +"FragmentActivity" }
                                        }
                                        group {
                                            +"You must call "
                                            link { +"FragmentActivity#getSupportFragmentManager" }
                                            +" to get the "
                                            link { +"FragmentManager" }
                                        }
                                    }
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
    fun `documentation with table`() {
        testInline(
            """
            |/src/main/java/sample/DocGenProcessor.java
            |package sample;
            |/**
            | * <table>
            | *     <caption>List of supported types</caption>
            | * <tr>
            | *   <td>cell 11</td> <td>cell 21</td>
            | * </tr>
            | * <tr>
            | *  <td>cell 12</td> <td>cell 22</td>
            | * </tr>
            | * </table>
            | */
            | public class DocGenProcessor { }
            """.trimIndent(), testConfiguration
        ) {
            pagesTransformationStage = { module ->
                val classPage =
                    module.children.single { it.name == "sample" }.children.single { it.name == "DocGenProcessor" } as ContentPage
                classPage.content.assertNode {
                    group {
                        header { +"DocGenProcessor" }
                        platformHinted {
                            group {
                                skipAllNotMatching() //Signature
                            }
                            comment {
                                table {
                                    check {
                                        caption!!.assertNode {
                                            caption {
                                                +"List of supported types"
                                            }
                                        }
                                    }
                                    group {
                                        group {
                                            +"cell 11"
                                        }
                                        group {
                                            +"cell 21"
                                        }
                                    }
                                    group {
                                        group {
                                            +"cell 12"
                                        }
                                        group {
                                            +"cell 22"
                                        }
                                    }
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


    @Test
    fun `multiple parameters with not natural order`() {
        testInline(
            """
            |/src/main/kotlin/test/source.kt
            |package test
            | /**
            |  * comment to function
            |  * @param c comment to c param
            |  * @param b comment to b param
            |  * @param[a] comment to a param
            |  */
            |fun function(c: String, b: Int, a: Double) {
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
                                    "c" to ParamAttributes(emptyMap(), emptySet(), "String"),
                                    "b" to ParamAttributes(emptyMap(), emptySet(), "Int"),
                                    "a" to ParamAttributes(emptyMap(), emptySet(), "Double")
                                )
                            }
                            after {
                                group { group { group { +"comment to function" } } }
                                header(2) { +"Parameters" }
                                group {
                                    table {
                                        group {
                                            +"c"
                                            group { group { +"comment to c param" } }
                                        }
                                        group {
                                            +"b"
                                            group { group { +"comment to b param" } }
                                        }
                                        group {
                                            +"a"
                                            group { group { +"comment to a param" } }
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
                                group {
                                    header(4) { +"Receiver" }
                                    pWrapped("comment to receiver")
                                }
                                header(2) { +"Parameters" }
                                group {
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
                                unnamedTag("Author") { comment { +"Kordyjan" } }
                                unnamedTag("Since") { comment { +"0.11" } }
                                header(2) { +"Parameters" }

                                group {
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
                val forJvm = (sampleFunction.documentables.firstOrNull() as DFunction).parameters.mapNotNull {
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
