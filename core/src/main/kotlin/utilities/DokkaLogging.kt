package org.jetbrains.dokka.utilities

interface DokkaLogger {
    fun debug(message: String)
    fun info(message: String)
    fun progress(message: String)
    fun warn(message: String)
    fun error(message: String)
}

object DokkaConsoleLogger : DokkaLogger {
    var warningCount: Int = 0
    var errorCount: Int = 0

    override fun debug(message: String)= println(message)

    override fun progress(message: String) = println("PROGRESS: $message")

    override fun info(message: String) = println(message)

    override fun warn(message: String) = println("WARN: $message").also { warningCount++ }

    override fun error(message: String) = println("ERROR: $message").also { errorCount++ }

    fun report() {
        if (warningCount > 0 || errorCount > 0) {
            println("generation completed with $warningCount warning" +
                    (if(warningCount == 1) "" else "s") +
                    " and $errorCount error" +
                    if(errorCount == 1) "" else "s"
            )
        } else {
            println("generation completed successfully")
        }
    }
}
