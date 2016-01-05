open class A {
    fun foo() {
    }
}

open class B {
    fun bar() {
    }
}

class C : A {
    fun xyzzy() {
    }

    companion object : B () {
        fun shazam()
    }
}
