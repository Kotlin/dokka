package foo

/**
 * Function with receiver
 */
fun String.fn() {
}

/**
 * Function with receiver
 */
fun String.fn(x: Int) {
}

/**
 * Property with receiver.
 */
val String.foobar: Int
     get() = size() * 2
