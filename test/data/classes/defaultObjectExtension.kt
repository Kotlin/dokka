class Foo {
    default object Default {
    }
}


/**
 * The def
 */
val Foo.Default.x: Int get() = 1
