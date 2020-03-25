package  org.jetbrains.dokka.test.tools.matchers.content

import org.jetbrains.dokka.pages.ContentComposite
import org.jetbrains.dokka.pages.ContentNode
import org.jetbrains.dokka.pages.ContentText
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

sealed class MatcherElement

class TextMatcher(val text: String) : MatcherElement()

open class NodeMatcher<T: ContentNode>(
    val kclass: KClass<T>,
    val assertions: T.() -> Unit = {}
): MatcherElement() {
    open fun tryMatch(node: ContentNode) {
        assertions(kclass.cast(node))
    }
}

class CompositeMatcher<T : ContentComposite>(
    kclass: KClass<T>,
    private val children: List<MatcherElement>,
    assertions: T.() -> Unit = {}
) : NodeMatcher<T>(kclass, assertions) {
    private val normalizedChildren: List<MatcherElement> by lazy {
        children.fold(listOf<MatcherElement>()) { acc, e ->
            when {
                acc.lastOrNull() is Anything && e is Anything -> acc
                acc.lastOrNull() is TextMatcher && e is TextMatcher ->
                    acc.dropLast(1) + TextMatcher((acc.lastOrNull() as TextMatcher).text + e.text)
                else -> acc + e
            }
        }
    }

    override fun tryMatch(node: ContentNode) {
        super.tryMatch(node)
        kclass.cast(node).children.fold(normalizedChildren.pop()) { acc, n -> acc.next(n) }.finish()
    }
}

object Anything : MatcherElement()

private sealed class MatchWalkerState {
    abstract fun next(node: ContentNode): MatchWalkerState
    abstract fun finish()
}

private class TextMatcherState(val text: String, val rest: List<MatcherElement>) : MatchWalkerState() {
    override fun next(node: ContentNode): MatchWalkerState {
        node as ContentText
        return when {
            text == node.text -> rest.pop()
            text.startsWith(node.text) -> TextMatcherState(text.removePrefix(node.text), rest)
            else -> throw AssertionError("Expected text: \"$text\", but found: \"${node.text}\"")
        }
    }

    override fun finish() = throw AssertionError("\"$text\" was not found" + rest.messageEnd)
}

private object EmptyMatcherState : MatchWalkerState() {
    override fun next(node: ContentNode): MatchWalkerState {
        throw AssertionError("Unexpected $node")
    }

    override fun finish() = Unit
}

private class NodeMatcherState(
    val matcher: NodeMatcher<*>,
    val rest: List<MatcherElement>
) : MatchWalkerState() {
    override fun next(node: ContentNode): MatchWalkerState {
        matcher.tryMatch(node)
        return rest.pop()
    }

    override fun finish() = throw AssertionError("Composite of type ${matcher.kclass} was not found" + rest.messageEnd)
}

private class SkippingMatcherState(
    val innerState: MatchWalkerState
) : MatchWalkerState() {
    override fun next(node: ContentNode): MatchWalkerState = runCatching { innerState.next(node) }.getOrElse { this }

    override fun finish() = innerState.finish()
}

private fun List<MatcherElement>.pop(): MatchWalkerState = when (val head = firstOrNull()) {
    is TextMatcher -> TextMatcherState(head.text, drop(1))
    is NodeMatcher<*> -> NodeMatcherState(head, drop(1))
    is Anything -> SkippingMatcherState(drop(1).pop())
    null -> EmptyMatcherState
}

private val List<MatcherElement>.messageEnd: String
    get() = filter { it !is Anything }
        .count().takeIf { it > 0 }
        ?.let { " and $it further matchers were not satisfied" } ?: ""

// Creating this whole mechanism was most scala-like experience I had since I stopped using scala.
// I don't know how I should feel about it.