package org.jetbrains.dokka.utilities

interface DokkaLogger {
    var warningsCount: Int
    var errorsCount: Int
    fun debug(message: String)
    fun info(message: String)
    fun progress(message: String)
    fun warn(message: String)
    fun error(message: String)
    fun report()
}

object DokkaConsoleLogger : DokkaLogger {
    override var warningsCount: Int = 0
    override var errorsCount: Int = 0

    override fun debug(message: String)= println(message)

    override fun progress(message: String) = println("PROGRESS: $message")

    override fun info(message: String) = println(message)

    override fun warn(message: String) = println("WARN: $message").also { warningsCount++ }

    override fun error(message: String) = println("ERROR: $message").also { errorsCount++ }

    override fun report() {
        if (warningsCount > 0 || errorsCount > 0) {
            println("Generation completed with $warningsCount warning" +
                    (if(warningsCount == 1) "" else "s") +
                    " and $errorsCount error" +
                    if(errorsCount == 1) "" else "s"
            )
        } else {
            println("generation completed successfully")
        }
    }
}
