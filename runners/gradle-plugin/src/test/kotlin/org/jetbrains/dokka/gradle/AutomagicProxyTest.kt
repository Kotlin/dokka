package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.gradle.AutomagicProxyTest.TestInterface
import java.util.function.BiConsumer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class AutomagicProxyTest {

    private class TestException(message: String, cause: Throwable?) : Exception(message, cause)

    private fun interface TestInterface {
        @Throws(Throwable::class)
        operator fun invoke(): Int
    }

    @Test
    fun `simple method invocation`() {
        val instance = TestInterface { 0 }
        val proxy = automagicTypedProxy<TestInterface>(instance.javaClass.classLoader, instance)
        assertEquals(0, proxy())
    }

    @Test
    fun `exception throw in DokkaBootstrap is not wrapped inside UndeclaredThrowableException`() {
        val instanceThrowingTestException = object : DokkaBootstrap {
            override fun configure(serializedConfigurationJSON: String, logger: BiConsumer<String, String>) = Unit
            override fun generate() {
                throw TestException("Test Exception Message", Exception("Cause Exception Message"))
            }
        }

        val proxy = automagicTypedProxy<DokkaBootstrap>(
            instanceThrowingTestException.javaClass.classLoader,
            instanceThrowingTestException
        )

        val exception = assertFailsWith<TestException> {
            proxy.generate()
        }

        assertEquals("Test Exception Message", exception.message)
        assertEquals("Cause Exception Message", exception.cause?.message)
    }
}
