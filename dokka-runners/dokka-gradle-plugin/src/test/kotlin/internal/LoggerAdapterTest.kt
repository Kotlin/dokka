package internal

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.jetbrains.dokka.gradle.internal.LoggerAdapter
import org.junit.jupiter.api.Test
import org.slf4j.event.SubstituteLoggingEvent
import org.slf4j.helpers.NOPLogger
import org.slf4j.helpers.SubstituteLoggerFactory
import kotlin.io.path.createTempFile
import kotlin.io.path.readText

class LoggerAdapterTest {

    @Test
    fun testLogger() {
        val logFactory = SubstituteLoggerFactory()

        val loggerAdapter = LoggerAdapter(
            createTempFile().toFile(),
            logFactory.getLogger("test"),
            "LOG-TAG"
        )

        loggerAdapter.error("an error msg")
        loggerAdapter.warn("a warn msg")
        loggerAdapter.debug("a debug msg")
        loggerAdapter.info("an info msg")
        loggerAdapter.progress("a progress msg")

        loggerAdapter.errorsCount shouldBe 1
        loggerAdapter.warningsCount shouldBe 1

        logFactory.eventQueue.map { it.render() }.shouldContainExactly(
            "ERROR e: [LOG-TAG] an error msg",
            "WARN w: [LOG-TAG] a warn msg",
            "INFO [LOG-TAG] a debug msg",
            "INFO [LOG-TAG] an info msg",
            "INFO [LOG-TAG] a progress msg",
        )
    }

    @Test
    fun testLogFile() {
        val logFile = createTempFile()

        LoggerAdapter(
            logFile.toFile(),
            NOPLogger.NOP_LOGGER,
            "LOG-TAG"
        ).use { loggerAdapter ->
            loggerAdapter.error("an error msg")
            loggerAdapter.warn("a warn msg")
            loggerAdapter.debug("a debug msg")
            loggerAdapter.info("an info msg")
            loggerAdapter.progress("a progress msg")

            loggerAdapter.errorsCount shouldBe 1
            loggerAdapter.warningsCount shouldBe 1
        }

        logFile.readText() shouldBe """
            |[ERROR] an error msg
            |[WARN] a warn msg
            |[DEBUG] a debug msg
            |[INFO] an info msg
            |[PROGRESS] a progress msg
            |
        """.trimMargin()
    }

    companion object {
        private fun SubstituteLoggingEvent.render(): String = buildString {
            append("$level")
            append(" ")
            append(message)
        }
    }
}
