package org.jetbrains.dokka.gradle

import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.dokka.DokkaBootstrap
import org.jetbrains.dokka.gradle.internal.LoggerAdapter
import org.jetbrains.dokka.utilities.DokkaLogger
import kotlin.test.*


class AutomagicProxyTest {

    private class TestException(message: String, cause: Throwable?) : Exception(message, cause)

    private interface TestInterfaceAlpha {
        operator fun invoke(): Int
    }

    private interface TestInterfaceBeta {
        operator fun invoke(): Int
    }

    @Test
    fun `simple method invocation`() {
        val instance = object : TestInterfaceAlpha {
            override fun invoke(): Int = 0
        }
        val proxy: TestInterfaceBeta by dynamicCast { instance }

        assertFalse(instance::class == proxy::class)

        assertEquals(0, proxy())
    }

    private interface DokkaBootstrapInternal {
        @Throws(Throwable::class)
        fun configure(serializedConfigurationJSON: String, logger: DokkaLogger)

        @Throws(Throwable::class)
        fun generate()
    }

    @Test
    fun `exception throw in DokkaBootstrap is not wrapped inside UndeclaredThrowableException`() {
        val instanceThrowingTestException = object : DokkaBootstrap {
            override fun configure(serializedConfigurationJSON: String, logger: DokkaLogger) = Unit
            override fun generate() {
                throw TestException("Test Exception Message", Exception("Cause Exception Message"))
            }
        }

        val proxy: DokkaBootstrapInternal by dynamicCast { instanceThrowingTestException }

        assertFalse(instanceThrowingTestException::class == proxy::class)

        assertEquals(
            Unit,
            proxy.configure("asd", LoggerAdapter(Logging.getLogger("test-logger")))
        )

        val exception = assertFailsWith<TestException> {
            proxy.generate()
        }

        assertEquals("Test Exception Message", exception.message)
        assertEquals("Cause Exception Message", exception.cause?.message)
    }
}
