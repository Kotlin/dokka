package org.jetbrains.dokka.pages

import kotlin.reflect.KClass

inline fun <reified T : ContentNode, R : ContentNode> R.mapTransform(noinline operation: (T) -> T): R =
    mapTransform(T::class, operation)

inline fun <reified T : ContentNode, R : ContentNode> R.recursiveMapTransform(noinline operation: (T) -> T): R =
        recursiveMapTransform(T::class, operation)

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T : ContentNode, R : ContentNode> R.mapTransform(type: KClass<T>, operation: (T) -> T): R {
    if (this::class == type) {
        return operation(this as T) as R
    }
    val new = when (this) {
        is ContentGroup -> copy(children = children.map { it.mapTransform(type, operation) })
        is ContentHeader -> copy(children = children.map { it.mapTransform(type, operation) })
        is ContentCodeBlock -> copy(children = children.map { it.mapTransform(type, operation) })
        is ContentCodeInline -> copy(children = children.map { it.mapTransform(type, operation) })
        is ContentTable -> copy(header = header.map { it.recursiveMapTransform(type, operation) }, children = children.map { it.recursiveMapTransform(type, operation) })
        is ContentList -> copy(children = children.map { it.mapTransform(type, operation) })
        is ContentDivergentGroup -> copy(children = children.map { it.mapTransform(type, operation) })
        is ContentDivergentInstance -> copy(
            before = before?.mapTransform(type, operation),
            divergent = divergent.mapTransform(type, operation),
            after = after?.mapTransform(type, operation)
        )
        is PlatformHintedContent -> copy(inner = inner.mapTransform(type, operation))
        else -> this
    }
    return new as R
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T : ContentNode, R : ContentNode> R.recursiveMapTransform(type: KClass<T>, operation: (T) -> T): R {
    val new = when (this) {
        is ContentGroup -> copy(children = children.map { it.recursiveMapTransform(type, operation) })
        is ContentHeader -> copy(children = children.map { it.recursiveMapTransform(type, operation) })
        is ContentCodeBlock -> copy(children = children.map { it.recursiveMapTransform(type, operation) })
        is ContentCodeInline -> copy(children = children.map { it.recursiveMapTransform(type, operation) })
        is ContentTable -> copy(header = header.map { it.recursiveMapTransform(type, operation) }, children = children.map { it.recursiveMapTransform(type, operation) })
        is ContentList -> copy(children = children.map { it.recursiveMapTransform(type, operation) })
        is ContentDivergentGroup -> copy(children = children.map { it.recursiveMapTransform(type, operation) })
        is ContentDivergentInstance -> copy(
                before = before?.recursiveMapTransform(type, operation),
                divergent = divergent.recursiveMapTransform(type, operation),
                after = after?.recursiveMapTransform(type, operation)
        )
        is PlatformHintedContent -> copy(inner = inner.recursiveMapTransform(type, operation))
        else -> this
    }
    if (new::class == type) {
        return operation(new as T) as R
    }
    return new as R
}
