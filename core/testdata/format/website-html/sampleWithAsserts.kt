import java.io.FileNotFoundException
import java.io.File

/**
 * @sample sample
 */
fun a(): String {
    return "Hello, Work"
}

fun b(): String {
    return "Hello, Rest"
}

/**
 * @throws FileNotFoundException every time
 */
fun readSomeFile(f: File) {
    throw FileNotFoundException("BOOM")
}

fun sample() {
    assertPrints(a(), "Hello, Work")
    assertTrue(a() == b())
    assertTrue(a() == b(), "A eq B")
    assertFails("reading file now") { readSomeFile(File("some.txt")) }
    assertFailsWith<FileNotFoundException> { readSomeFile(File("some.txt")) }

    assertFails { readSomeFile(File("some.txt")) }

    fun indented() {
        assertFalse(a() != b(), "A neq B")
    }
}