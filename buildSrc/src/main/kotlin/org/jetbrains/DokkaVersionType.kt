package org.jetbrains

enum class DokkaVersionType(val suffix: Regex) {
    RELEASE("^$".toRegex()),
    RC("RC\\d?".toRegex()),
    SNAPSHOT("SNAPSHOT".toRegex()),
    DEV("dev-\\d+".toRegex());
}
