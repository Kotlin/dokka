package org.jetbrains.dokka

class DokkaGenerator(
    private val configurationWithLinks: DokkaConfiguration,
    private val logger: DokkaLogger
) {
    fun generate() {
        println("RUNNED")
        logger.error("runned")
        throw AssertionError()
    }

}
