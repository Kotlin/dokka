package utils

import org.jetbrains.dokka.model.Class
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.Property
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import kotlin.reflect.KClass
import kotlin.reflect.full.safeCast

@DslMarker
annotation class TestDSL

@TestDSL
abstract class ModelDSL : AbstractCoreTest() {
    operator fun Documentable?.div(name: String): Documentable? =
        this?.children?.find { it.name == name }

    inline fun <reified T : Documentable> Documentable?.cast(): T =
        (this as? T).assertNotNull()
}

@TestDSL
interface AssertDSL {
    infix fun Any?.equals(other: Any?) = this.assertEqual(other)
    infix fun Collection<Any>?.allEquals(other: Any?) =
        this?.also { c -> c.forEach { it equals other } } ?: run { assert(false) { "Collection is empty" } }

    infix fun <T> Collection<T>?.counts(n: Int) = this.orEmpty().assertCount(n)

    infix fun <T> T?.notNull(name: String): T = this.assertNotNull(name)

    fun <T> Collection<T>.assertCount(n: Int, prefix: String = "") =
        assert(count() == n) { "${prefix}Expected $n, got ${count()}" }

    fun <T> T?.assertEqual(expected: T, prefix: String = "") = assert(this == expected) {
        "${prefix}Expected $expected, got $this"
    }
}

inline fun <reified T : Any> Any?.assertIsInstance(name: String): T =
    this.let { it as? T } ?: throw AssertionError("$name should not be null")

fun List<DocumentationNode>.commentsToString(): String =
    this.flatMap { it.children }.joinToString(separator = "\n") { it.root.docTagSummary() }

fun TagWrapper.text(): String = when (val t = this) {
    is NamedTagWrapper -> "${t.name}: [${t.root.text()}]"
    else -> t.root.text()
}

fun DocTag.text(): String = when (val t = this) {
    is Text -> t.body
    is Code -> t.children.joinToString("\n") { it.text() }
    is P -> t.children.joinToString(separator = "\n") { it.text() }
    else -> t.toString()
}

fun <T : Documentable> T?.comments(): String = docs().map { it.text() }
    .joinToString(separator = "\n") { it }

fun <T> T?.assertNotNull(name: String = ""): T = this ?: throw AssertionError("$name should not be null")

fun <T : Documentable> T?.docs() = this?.documentation.orEmpty().values.flatMap { it.children }

val Class.supers
    get() = supertypes.flatMap{it.component2()}