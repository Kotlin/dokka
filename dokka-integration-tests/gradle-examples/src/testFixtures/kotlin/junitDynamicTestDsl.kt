/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.gradle

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream
import kotlin.coroutines.CoroutineContext


suspend fun asdads() = dynamicTest {

    context("container") {
        context("container") {
            context("container") {

            }

        }
        context("container") {

        }
        test("asd") {
//            context("container") {
//
//            }
//            test("asd") {
//
//            }
        }
    }

}


@TestFactory
suspend fun `test sth else`() = dynamicTest {
    test("a test") { 1 shouldBe 1 }

    context("foo") {
        test("bar") { 1 shouldBe 1 }
        test("baz") { 1 shouldBe 1 }
    }

    context("foo2") {
        context("another container") {
            test("bar") { 1 shouldBe 1 }
            test("baz") {
                val st = MutableStateFlow("")
                st.emit("")
                1 shouldBe 1
            }
        }
        test("baz") { 1 shouldBe 1 }
    }

    test("another test") { 1 shouldBe 1 }
}

suspend fun dynamicTest(
    block: suspend DynamicContainerScope.RootScope.() -> Unit
): Stream<out DynamicNode> {
    val testScope = TestScope()
    val root = DynamicContainerScope.RootScope(testScope)
    root.block()
    return root.build()
}

@DslMarker
annotation class DynamicTestDsl

@DynamicTestDsl
sealed class DynamicContainerScope {

    abstract val coroutineContext: CoroutineContext
    private val nodes: MutableSharedFlow<DynamicNode> = MutableSharedFlow()

    class RootScope internal constructor(
        private val testScope: TestScope
    ) : DynamicContainerScope() {
        override val coroutineContext: CoroutineContext
            get() = testScope.coroutineContext
    }

    class ContainerScope internal constructor(
        override val coroutineContext: CoroutineContext
    ) : DynamicContainerScope()

    suspend fun test(name: String, block: suspend DynamicTestScope.() -> Unit) {
        val node = DynamicTest.dynamicTest(name) {
            CoroutineScope(coroutineContext + CoroutineName("dynamic test $name")).launch {
                DynamicTestScope.block()
            }
        }
        nodes.emit(node)
    }

    fun context(name: String, block: suspend ContainerScope.() -> Unit) {
        CoroutineScope(coroutineContext + CoroutineName("dynamic test container $name")).launch {
            val containerScope = ContainerScope(coroutineContext)
            containerScope.block()
            val node = DynamicContainer.dynamicContainer(
                name,
                containerScope.build()
            )
            nodes.emit(node)
        }
    }

    internal suspend fun build(): Stream<out DynamicNode> {
        val builder = Stream.builder<DynamicNode>()
        nodes.onEach { builder.add(it) }
            .collect()
        return builder.build()
    }
}

@DynamicTestDsl
object DynamicTestScope
