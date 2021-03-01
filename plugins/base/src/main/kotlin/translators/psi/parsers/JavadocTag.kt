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
}