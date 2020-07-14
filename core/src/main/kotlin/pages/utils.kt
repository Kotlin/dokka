package org.jetbrains.dokka.pages

import kotlin.reflect.KClass

inline fun <reified T : ContentNode, R : ContentNode> R.mapTransform(noinline operation: (T) -> T): R =
    mapTransform(T::class, operation)

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T : ContentNode, R : ContentNode> R.mapTransform(type: KClass<T>, operation: (T) -> T): R {
    if (this::class == type) {
        return operation(this as T) as R
    }
    val new = when (this) {
        is ContentGroup -> this.copy(children.map { it.mapTransform(type, operation) })
        is ContentHeader -> this.copy(children.map { it.mapTransform(type, operation) })
        is ContentCodeBlock -> this.copy(children.map { it.mapTransform(type, operation) })
        is ContentCodeInline -> this.copy(children.map { it.mapTransform(type, operation) })
        is ContentTable -> this.copy(children.map { it.mapTransform(type, operation) })
        is ContentList -> this.copy(children.map { it.mapTransform(type, operation) })
        is ContentDivergentGroup -> this.copy(children.map { it.mapTransform(type, operation) })
        is ContentDivergentInstance -> this.copy(
            before = before?.mapTransform(type, operation),
            divergent = divergent.mapTransform(type, operation),
            after = after?.mapTransform(type, operation)
        )
        is PlatformHintedContent -> this.copy(inner.mapTransform(type, operation))
        else -> this
    }
    return new as R
}
