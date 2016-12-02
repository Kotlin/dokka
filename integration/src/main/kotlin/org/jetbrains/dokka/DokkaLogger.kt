package org.jetbrains.dokka

interface DokkaLogger {
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String)
}

