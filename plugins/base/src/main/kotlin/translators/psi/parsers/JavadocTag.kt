package org.jetbrains.dokka.base.translators.psi.parsers

internal enum class JavadocTag {
    PARAM, THROWS, RETURN, AUTHOR, SEE, DEPRECATED, EXCEPTION, HIDE,

    /**
     * Artificial tag created to handle tag-less section
     */
    DESCRIPTION,;

    override fun toString(): String = super.toString().toLowerCase()

    /* Missing tags:
        SERIAL,
        SERIAL_DATA,
        SERIAL_FIELD,
        SINCE,
        VERSION
     */

    companion object {
        private val name2Value = values().associateBy { it.name.toLowerCase() }

        /**
         * Lowercase-based `Enum.valueOf` variation for [JavadocTag].
         *
         * Note: tags are [case-sensitive](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html) in Java,
         * thus we are not allowed to use case-insensitive or uppercase-based lookup.
         */
        fun lowercaseValueOfOrNull(name: String): JavadocTag? = name2Value[name]
    }
}
