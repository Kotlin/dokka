# Extra

## Introduction

`ExtraProperty` classes are used both by the [Documentable](documentable_model.md) and the [Content](page_content.md#content-model)
models.

`ExtraProperty` is declared as:

```kotlin
interface ExtraProperty<in C : Any> {
    interface Key<in C : Any, T : Any> {
        fun mergeStrategyFor(left: T, right: T): MergeStrategy<C> = MergeStrategy.Fail {
            throw NotImplementedError("Property merging for $this is not implemented")
        }
    }

    val key: Key<C, *>
}
```

To create a new extra, you need to implement the `ExtraProperty` interface. It is advised to use the following pattern
when declaring new extras:

```kotlin
data class CustomExtra(
    [any data relevant to your extra], 
    [any data relevant to your extra] 
): ExtraProperty<Documentable> {
    override val key: CustomExtra.Key<Documentable, *> = CustomExtra
    companion object : CustomExtra.Key<Documentable, CustomExtra>
}
```

Merge strategy (the `mergeStrategyFor` method) for extras is invoked during the
[merging](../extension_points/core_extension_points.md#documentablemerger) of documentables from different 
[source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets), where each documentable
has its own `Extra` of the same type. 

## PropertyContainer

All extras for `ContentNode` and `Documentable` classes are stored in the `PropertyContainer<C : Any>` class instances.

```kotlin
data class DFunction(
    ...
    override val extra: PropertyContainer<DFunction> = PropertyContainer.empty()
    ...
) : WithExtraProperties<DFunction>
```

`PropertyContainer` has a number of convenient functions for handling extras in a collection-like manner.

The generic class parameter `C` limits the types of properties that can be stored in the container - it must
match the generic `C` class parameter from the `ExtraProperty` interface. This allows creating extra properties
which can only be stored in a specific `Documentable`.

## Usage example

In following example we will create a `DFunction`-only extra property, store it and then retrieve its value:

```kotlin
// Extra that is applicable only to DFunction
data class CustomExtra(val customExtraValue: String) : ExtraProperty<DFunction> {
    override val key: ExtraProperty.Key<Documentable, *> = CustomExtra
    companion object: ExtraProperty.Key<Documentable, CustomExtra>
}

// Storing it inside the documentable
fun DFunction.withCustomExtraProperty(data: String): DFunction {
    return this.copy(
        extra = extra + CustomExtra(data)
    )
}

// Retrieveing it from the documentable
fun DFunction.getCustomExtraPropertyValue(): String? {
    return this.extra[CustomExtra]?.customExtraValue
}
```

___

You can also use extras as markers, without storing any data in them:

```kotlin

object MarkerExtra : ExtraProperty<Any>, ExtraProperty.Key<Any, MarkerExtra> {
    override val key: ExtraProperty.Key<Any, *> = this
}

fun Documentable.markIfFunction(): Documentable {
    return when(this) {
        is DFunction -> this.copy(extra = extra + MarkerExtra)
        else -> this
    }
}

fun WithExtraProperties<Documentable>.isMarked(): Boolean {
    return this.extra[MarkerExtra] != null
}
```
