package matchers.content

import org.jetbrains.dokka.pages.ContentComposite
import org.jetbrains.dokka.pages.ContentGroup
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.test.tools.matchers.content.*
import kotlin.reflect.KClass

// entry point:
fun ContentNode.assertNode(block: ContentMatcherBuilder<ContentComposite>.() -> Unit) {
    ContentMatcherBuilder(ContentComposite::class).apply(block).build().tryMatch(this)
}


// DSL:
@DslMarker
annotation class ContentMatchersDsl

@ContentMatchersDsl
class ContentMatcherBuilder<T : ContentComposite> @PublishedApi internal constructor(private val kclass: KClass<T>) {
    @PublishedApi
    internal val children = mutableListOf<MatcherElement>()
    internal val assertions = mutableListOf<T.() -> Unit>()

    fun build() = CompositeMatcher(kclass, children) { assertions.forEach { it() } }

    // part of DSL that cannot be defined as an extension
    operator fun String.unaryPlus() {
        children += TextMatcher(this)
    }
}

fun <T : ContentComposite> ContentMatcherBuilder<T>.check(assertion: T.() -> Unit) {
    assertions += assertion
}

inline fun <reified S : ContentComposite> ContentMatcherBuilder<*>.composite(
    block: ContentMatcherBuilder<S>.() -> Unit
) {
    children += ContentMatcherBuilder(S::class).apply(block).build()
}

inline fun <reified S : ContentNode> ContentMatcherBuilder<*>.node(noinline assertions: S.() -> Unit) {
    children += NodeMatcher(S::class, assertions)
}

fun ContentMatcherBuilder<*>.skipAllNotMatching() {
    children += Anything
}


// Convenience functions:
fun ContentMatcherBuilder<*>.group(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) {
    children += ContentMatcherBuilder(ContentGroup::class).apply(block).build()
}

fun ContentMatcherBuilder<*>.somewhere(block: ContentMatcherBuilder<*>.() -> Unit) {
    skipAllNotMatching()
    block()
    skipAllNotMatching()
}