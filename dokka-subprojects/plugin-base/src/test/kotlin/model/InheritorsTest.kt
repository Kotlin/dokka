/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package model

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.analysis.kotlin.markdown.MARKDOWN_ELEMENT_FILE_NAME
import org.jetbrains.dokka.base.transformers.documentables.InheritorsInfo
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.CustomDocTag
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.P
import org.jetbrains.dokka.model.doc.Text
import utils.AbstractModelTest
import utils.assertNotNull
import utils.comments
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InheritorsTest : AbstractModelTest("/src/main/kotlin/inheritors/Test.kt", "inheritors") {

    val configuration = dokkaConfiguration {
        suppressObviousFunctions = false
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/main/kotlin")
            }
        }
    }

    @Test
    fun simple() {
        inlineModelTest(
            """|interface A{}
               |class B() : A {}
            """.trimMargin(),
        ) {
            with((this / "inheritors" / "A").cast<DInterface>()) {
                val map = extra[InheritorsInfo].assertNotNull("InheritorsInfo").value
                with(map.keys.also { it counts 1 }.find { it.analysisPlatform == Platform.jvm }.assertNotNull("jvm key").let { map[it]!! }
                ) {
                    this counts 1
                    first().classNames equals "B"
                }
            }
        }
    }

    @Test
    fun sealed() {
        inlineModelTest(
            """|sealed class A {}
               |class B() : A() {}
               |class C() : A() {}
               |class D()
            """.trimMargin(),
        ) {
            with((this / "inheritors" / "A").cast<DClass>()) {
                val map = extra[InheritorsInfo].assertNotNull("InheritorsInfo").value
                with(map.keys.also { it counts 1 }.find { it.analysisPlatform == Platform.jvm }.assertNotNull("jvm key").let { map[it]!! }
                ) {
                    this counts 2
                    mapNotNull { it.classNames }.sorted() equals listOf("B", "C")
                }
            }
        }
    }

    @Test
    fun multiplatform() {
        val configuration = dokkaConfiguration {
            sourceSets {
                val commonSourceSet = sourceSet {
                    name = "common"
                    sourceRoots = listOf("common/src/")
                    analysisPlatform = "common"
                }
                sourceSet {
                    name = "jvm"
                    sourceRoots = listOf("jvm/src/")
                    analysisPlatform = "jvm"
                    dependentSourceSets =  setOf(commonSourceSet.value.sourceSetID)
                }
                sourceSet {
                    name = "js"
                    sourceRoots = listOf("js/src/")
                    analysisPlatform = "js"
                    dependentSourceSets =  setOf(commonSourceSet.value.sourceSetID)
                }
            }
        }

        testInline(
            """
            |/common/src/main/kotlin/inheritors/Test.kt
            |package inheritors
            |interface A{}
            |/jvm/src/main/kotlin/inheritors/Test.kt
            |package inheritors
            |class B() : A {}
            |/js/src/main/kotlin/inheritors/Test.kt
            |package inheritors
            |class B() : A {}
            |class C() : A {}
        """.trimMargin(),
            configuration,
            cleanupOutput = false,
        ) {
            documentablesTransformationStage = { m ->
                with((m / "inheritors" / "A").cast<DInterface>()) {
                    val map = extra[InheritorsInfo].assertNotNull("InheritorsInfo").value
                    with(map.keys.also { it counts 2 }) {
                        with(find { it.analysisPlatform == Platform.jvm }.assertNotNull("jvm key").let { map[it]!! }) {
                            this counts 1
                            first().classNames equals "B"
                        }
                        with(find { it.analysisPlatform == Platform.js }.assertNotNull("js key").let { map[it]!! }) {
                            this counts 2
                            val classes = listOf("B", "C")
                            assertTrue(all { classes.contains(it.classNames) }, "One of subclasses missing in js" )
                        }
                    }

                }
            }
        }
    }

    @Test
    fun `should inherit docs`() {
        val expectedDoc = listOf(P(listOf(Text("some text"))))
        inlineModelTest(
            """|interface A<out E> {
               | /**
               | * some text
               | */
               | val a: Int
               | 
               | /**
               | * some text
               | */
               | fun b(): E
               |}
               |open class C
               |class B<out E>() : C(), A<out E> {
               | val a = 0
               | override fun b(): E {}
               |}
            """.trimMargin(),
            platform = Platform.common.toString()
        ) {
            with((this / "inheritors" / "A").cast<DInterface>()) {
                with(this / "a") {
                    val propDoc = this?.documentation?.values?.single()?.children?.first()?.children
                    propDoc equals expectedDoc
                }
                with(this / "b") {
                    val funDoc = this?.documentation?.values?.single()?.children?.first()?.children
                    funDoc equals expectedDoc
                }

            }

            with((this / "inheritors" / "B").cast<DClass>()) {
                with(this / "a") {
                    val propDoc = this?.documentation?.values?.single()?.children?.first()?.children
                    propDoc equals expectedDoc
                }
            }
        }
    }

