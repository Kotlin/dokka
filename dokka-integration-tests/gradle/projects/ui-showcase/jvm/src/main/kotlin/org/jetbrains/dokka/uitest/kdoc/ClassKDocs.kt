package org.jetbrains.dokka.uitest.kdoc

/**
 * A demonstration of various KDoc tags
 *
 * This class has no useful logic; it's just a documentation example.
 *
 * @param T some random type to test generics
 * @property primaryConstructorProperty documentation for the property
 * @constructor creates this class
 */
class ClassKDocs<T>(
    val primaryConstructorProperty: String
)
