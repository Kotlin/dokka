
class Test(var value: String) {
    fun brokenApply(v: String) = apply { value = v }

    fun brokenRun(v: String) = run {
        value = v
        this
    }

    fun brokenLet(v: String) = let {
        it.value = v
        it
    }

    fun brokenGenerics() = listOf("a", "b", "c")

    fun working(v: String) = doSomething()

    fun doSomething(): String = "Hello"
}