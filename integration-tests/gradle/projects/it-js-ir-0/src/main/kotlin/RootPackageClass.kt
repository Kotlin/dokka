@file:Suppress("unused")

import org.w3c.dom.url.URLSearchParams
import react.Props
import react.State

/**
 * A class that lives inside the root package
 */
class RootPackageClass {
    val description = "I do live in the root package!"
}

// sample method that uses classes from dom and react, should compile
fun URLSearchParams.react(props: Props, state: State) {}

fun test(list: MutableList<Int>) = "list"

@JsModule("is-sorted")
@JsNonModule
external fun <T> sorted(a: Array<T>): Boolean