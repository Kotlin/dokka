data class Foo {
    inline fun bar(noinline notInlined: () -> Unit) {
    }

    inline val x: Int
}
