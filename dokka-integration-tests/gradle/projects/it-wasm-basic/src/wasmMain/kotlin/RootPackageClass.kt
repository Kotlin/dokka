@file:Suppress("unused")

import kotlinx.coroutines.CoroutineScope

/**
 * A class that lives inside the root package
 */
class RootPackageClass {
    val description = "I do live in the root package!"
}

fun test(list: MutableList<Int>) = "list"

@JsModule("is-sorted")
@JsNonModule
external fun <T> sorted(a: Array<T>): Boolean

// this declaration uses external library and external documentation link
fun CoroutineScope.externalClass() = "some string"

/**
 * Some external function with JsFun
 * @see kotlin.JsFun
 */
@kotlin.JsFun("xxx")
external fun externalFun()