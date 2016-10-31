package org.jetbrains.dokka.maven

import org.apache.maven.plugin.logging.Log
import org.jetbrains.dokka.DokkaLogger

class MavenDokkaLogger(val log: Log) : DokkaLogger {
    override fun error(message: String) {
        log.error(message)
    }

    override fun info(message: String) {
        log.info(message)
    }

    override fun warn(message: String) {
        log.warn(message)
    }
}