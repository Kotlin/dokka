package foo

data class Person internal constructor(
        val name: String = "",
        val age: Int = 0
) {
    constructor(age: Int): this("", age)
}
