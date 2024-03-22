package org.jetbrains.dokka.uitest.types

/**
 * Documentation for a function
 */
fun simpleKotlinTopLevelFunction() {}

@Deprecated("because", replaceWith = ReplaceWith("this.foo()", "kotlin.String"), DeprecationLevel.ERROR)
fun simpleDeprecatedKotlinTopLevelFunction() {}
