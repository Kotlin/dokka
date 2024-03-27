# Documentable Model

The Documentable model represents the data that is parsed from some programming language sources. Think of this data as 
of something that could be seen or produced by a compiler frontend, it's not far off from the truth.

By default, the documentables are created from:

* Descriptors (Kotlin's K1 compiler)
* Symbols (Kotlin's K2 compiler)
* [PSI](https://plugins.jetbrains.com/docs/intellij/psi.html) (Java's model). 

Code-wise, you can have a look at following classes:

* `DefaultDescriptorToDocumentableTranslator` - responsible for Kotlin -> `Documentable` mapping
* `DefaultPsiToDocumentableTranslator` - responsible for Java -> `Documentable` mapping

Upon creation, the documentable model represents a collection of trees, each with `DModule` as root.

Take some arbitrary Kotlin source code that is located within the same module:

```kotlin
// Package 1
class Clazz(val property: String) {
    fun function(parameter: String) {}
}

fun topLevelFunction() {}

// Package 2
enum class Enum { }

val topLevelProperty: String
```

This would be represented roughly as the following Documentable tree:

```mermaid
flowchart TD
    DModule --> firstPackage[DPackage]
    firstPackage --> DClass
    firstPackage --> toplevelfunction[DFunction] 
    DClass --> DProperty
    DClass --> DFunction
    DFunction --> DParameter
    DModule --> secondPackage[DPackage]
    secondPackage --> DEnum
    secondPackage --> secondPackageProperty[DProperty]
```

At later stages of transformation, all trees are folded into one by 
[DocumentableMerger](../extension_points/core_extension_points.md#documentablemerger).

## Documentable

The main building block of the documentable model is the `Documentable` class. It is the base class for all more specific 
types. All implementations represent elements of source code with mostly self-explanatory names: `DFunction`, 
`DPackage`, `DProperty`, and so on.

`DClasslike` is the base class for all class-like documentables, such as `DClass`, `DEnum`, `DAnnotation` and others.

The contents of each documentable normally represent what you would see in the source code. 

For example, if you open
`DClass`, you should find that it contains references to functions, properties, companion objects, constructors and so
on. `DEnum` should have references to its entries, and `DPackage` can have references to both classlikes and top-level
functions and properties (Kotlin-specific).

Here's an example of a documentable:

```kotlin
data class DClass(
    val dri: DRI,
    val name: String,
    val constructors: List<DFunction>,
    val functions: List<DFunction>,
    val properties: List<DProperty>,
    val classlikes: List<DClasslike>,
    val sources: SourceSetDependent<DocumentableSource>,
    val visibility: SourceSetDependent<Visibility>,
    val companion: DObject?,
    val generics: List<DTypeParameter>,
    val supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
    val documentation: SourceSetDependent<DocumentationNode>,
    val expectPresentInSet: DokkaSourceSet?,
    val modifier: SourceSetDependent<Modifier>,
    val sourceSets: Set<DokkaSourceSet>,
    val isExpectActual: Boolean,
    val extra: PropertyContainer<DClass> = PropertyContainer.empty()
) : DClasslike(), WithAbstraction, WithCompanion, WithConstructors,
    WithGenerics, WithSupertypes, WithExtraProperties<DClass>
```

___

There are three non-documentable classes that are important for this model:

* `DRI`
* `SourceSetDependent`
* `ExtraProperty`.

### DRI

`DRI` stans for _Dokka Resource Identifier_ - a unique value that identifies a specific `Documentable`.
All references and relations between the documentables (other than direct ownership) are described using `DRI`.

For example, `DFunction` with a parameter of type `Foo` only has `Foo`'s `DRI`, but not the actual reference
to `Foo`'s `Documentable` object.

#### Example

For an example of how a `DRI` can look like, let's take the `limitedParallelism` function from `kotlinx.coroutines`:

```kotlin
package kotlinx.coroutines

import ...
        
public abstract class MainCoroutineDispatcher : CoroutineDispatcher() {
    
    override fun limitedParallelism(parallelism: Int): CoroutineDispatcher {
        ...
    }
}
```

If we were to re-create the DRI of this function in code, it would look something like this:

```kotlin
DRI(
    packageName = "kotlinx.coroutines",
    classNames = "MainCoroutineDispatcher",
    callable = Callable(
        name = "limitedParallelism",
        receiver = null,
        params = listOf(
            TypeConstructor(
                fullyQualifiedName = "kotlin.Int",
                params = emptyList()
            )
        )
    ),
    target = PointingToDeclaration,
    extra = null
)
```

If you format it as `String`, it would look like this:

```
kotlinx.coroutines/MainCoroutineDispatcher/limitedParallelism/#kotlin.Int/PointingToDeclaration/
```

### SourceSetDependent

`SourceSetDependent` helps handling multiplatform data by associating platform-specific data (declared with either
`expect` or `actual` modifiers) with particular 
[source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets).

This comes in handy if the `expect` / `actual` declarations differ. For example, the default value for `actual` might 
differ from that declared in `expect`, or code comments written for `expect` might be different from what's written
for `actual`.

Under the hood, it's a `typealias` to a `Map`:

```kotlin
typealias SourceSetDependent<T> = Map<DokkaSourceSet, T>
```

### ExtraProperty

`ExtraProperty` is used to store any additional information that falls outside of the regular model. It is highly
recommended to use extras to provide any additional information when creating custom Dokka plugins.

This element is a bit more complex, so you can read more about how to use it
[in a separate section](extra.md).

___

## Documentation model

The Documentation model is used alongside documentables to store data obtained by parsing
code comments (such as KDocs / Javadocs).

### DocTag

`DocTag` describes a specific documentation syntax element.

It's universal across language sources. For example, the DocTag `B` is the same for `**bold**` in Kotlin and
`<b>bold</b>` in Java.

However, some DocTag elements are specific to one language. There are many such examples for Java, because it allows 
HTML tags inside the Javadoc comments, some of which are simply not possible to reproduce with Markdown that KDocs use.

`DocTag` elements can be deeply nested with other `DocTag` children elements.

Examples:

```kotlin
data class H1(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

data class H2(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

data class Strikethrough(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

data class Strong(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : DocTag()

data class CodeBlock(
    override val children: List<DocTag> = emptyList(),
    override val params: Map<String, String> = emptyMap()
) : Code()

```

### TagWrapper

`TagWrapper` describes the whole comment description or a specific comment tag. For example: `@see` / `@author` / `@return`.

Since each such section may contain formatted text inside it, each `TagWrapper` has `DocTag` children.

```kotlin
/**
 * @author **Ben Affleck*
 * @return nothing, except _sometimes_ it may throw an [Error]
 */
fun foo() {}
```

### DocumentationNode

`DocumentationNode` acts as a container for multiple `TagWrapper` elements for a specific `Documentable`, usually
used like this:

```kotlin
data class DFunction(
    ...
    val documentation: SourceSetDependent<DocumentationNode>,
    ...
)
```
