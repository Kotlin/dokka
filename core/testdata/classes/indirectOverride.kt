abstract class C() {
    abstract fun foo()
}

abstract class D(): C()

class E(): D() {
    override fun foo() {}
}
