/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.internal

import org.jetbrains.dokka.DokkaBootstrap
import java.util.function.BiConsumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// should be top-level
private class FailingTestBootstrap : DokkaBootstrap {
    override fun configure(serializedConfigurationJSON: String, logger: BiConsumer<String, String>) {
        throw TestException("Test Exception Message: configure", Exception("Cause Exception Message: configure"))
    }

    override fun generate() {
        throw TestException("Test Exception Message: generate", Exception("Cause Exception Message: generate"))
    }
}

private class TestException(message: String, cause: Throwable?) : Exception(message, cause)

class DokkaBootstrapProxyTest {
    @Test
    fun `exception thrown in DokkaBootstrap is not wrapped inside InvocationTargetException`() {
        val proxy = DokkaBootstrapProxy(
            FailingTestBootstrap::class.java.classLoader,
            FailingTestBootstrap::class
        )

        assertFailsWith<TestException> {
            proxy.configure("") { _, _ -> }
        }.also { exception ->
            assertEquals("Test Exception Message: configure", exception.message)
            assertEquals("Cause Exception Message: configure", exception.cause?.message)
        }
        assertFailsWith<TestException> {
            proxy.generate()
        }.also { exception ->
            assertEquals("Test Exception Message: generate", exception.message)
            assertEquals("Cause Exception Message: generate", exception.cause?.message)
        }
    }
}
