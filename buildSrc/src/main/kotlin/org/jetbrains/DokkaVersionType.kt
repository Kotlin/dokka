package org.jetbrains

enum class DokkaVersionType(val suffix: Regex) {
    Release("^$".toRegex()), Snapshot("SNAPSHOT".toRegex()), Dev("dev-\\d+".toRegex()), MC("mc-\\d+".toRegex())
}
