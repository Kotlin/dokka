package org.jetbrains.dokka.uitest.types

@Deprecated("because", replaceWith = ReplaceWith("this.foo()", "kotlin.String"), DeprecationLevel.ERROR)
class SimpleDeprecatedKotlinClass {
}
