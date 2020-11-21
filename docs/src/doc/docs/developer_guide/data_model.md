# Dokka Data Model

There a four data models that Dokka uses: Documentable Model, Documentation Model, Page Model and Content Model.

## Documentable Model

Documentable model represents parsed data, returned by compiler analysis. It retains basic order structure of parsed `Psi` or `Descriptor` models.

After creation, it is a collection of trees, each with `DModule` as a root. After the Merge step, all trees are folded into one. 

The main building block of this model is `Documentable` class, that is a base class for all more specific types that represents elements of parsed Kotlin and Java classes with pretty self-explanatory names: `DPackage`, `DFunction` and so on. `DClasslike` is a base for class-like elements, such as Classes, Enums, Interfaces and so on.

There are three non-documentable classes important for the model: `DRI`, `SourceSetDependent` and `ExtraProperty`.

* `DRI` (Dokka Resource Identifier) is a unique value that identifies specific `Documentable`. All references to other documentables different than direct ownership are described using DRIs. For example, `DFunction` with parameter of type `X` has only X's DRI, not the actual reference to X's Documentable object.
* `SourceSetDependent` is a map that handles multiplatform data, by connecting platform-specific data, declared with either `expect` or `actual` modifier, to a particular Source Set
* `ExtraProperty` is used to store any additional information that falls outside of regular model. It is highly recommended to use extras to provide any additional information when creating custom Dokka plugins. This element is a bit more complex, so you can read more about how to use it below.

### `ExtraProperty` class usage

`ExtraProperty` classes are used both by Documentable and Content models. To declare a new extra, you need to implement `ExtraProperty` interface.

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

It is advised to use following pattern when declaring new extras:

```kotlin
data class CustomExtra( [any values relevant to your extra ] ): ExtraProperty<Documentable> {
    companion object : CustomExtra.Key<Documentable, CustomExtra>
    override val key: CustomExtra.Key<Documentable, *> = CustomExtra
}
```
Merge strategy for extras is invoked only if merged objects have different values for same Extra. If you don't expect it to happen, you can omit implementing `mergeStrategyFor` function.

All extras for `ContentNode` and `Documentable` classes are stored in `PropertyContainer<C : Any>` class instances. The `C` generic class parameter limits the type of properties, that can be stored in the container -  it must match generic `C` class parameter from `ExtraProperty` interface. For example, if you would create `DFunction`-only `ExtraProperty`, it will be limited to be added only to `PropertyContainer<DFunction>`. 

In following example we will create `Documentable`-only property, store it in the container and then retrieve its value:

```kotlin
data class CustomExtra(val customExtraValue: String) : ExtraProperty<Documentable> {

    companion object: ExtraProperty.Key<Documentable, CustomExtra>

    override val key: ExtraProperty.Key<Documentable, *> = CustomExtra
}

val extra : PropertyContainer<DFunction> = PropertyContainer.withAll(
    CustomExtra("our value")
)

val customExtraValue : String? = extra[CustomProperty]?.customExtraValue
``` 

You can also use extras as markers, without storing any data in them:

```kotlin

object MarkerExtra : ExtraProperty<Any>, ExtraProperty.Key<Any, MarkerExtra> {
    override val key: ExtraProperty.Key<Any, *> = this
}

val extra : PropertyContainer<Any> = PropertyContainer.withAll(MarkerExtra)

val isMarked : Boolean = extra[MarkerExtra] != null

```

## Documentation Model

Documentation model is used along Documentable Model to store data obtained by parsing code commentaries.

There are three important classes here:

* `DocTag` describes a specific documentation syntax element, for example: header, footer, list, link, raw text, paragraph, etc.
* `TagWrapper` described a whole comment description or a specific comment tag, for example: @See, @Returns, @Author; and holds consisting `DocTag` elements 
* `DocumentationNode` acts as a container for `TagWrappers` for a specific `Documentable`

DocumentationNodes are references by a specific `Documentable`

## Page Model

Page Model represents the structure of future generated documentation pages and is independent of the final output format, which each node corresponding to exactly one output file. `Renderer` is processing each page separately.Subclasses of `PageNode` represents different kinds of rendered pages for Modules, Packages, Classes etc.

The Page Model is a tree structure, with `RootPageNode` being the root. 

## Content Model

Content Model describes how the actual page content is presented. It organizes it's structure into groups, tables, links, etc. Each node is identified by unique `DCI` (Dokka Content Identifier) and all references to other nodes different than direct ownership are described using DCIs.

`DCI` aggregates `DRI`s of all `Documentables` that make up specific `ContentNode`. 

Also, all `ExtraProperty` info from consisting `Documentable`s is propagated into Content Model and available for `Renderer`.

