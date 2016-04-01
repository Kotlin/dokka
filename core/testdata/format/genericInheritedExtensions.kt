open class Foo<T>

class Bar<T> : Foo<T>()

fun <T> Foo<T>.first() {

}

fun <T> Bar<T>.second() {

}
