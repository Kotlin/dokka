/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package matchers.content

import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.test.tools.matchers.content.*
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.asserter

// entry point:
public fun ContentNode.assertNode(block: ContentMatcherBuilder<ContentComposite>.() -> Unit) {
    val matcher = ContentMatcherBuilder(ContentComposite::class).apply(block).build()
    try {
        matcher.tryMatch(this)
    } catch (e: MatcherError) {
        throw AssertionError(e.message + "\n" + matcher.toDebugString(e.anchor, e.anchorAfter))
    }
}


// DSL:
@DslMarker
public annotation class ContentMatchersDsl

@ContentMatchersDsl
public class ContentMatcherBuilder<T : ContentComposite> @PublishedApi internal constructor(private val kclass: KClass<T>) {
    @PublishedApi
    internal val children: MutableList<MatcherElement> = mutableListOf()
    internal val assertions = mutableListOf<T.() -> Unit>()

    public fun build(): CompositeMatcher<T> = CompositeMatcher(kclass, childrenOrSkip()) { assertions.forEach { it() } }

    // part of DSL that cannot be defined as an extension
    public operator fun String.unaryPlus() {
        children += TextMatcher(this)
    }

    private fun childrenOrSkip() = if (children.isEmpty() && assertions.isNotEmpty()) listOf(Anything) else children
}

public fun <T : ContentComposite> ContentMatcherBuilder<T>.check(assertion: T.() -> Unit) {
    assertions += assertion
}

private val ContentComposite.extractedText
    get() = withDescendants().filterIsInstance<ContentText>().joinToString(separator = "") { it.text }

public fun <T : ContentComposite> ContentMatcherBuilder<T>.hasExactText(expected: String) {
    assertions += {
        assertEquals(expected, this.extractedText)
    }
}

public inline fun <reified S : ContentComposite> ContentMatcherBuilder<*>.composite(
    block: ContentMatcherBuilder<S>.() -> Unit
) {
    children += ContentMatcherBuilder(S::class).apply(block).build()
}

public inline fun <reified S : ContentNode> ContentMatcherBuilder<*>.node(noinline assertions: S.() -> Unit = {}) {
    children += NodeMatcher(S::class, assertions)
}

public fun ContentMatcherBuilder<*>.skipAllNotMatching() {
    children += Anything
}


// Convenience functions:
public fun ContentMatcherBuilder<*>.group(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) {
    composite(block)
}

public fun ContentMatcherBuilder<*>.tabbedGroup(
    block: ContentMatcherBuilder<ContentGroup>.() -> Unit
) {
    composite<ContentGroup> {
        block()
        check { assertContains(this.style, ContentStyle.TabbedContent) }
    }
}

public fun ContentMatcherBuilder<*>.tab(
    tabbedContentType: TabbedContentType, block: ContentMatcherBuilder<ContentGroup>.() -> Unit
) {
    composite<ContentGroup> {
        block()
        check {
            assertEquals(tabbedContentType, this.extra[TabbedContentTypeExtra]?.value)
        }
    }
}

public fun ContentMatcherBuilder<*>.header(expectedLevel: Int? = null, block: ContentMatcherBuilder<ContentHeader>.() -> Unit) {
    composite<ContentHeader> {
        block()
        check { if (expectedLevel != null) assertEquals(expectedLevel, this.level) }
    }
}

public fun ContentMatcherBuilder<*>.p(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) {
    composite<ContentGroup> {
        block()
        check { assertContains(this.style, TextStyle.Paragraph) }
    }
}

public fun ContentMatcherBuilder<*>.link(block: ContentMatcherBuilder<ContentLink>.() -> Unit) {
    composite(block)
}

public fun ContentMatcherBuilder<*>.table(block: ContentMatcherBuilder<ContentTable>.() -> Unit) {
    composite(block)
}

public fun ContentMatcherBuilder<*>.platformHinted(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) {
    composite<PlatformHintedContent> { group(block) }
}

public fun ContentMatcherBuilder<*>.list(block: ContentMatcherBuilder<ContentList>.() -> Unit) {
    composite(block)
}

public fun ContentMatcherBuilder<*>.codeBlock(block: ContentMatcherBuilder<ContentCodeBlock>.() -> Unit) {
    composite(block)
}

public fun ContentMatcherBuilder<*>.codeInline(block: ContentMatcherBuilder<ContentCodeInline>.() -> Unit) {
    composite(block)
}

public fun ContentMatcherBuilder<*>.caption(block: ContentMatcherBuilder<ContentGroup>.() -> Unit) {
    composite<ContentGroup> {
        block()
        check { assertContains(this.style, ContentStyle.Caption) }
    }
}

public fun ContentMatcherBuilder<*>.br() {
    node<ContentBreakLine>()
}

public fun ContentMatcherBuilder<*>.somewhere(block: ContentMatcherBuilder<*>.() -> Unit) {
    skipAllNotMatching()
    block()
    skipAllNotMatching()
}

public fun ContentMatcherBuilder<*>.divergentGroup(
    block: ContentMatcherBuilder<ContentDivergentGroup>.() -> Unit
) {
    composite(block)
}

public fun ContentMatcherBuilder<ContentDivergentGroup>.divergentInstance(
    block: ContentMatcherBuilder<ContentDivergentInstance>.() -> Unit
) {
    composite(block)
}

public fun ContentMatcherBuilder<ContentDivergentInstance>.before(
    block: ContentMatcherBuilder<ContentComposite>.() -> Unit
) {
    composite(block)
}

public fun ContentMatcherBuilder<ContentDivergentInstance>.divergent(
    block: ContentMatcherBuilder<ContentComposite>.() -> Unit
) {
    composite(block)
}

public fun ContentMatcherBuilder<ContentDivergentInstance>.after(
    block: ContentMatcherBuilder<ContentComposite>.() -> Unit
) {
    composite(block)
}

/*
 * TODO replace with kotlin.test.assertContains after migrating to Kotlin language version 1.5+
 */
private fun <T> assertContains(iterable: Iterable<T>, element: T) {
    asserter.assertTrue(
        { "Expected the collection to contain the element.\nCollection <$iterable>, element <$element>." },
        iterable.contains(element)
    )
}