//     TODO [beresnev] fix, needs access to analysis
//    class IgnoreCommonBuiltInsPlugin : DokkaPlugin() {
//        private val kotlinAnalysisPlugin by lazy { plugin<DescriptorKotlinAnalysisPlugin>() }
//        @Suppress("unused")
//        val stdLibKotlinAnalysis by extending {
//            kotlinAnalysisPlugin.kotlinAnalysis providing { ctx ->
//                ProjectKotlinAnalysis(
//                    sourceSets = ctx.configuration.sourceSets,
//                    logger = ctx.logger,
//                    analysisConfiguration = DokkaAnalysisConfiguration(ignoreCommonBuiltIns = true)
//                )
//            } override kotlinAnalysisPlugin.defaultKotlinAnalysis
//        }
//
//        @OptIn(DokkaPluginApiPreview::class)
//        override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement =
//            PluginApiPreviewAcknowledgement
//    }
//    @Test
//    fun `should inherit docs for stdLib #2638`() {
//        val testConfiguration = dokkaConfiguration {
//            suppressObviousFunctions = false
//            sourceSets {
//                sourceSet {
//                    sourceRoots = listOf("src/")
//                    analysisPlatform = "common"
//                    languageVersion = "1.4"
//                }
//            }
//        }
//
//        inlineModelTest(
//            """
//            package kotlin.collections
//
//            import kotlin.internal.PlatformDependent
//
//            /**
//             * Classes that inherit from this interface can be represented as a sequence of elements that can
//             * be iterated over.
//             * @param T the type of element being iterated over. The iterator is covariant in its element type.
//             */
//            public interface Iterable<out T> {
//                /**
//                 * Returns an iterator over the elements of this object.
//                 */
//                public operator fun iterator(): Iterator<T>
//            }
//
//            /**
//             * Classes that inherit from this interface can be represented as a sequence of elements that can
//             * be iterated over and that supports removing elements during iteration.
//             * @param T the type of element being iterated over. The mutable iterator is invariant in its element type.
//             */
//            public interface MutableIterable<out T> : Iterable<T> {
//                /**
//                 * Returns an iterator over the elements of this sequence that supports removing elements during iteration.
//                 */
//                override fun iterator(): MutableIterator<T>
//            }
//
//            /**
//             * A generic collection of elements. Methods in this interface support only read-only access to the collection;
//             * read/write access is supported through the [MutableCollection] interface.
//             * @param E the type of elements contained in the collection. The collection is covariant in its element type.
//             */
//            public interface Collection<out E> : Iterable<E> {
//                // Query Operations
//                /**
//                 * Returns the size of the collection.
//                 */
//                public val size: Int
//
//                /**
//                 * Returns `true` if the collection is empty (contains no elements), `false` otherwise.
//                 */
//                public fun isEmpty(): Boolean
//
//                /**
//                 * Checks if the specified element is contained in this collection.
//                 */
//                public operator fun contains(element: @UnsafeVariance E): Boolean
//
//                override fun iterator(): Iterator<E>
//
//                // Bulk Operations
//                /**
//                 * Checks if all elements in the specified collection are contained in this collection.
//                 */
//                public fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
//            }
//
//            /**
//             * A generic collection of elements that supports adding and removing elements.
//             *
//             * @param E the type of elements contained in the collection. The mutable collection is invariant in its element type.
//             */
//            public interface MutableCollection<E> : Collection<E>, MutableIterable<E> {
//                // Query Operations
//                override fun iterator(): MutableIterator<E>
//
//                // Modification Operations
//                /**
//                 * Adds the specified element to the collection.
//                 *
//                 * @return `true` if the element has been added, `false` if the collection does not support duplicates
//                 * and the element is already contained in the collection.
//                 */
//                public fun add(element: E): Boolean
//
//                /**
//                 * Removes a single instance of the specified element from this
//                 * collection, if it is present.
//                 *
//                 * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
//                 */
//                public fun remove(element: E): Boolean
//
//                // Bulk Modification Operations
//                /**
//                 * Adds all of the elements of the specified collection to this collection.
//                 *
//                 * @return `true` if any of the specified elements was added to the collection, `false` if the collection was not modified.
//                 */
//                public fun addAll(elements: Collection<E>): Boolean
//
//                /**
//                 * Removes all of this collection's elements that are also contained in the specified collection.
//                 *
//                 * @return `true` if any of the specified elements was removed from the collection, `false` if the collection was not modified.
//                 */
//                public fun removeAll(elements: Collection<E>): Boolean
//
//                /**
//                 * Retains only the elements in this collection that are contained in the specified collection.
//                 *
//                 * @return `true` if any element was removed from the collection, `false` if the collection was not modified.
//                 */
//                public fun retainAll(elements: Collection<E>): Boolean
//
//                /**
//                 * Removes all elements from this collection.
//                 */
//                public fun clear(): Unit
//            }
//
//            /**
//             * A generic ordered collection of elements. Methods in this interface support only read-only access to the list;
//             * read/write access is supported through the [MutableList] interface.
//             * @param E the type of elements contained in the list. The list is covariant in its element type.
//             */
//            public interface List<out E> : Collection<E> {
//                // Query Operations
//
//                override val size: Int
//                override fun isEmpty(): Boolean
//                override fun contains(element: @UnsafeVariance E): Boolean
//                override fun iterator(): Iterator<E>
//
//                // Bulk Operations
//                override fun containsAll(elements: Collection<@UnsafeVariance E>): Boolean
//
//                // Positional Access Operations
//                /**
//                 * Returns the element at the specified index in the list.
//                 */
//                public operator fun get(index: Int): E
//
//                // Search Operations
//                /**
//                 * Returns the index of the first occurrence of the specified element in the list, or -1 if the specified
//                 * element is not contained in the list.
//                 */
//                public fun indexOf(element: @UnsafeVariance E): Int
//
//                /**
//                 * Returns the index of the last occurrence of the specified element in the list, or -1 if the specified
//                 * element is not contained in the list.
//                 */
//                public fun lastIndexOf(element: @UnsafeVariance E): Int
//
//                // List Iterators
//                /**
//                 * Returns a list iterator over the elements in this list (in proper sequence).
//                 */
//                public fun listIterator(): ListIterator<E>
//
//                /**
//                 * Returns a list iterator over the elements in this list (in proper sequence), starting at the specified [index].
//                 */
//                public fun listIterator(index: Int): ListIterator<E>
//
//                // View
//                /**
//                 * Returns a view of the portion of this list between the specified [fromIndex] (inclusive) and [toIndex] (exclusive).
//                 * The returned list is backed by this list, so non-structural changes in the returned list are reflected in this list, and vice-versa.
//                 *
//                 * Structural changes in the base list make the behavior of the view undefined.
//                 */
//                public fun subList(fromIndex: Int, toIndex: Int): List<E>
//            }
//
//            // etc
//            """.trimMargin(),
//            platform = Platform.common.toString(),
//            configuration = testConfiguration,
//            prependPackage = false,
//            pluginsOverrides = listOf(IgnoreCommonBuiltInsPlugin())
//        ) {
//            with((this / "kotlin.collections" / "List" / "contains").cast<DFunction>()) {
//                documentation.size equals 1
//
//            }
//        }
//    }

    @Test
    fun `should inherit docs in case of diamond inheritance`() {
        inlineModelTest(
            """
            public interface Collection2<out E>  {
                /**
                 * Returns `true` if the collection is empty (contains no elements), `false` otherwise.
                 */
                public fun isEmpty(): Boolean
            
                /**
                 * Checks if the specified element is contained in this collection.
                 */
                public operator fun contains(element: @UnsafeVariance E): Boolean
            }
            
            public interface MutableCollection2<E> : Collection2<E>, MutableIterable2<E> 
            

            public interface List2<out E> : Collection2<E> {
                override fun isEmpty(): Boolean
                override fun contains(element: @UnsafeVariance E): Boolean
            }
            
            public interface MutableList2<E> : List2<E>, MutableCollection2<E>
            
            public class AbstractMutableList2<E> : MutableList2<E> {
                protected constructor()
            
                // From List
            
                override fun isEmpty(): Boolean = size == 0
                public override fun contains(element: E): Boolean = indexOf(element) != -1
            }
            public class ArrayDeque2<E> : AbstractMutableList2<E> {
                override fun isEmpty(): Boolean = size == 0
                public override fun contains(element: E): Boolean = indexOf(element) != -1
            
            }
            """.trimMargin()
        ) {
            with((this / "inheritors" / "ArrayDeque2" / "isEmpty").cast<DFunction>()) {
                documentation.size equals 1
            }
            with((this / "inheritors" / "ArrayDeque2" / "contains").cast<DFunction>()) {
                documentation.size equals 1
            }
        }
    }

    @Test
    fun `java nested classes should not be inherited`() {
        testInline(
            """
            |/src/main/kotlin/sample/ParentInKotlin.kt
            |package sample
            |class Child: JavaParent()
            |
            |/src/main/kotlin/sample/ChildInJava.java
            |package sample;
            |public class JavaParent {
            |    public class InnerJavaParent{}
            |    public static class NestedJavaParent{}
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val childClass = module.packages.flatMap { it.classlikes }
                    .find { it.name == "Child" } as DClass
                assertEquals(emptyList(), childClass.classlikes)

                val javaParent = module.packages.flatMap { it.classlikes }
                    .find { it.name == "JavaParent" } as DClass
                assertEquals(2, javaParent.classlikes.size)
            }
        }
    }
    @Test
    fun `kotlin nested classes should not be inherited`() {
        testInline(
            """
            |/src/main/kotlin/sample/ParentInKotlin.kt
            |package sample
            |class KotlinChild: KotlinParent() 
            |open class KotlinParent {
            | class NestedClass
            | inner class InnerClass
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val childKotlinClass = module.packages.flatMap { it.classlikes }
                    .find { it.name == "KotlinChild" } as DClass
                assertEquals(emptyList(), childKotlinClass.classlikes)
                val kotlinParent = module.packages.flatMap { it.classlikes }
                    .find { it.name == "KotlinParent" } as DClass
                assertEquals(2, kotlinParent.classlikes.size)
            }
        }
    }

    @Test
    fun `nested classes should not be inherited in an enum entry`() {
        testInline(
            """
            |/src/main/kotlin/sample/ParentInKotlin.kt
            |package sample
            |enum class A {
            |   E;
            |   class NestedClass
            |   inner class InnerClass
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = { module ->
                val enumEntry = module
                    .dfs { it.name == "E" } as DEnumEntry
                assertEquals(emptyList(), enumEntry.classlikes)
                val enumClass = module
                    .dfs { it.name == "A" } as DEnum
                assertEquals(2, enumClass.classlikes.size)
            }
        }
    }

    @Test
    fun `DRI of generic inherited members (fake override) should lead to super member`() {
        inlineModelTest(
            """
                |interface A<T> { fun x(): T }
                |interface B : A<Int> { }
                """
        ) {
            with((this / "inheritors" / "A" / "x").cast<DFunction>()) {
                dri.classNames equals "A"
                dri.packageName equals "inheritors"
            }
            with((this / "inheritors" / "B" / "x").cast<DFunction>()) {
                dri.classNames equals "A"
                dri.packageName equals "inheritors"
            }
        }
    }
    @Test
    fun `DRI of inherited members should lead to super member`() {
        inlineModelTest(
            """
                |interface A { fun x() = 0 }
                |interface B : A { }
                """
        ) {
            with((this / "inheritors" / "A" / "x").cast<DFunction>()) {
                dri.classNames equals "A"
                dri.packageName equals "inheritors"
            }
            with((this / "inheritors" / "B" / "x").cast<DFunction>()) {
                dri.classNames equals "A"
                dri.packageName equals "inheritors"
            }
        }
    }
    @Test
    fun `DRI of generic override members should lead to themself`() {
        inlineModelTest(
            """
                |open class A<T> { open fun x(p: T):T  { return p } }
                |class B : A<Int>() { override fun x(p: Int): Int = 0 }
                """
        ) {
            with((this / "inheritors" / "A" / "x").cast<DFunction>()) {
                dri.classNames equals "A"
                dri.packageName equals "inheritors"
            }
            with((this / "inheritors" / "B" / "x").cast<DFunction>()) {
                dri.classNames equals "B"
                dri.packageName equals "inheritors"
            }
        }
    }

    @Test
    fun `substitution override fun and prop (fake override) for override declaration should have the override keyword`() {
        inlineModelTest(
            """
               class A<T>  : C<T>()

                open class C<T> : D<T> {
                    override fun f(a: T) = 1
                    override val p: T? = null
                }
                
                interface D<T> {
                    fun f(a: T) = 0
                    val p: T? = null
                }
            """.trimMargin()
        ) {
            with((this / "inheritors" / "A"/ "f").cast<DFunction>()) {
                name equals "f"
                extra[AdditionalModifiers]?.content?.values?.firstOrNull()
                    ?.firstOrNull() equals ExtraModifiers.KotlinOnlyModifiers.Override
            }
            with((this / "inheritors" / "C"/ "f").cast<DFunction>()) {
                name equals "f"
                extra[AdditionalModifiers]?.content?.values?.firstOrNull()
                    ?.firstOrNull() equals ExtraModifiers.KotlinOnlyModifiers.Override
            }

            with((this / "inheritors" / "A"/ "p").cast<DProperty>()) {
                name equals "p"
                extra[AdditionalModifiers]?.content?.values?.firstOrNull()
                    ?.firstOrNull() equals ExtraModifiers.KotlinOnlyModifiers.Override
            }
            with((this / "inheritors" / "C"/ "p").cast<DProperty>()) {
                name equals "p"
                extra[AdditionalModifiers]?.content?.values?.firstOrNull()
                    ?.firstOrNull() equals ExtraModifiers.KotlinOnlyModifiers.Override
            }
            with((this / "inheritors" / "D"/ "f").cast<DFunction>()) {
                name equals "f"
                extra[AdditionalModifiers]?.content?.values?.firstOrNull()?.firstOrNull() equals null
            }
        }
    }

    @Test
    fun `substitution override (fake override) fun and prop should not have the override modifier`() {
        inlineModelTest(
            """
            |open class Job<T> {
            |    open fun do1(p: T) = p
            |    var p: T? = null
            |}
            |class GoodJob : Job<Int>()
        """
        ) {
            with((this / "inheritors" / "GoodJob" / "do1").cast<DFunction>()) {
                name equals "do1"
                extra[AdditionalModifiers]?.content?.values?.singleOrNull().orEmpty() equals emptySet<ExtraModifiers>()
            }
            with((this / "inheritors" / "GoodJob" / "p").cast<DProperty>()) {
                name equals "p"
                extra[AdditionalModifiers]?.content?.values?.singleOrNull().orEmpty() equals emptySet<ExtraModifiers>()
            }
        }
    }

    @Test
    fun `intersection override (fake override) fun and prop should not have the override modifier`() {
        inlineModelTest(
            """
            |interface A { fun x() val p: Int } 
            |interface B { fun x() val p: Int }  
            |interface C : A, B
        """
        ) {
            with((this / "inheritors" / "C" / "x").cast<DFunction>()) {
                name equals "x"
                extra[AdditionalModifiers]?.content?.values?.singleOrNull().orEmpty() equals emptySet<ExtraModifiers>()
            }
            with((this / "inheritors" / "C" / "p").cast<DProperty>()) {
                name equals "p"
                extra[AdditionalModifiers]?.content?.values?.singleOrNull().orEmpty() equals emptySet<ExtraModifiers>()
            }
        }
    }

    @Test
    fun `substitution override (fake override) should have KDoc`() {
        inlineModelTest(
            """
            |open class Job<T> {
            |   /** some doc */
            |    open fun do1(p: T) = p
            |    /** some doc */
            |    var p: T? = null
            |}
            |class GoodJob : Job<Int>()
        """
        ) {
            with((this / "inheritors" / "GoodJob" / "do1").cast<DFunction>()) {
                name equals "do1"
                documentation.values.single().children.first() equals Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text(body = "some doc"))
                            )
                        ), name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )
            }
            with((this / "inheritors" / "GoodJob" / "p").cast<DProperty>()) {
                name equals "p"
                documentation.values.single().children.first() equals Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text(body = "some doc"))
                            )
                        ), name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )
            }
        }
    }

    @Test
    fun `members implemented by delegation should inherit KDoc`() {
        inlineModelTest(
            """
               |interface CookieJar {
               |    /**
               |    * Saves cookies
               |     */
               |    fun saveFromResponse(url: String)
               |}
               |
               |class JavaNetCookieJar private constructor(
               |    delegate: CookieJarImpl,
               |) : CookieJar by delegate
            """.trimMargin()
        ) {
            with((this / "inheritors" / "JavaNetCookieJar"/ "saveFromResponse").cast<DFunction>()) {
                name equals "saveFromResponse"
                documentation.values.single().children.first() equals Description(
                    CustomDocTag(
                        children = listOf(
                            P(
                                children = listOf(Text(body = "Saves cookies"))
                            )
                        ), name = MARKDOWN_ELEMENT_FILE_NAME
                    )
                )
            }
        }
    }

    @Test
    fun `intersection overridden should have KDoc and correct DRI`() {
        inlineModelTest(
            """
            |open class FirstParent {
            |    fun basicMethod() = "OK"
            |    /**
            |    * Some doc
            |    */
            |    override fun toString(): String {
            |        return super.toString()
            |    }
            |}
            |
            |interface ISecondParent {}
            |
            |class ChildWithOneParent : FirstParent()
            |class ChildWithTwoParent : FirstParent(), ISecondParent
        """
        ) {
            with((this / "inheritors" / "ChildWithTwoParent" / "toString").cast<DFunction>()) {
                comments() equals "Some doc\n"
                dri equals DRI("inheritors", "FirstParent", org.jetbrains.dokka.links.Callable("toString", null, emptyList()) )
            }
        }
    }
}