@file:Suppress("unused")

import org.w3c.dom.url.URLSearchParams
import react.Props
import react.RComponent
import react.State

/**
 * A class that lives inside the root package
 */
class RootPackageClass {
    val description = "I do live in the root package!"
}

fun RComponent<*, *>.params() = URLSearchParams()

fun test(list: MutableList<Int>) = "list"

@JsModule("is-sorted")
@JsNonModule
external fun <T> sorted(a: Array<T>): Boolean