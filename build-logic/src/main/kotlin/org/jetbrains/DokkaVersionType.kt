package org.jetbrains

enum class DokkaVersionType(val suffix: Regex) {
    RELEASE("^$".toRegex()),
    RC("RC\\d?".toRegex()),
    SNAPSHOT("SNAPSHOT".toRegex()),
    DEV("dev-\\d+".toRegex());

    companion object {
        fun from(dokkaVersion: String?): DokkaVersionType? {
            if (dokkaVersion.isNullOrBlank()) return null

            val dokkaVersionSuffix = dokkaVersion.substringAfter("-", "")

            return values().single { it.suffix.matches(dokkaVersionSuffix) }
        }
    }
}
