open class Foo() {
    open val xyzzy: Int get() = 0
}

class Bar(): Foo() {
    override val xyzzy: Int get() = 1
}
