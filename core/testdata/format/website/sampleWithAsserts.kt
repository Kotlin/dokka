/**
 * @sample sample
 */
fun a(): String {
    return "Hello, Work"
}

fun b(): String {
    return "Hello, Rest"
}

fun sample() {
    assertPrints(a(), "Hello, Work")
    assertTrue(a() == b())
}