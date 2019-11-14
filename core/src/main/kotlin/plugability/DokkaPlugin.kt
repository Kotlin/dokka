package org.jetbrains.dokka.plugability

interface DokkaPlugin {
    val name: String
    fun install(context: DokkaContext)
}