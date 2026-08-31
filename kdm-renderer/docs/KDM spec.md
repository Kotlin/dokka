# KDM spec

KEEP: [https://github.com/Kotlin/KEEP/discussions/484](https://github.com/Kotlin/KEEP/discussions/484)

# Main questions

## Serialization format

Note: the documentation on declarations itself could use the same, or a different serialization mechanism \- [https://youtrack.jetbrains.com/issue/KT-88346](https://youtrack.jetbrains.com/issue/KT-88346)

* JSON  
  * Easy to read, easy to use.  
  * A lot of tools.  
  * Stable kotlinx-serialization support.  
  * Good size when compressed.  
* ProtoBuf  
  * A bit smaller than JSON, but since most of the information is strings, the win isn't that big.  
  * Might be faster to encode/decode than JSON?  
  * Schema-based \- most of the tools can use a schema to understand the data.  
  * Polymorphism is possible, but a bit hard with kotlinx-serialization \- e.g., having the same model as in JSON will require custom serializer implementations.  
    1. Possible to generate a library based on a schema (not using kx.serialization).  
* Kotlin metadata \+ meta-metadata  
  * Uses ProtoBuf.  
  * Reuses the existing API for working with metadata from Kotlin.  
  * Requires some documentation metadata extension.  
  * “Effectively stable” and always kept compatible.  
  * API for external tools (not in Kotlin) might not be so nice, as it was never treated as a public API.

## Java

✅Java in the input:

* ✅Mapped Java annotations \- like Deprecated \- they have different properties.  
  * **Preliminary resolution:**  
    * Replace deprecated annotations with a separate concept (see [KDM spec](https://docs.google.com/document/d/18Qus_5Y5cbbrpVnx8xoo1-8nGhtqcZ9z-Q49bk1BzWE/edit?tab=t.0#heading=h.jq39g5jdfpwu)) as it’s special in both Java and Kotlin  
    * Other [mapped annotations](https://github.com/JetBrains/kotlin/blob/944976ab8fbfb96e72c80cb4375535a1166532bf/compiler/fir/checkers/checkers.jvm/src/org/jetbrains/kotlin/fir/analysis/jvm/checkers/expression/FirJavaAnnotationsChecker.kt#L33) don’t need additional handling \-\> just use Kotlin ones.  
* ✅What about Java primitive types: `int` (Java primitive), `Integer` (Java object), `Int` (Kotlin type)  
  * **Preliminary resolution:**  
    * Represent primitives in **types** as `kotlin.Int` and co. types with additional metadata regarding nullability, which should allow us to distinguish between `int`, `Integer,` and `@NotNull Integer,` as in Java, it's possible to have two functions like `void foo(int value)` and `void foo(Integer value),` which are different declarations.  
* ✅What about built-in/mapped types?  `Throwable` or `String`  
  * **Preliminary resolution:**  
    * Store a reference to the mapped Java declarations. It can be used for potential cross-linking from Kotlin docs to JDK docs.  
    * This could be especially useful for declarations, which are not one-to-one mappings, like mapping Kotlin’s `List.size` **property** to Java’s `List.size()` **function**.

Java in the output: Decide if it should be possible to generate a Java-like API based on KDM (mostly for Google and [Javadoc generation](https://youtrack.jetbrains.com/issue/OSIP-1348/Research-Java-stub-based-Javadoc-generation-for-Kotlin-libraries)).

* Google feedback: [https://github.com/Kotlin/KEEP/discussions/484\#discussioncomment-16921555](https://github.com/Kotlin/KEEP/discussions/484#discussioncomment-16921555)  
* The main problem is that they need to generate documentation for both Kotlin and **Java** consumers/users \- they have a tool called Dackka, which is a HUGE Dokka plugin. They also have their own “rendering representation” from which they generate Kotlin and Java API references.  
* Google relies heavily on Dokka's API, using its own filters, transformers, and renderers.  
  * We need to evaluate whether the model covers their use cases or requires a migration path.  
  * I've communicated this to Google; they were more or less ok with it, but would really like to reuse the model or have a **full API needed in AA.** There is minimal KDoc API, and no Javadoc API at all.  
  * Their options:  
    * Use KDM  
    * Copy Dokka’s code and maintain it themselves  
    * Migrate to AA \+ **NEW** KDoc API  
* Google will need Java API generation almost FOREVER.  
* Maybe they just need some additional information in JSON, and they could approximate an API then?  
  * This is more or less what they are doing right now: Dokka’s models represent the Kotlin API and contain almost all the information needed (like file annotations or the file name from the `source` [property](https://github.com/Kotlin/dokka/blob/b49856b08a8e56cf7d272cab6a6b16a47b71afb6/dokka-subprojects/core/src/main/kotlin/org/jetbrains/dokka/model/Documentable.kt#L1028)).  
  * So the most problematic part is that they need information about file names and their annotations (JVM multifile facade and JVM file name), which is not really a Kotlin API but rather a Kotlin/JVM-specific ABI.

## Variants of declarations

Note that documentation itself also should have the ability to “inherit” documentation from some other declaration (from supertype or expect) \- [https://youtrack.jetbrains.com/issue/KT-88347](https://youtrack.jetbrains.com/issue/KT-88347).

Inheritance, expect-actual, and Android flavors seem very similar, so maybe those could be generalized?

* How to represent expect-actual declarations?  
  * For this, we need to collect **all** declarations present in a specific source-set and its dependencies, rather than just analyzing a file once (and collecting its declarations)in the context of the source-set where it’s located.  
  * This will require implementing deduplication logic to avoid repeating declarations.  
    * Raw prototype data (CK core module):  
      * If we go with Dokka's analysis approach (analyzing each source file once), the output is ±1 MB, since all declarations are shared.  
      * If we analyze everything in every source set, the output is 30 MB (because of all K/N targets overhead).  
      * If we do a simple deduplication (store the ID instead of the whole declaration for duplicates), the output is 3 MB (60 KB after default zip compression on Mac).  
    * Numbers may vary depending on the model's design. Current overhead arises because, for every duplicated declaration, we need to store its ID in a fragment rather than the declaration itself.  
  * Should we analyze only `leaf` source sets (e.g., `jvmMain`/`iosArm64Main`) and then automatically "infer" shared parts?  
    * Or it might not work correctly or precisely?  
    * This could allow for not analyzing shared source-sets.  
  * What about projects with two JS targets? (Is it still possible?)  
    * Do external targets somehow affect this?  
  * See also:  
    * [KT-88307](https://youtrack.jetbrains.com/issue/KT-88307) Support actualization of multiple top-level \`expect\` functions with the same \`actual\`  
    * [KT-73557](https://youtrack.jetbrains.com/issue/KT-73557) Allow refining of expect declarations for platform groups  
  * Related: what about declarations, which have the same signature in different source-sets, but no `expect`? :)  
    * It was the case at some point for some js/wasm-js declarations, at least.  
    * [https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/synchronized.html](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/synchronized.html)   
* How Android flavors/variants should work with KDM?  
  * Android variants are not the same as fragments/source-sets, but very similar to `leaf targets`.  
    * E.g., AGP has a classpath only for final variants, not for intermediate source sets.  
    * From the AGP perspective, they compile sources for each variant independently, so source sets are not as in KMP.  
  * Automatic deduplication based on the source-set structure could be nice here, as most of the code will still be in “main” (see also [https://github.com/Kotlin/dokka/issues/4472](https://github.com/Kotlin/dokka/issues/4472)).  
  * OR: should the variant be like a `source-set-tree`, and we need to produce KDM per `source-set-tree`?  
* Producing a single artifact per source-set-tree:  
  * main \= commonMain, jvmMain, nativeMain, etc.  
  * test \= commonTest, jvmTest, nativeTest.  
  * Don’t mix those in one KDM artifact.  
  * In Dokka, it’s possible to generate docs from tests, and it’s used a bit for showing what things are tested \- this is very low on usage, and there is no clear understanding of why users do this.  
    * Potentially, the use case could be similar to that of those who generate Dokka for an Android app. :)

### Links/ids to expect/actuals

Considering the fact that we could have the following situation:

```kotlin
// common code
expect class A {
  fun plus(other: A): Long
}
fun acceptsA(a: A)

/** [A.plus(A)] and [accepts(A)] in future should resolve to expect functions **/

// platform code
actual typealias A = Int

/** 
 * [A.plus(A)] and [accepts(A)] in future should resolve to expect functions
 * should also resolve, as well as 
 * [A.plus(Int)] and [accepts(Int)]
 */
```

And `expect/actual` matching can be even more complex, in the case of expect refinement and multi-expect for a single actual.

```kotlin
// common code
expect class SomeNumber {
    fun plus(other: SomeNumber): SomeNumber
    fun plus(other: Int): SomeNumber
    fun plus(other: Long): SomeNumber
}

expect class A
expect class B

expect fun foo(it: A): String
expect fun foo(it: B): String

/**
 * if we will have references to overload:
 * [SomeNumber.plus(Int)] -> resolved to single declaration
 * [SomeNumber.plus(Long)] -> resolved to single declaration
 * [SomeNumber.plus(SomeNumber)] -> resolved to single declaration
 * [SomeNumber.plus] -> resolved to three declarations
 *
 * [foo(A)] -> resolved
 * [foo(B)] -> resolved
 * [foo(Int)] -> UNRESOLVED
 */

// platform code

// `Long.plus(SomeNumber)` = `Long.plus(Long)` -> so two `expect` declarations are actualized by one
actual typealias SomeNumber = Long

actual typealias A = Int
actual typealias B = Int

// two `expect` declarations are actualized by one
actual fun foo(it: Int): String

/**
 * if we will have references to overload:
 * [SomeNumber.plus(Int)] -> resolved (common one)
 * [SomeNumber.plus(Long)] -> resolved to TWO declarations
 * [SomeNumber.plus(SomeNumber)] -> resolved to TWO declarations
 * [SomeNumber.plus] -> resolved to TWO declarations (?)
 *
 * [foo(A)] -> resolved (common one)
 * [foo(B)] -> resolved (common one)
 * [foo(Int)] -> resolved (PLATFORM one)
 */
```

How should those references then be stored in the model in both common and platform cases?

### Inheritance in the absence of a KDM artifact

Java libraries will never have KDM published either, and there could be a case where the dependency doesn't have KDM published (yet), and we need to do at least something in that case.  
JDK in this context could also be treated as a Java library without KDM published. Kotlin stdlib will expose some JDK types via Kotlin types (via mapped types), as if it were built against JDK X.

How to represent inherited/overridden declarations:

* Use different IDs: `org.example/MyException/cause` (override) and only `kotlin/Throwable/cause` (without override) stored?  
  * This might be problematic for KDoc references, as links like `[MyException.cause]` should allow redirecting to the `MyException` page, and not to `Throwable`, if the `cause` was not overridden.  
* To correctly support inherited declarations, every such declaration should be stored in the KDM, as well as have a unique ID, and some metadata/field that will distinguish between just an “inherited” declaration, in which case, documentation from the parent should be used, or an overridden one, in which case, we should use documentation from the current declaration.  
  * Potentially, just the absence of documentation on such declarations is enough for this.  
  * Though it might be harder in this case to split “explicit overrides” and “inherited” declarations during rendering, currently, Dokka has a configuration for this during building.  
  * This could also increase the size of the resulting KDM artifact, so we might need to deduplicate the same-looking overrides as well.  
  * Related to documentation inheritance and deduplication of such cases.  
* "unavailable" declarations during inheritance  
  * Should we additionally store those declarations in KDM?  
    1. E.g., see the example below with `java.util/List/addFirst`  
  * Or produce an additional KDM artifact model for such dependencies?  
* What should happen with the model when the override is added or removed?

The case with extending JDK-version dependent mapped types:

- `mappedTo` naming is TBD, maybe it should be like `actualizes` or similar  
- Overall, the same situation could happen with any other **Java library** when KDM is not available, and multiple library versions are involved. The JDK is not very specific here, but it is a good example.

```json
// stdlib KDM for List will be like this if built over JDK 8
[{
  "kind": "class",
  "id": "kotlin/List",
  "mappedTo": "java.util/List",
  "declarations": [ "prop:kotlin/List/size", "fun:kotlin/List/get", ...]
 },
 {
  "kind": "prop",
  "id": "prop:kotlin/List/size",
  "mappedTo": "fun:java.util/List/size"
  ...
 },
 {
  "kind": "fun",
  "id": "fun:kotlin/List/get",
  "mappedTo": "fun:java.util/List/get",
  ...
 },
 ...
]

// library built by JDK 21 and extends List
[{
  "kind": "class",
  "id": "org.example/List",
  "superTypes": [{"kind": "classlike", "classLikeId": "kotlin/List"})
  "declarations": [ 
    "prop:org.example/List/size",
    "fun :org.example/List/get",
    "fun :org.example/List/addFirst" // comes from JDK 21
  ]
 },
 {
  "kind": "prop",
  "id": "prop:org.example/List/size",
  "overrides": ["prop:kotlin/List/size"]
  ...
 },
 {
  "kind": "fun",
  "id": "fun:org.example/List/get",
  "overrides": ["fun:kotlin/List/get"]
  ...
 },
// here goes a method, which is added in JDK 21
 {
  "kind": "fun",
  "id": "fun:org.example/List/addFirst",
  // How should we handle this situation?
  // - there is no `kotlin/List/addFirst`
  // - there is no KDM published for `java.util/List/addFirst`
  "overrides": ["fun:java.util/List/addFirst"]
  ...
 ...
]
```

Original JDK-related questions:

* What about JDK **version-dependent** types when **extending** them, e.g., if new methods were added in JDK X? Examples: Throwable or List.  
* We probably can't really treat the JDK as a library because of built-in/mapped types, so how should it be handled? What about inheritance from `Throwable` and co.  
* **✅Preliminary resolution (for both):**  
  * Do we need to allow users to analyze against a specific version of JDK? \- **YES**  
  * **For everything else, JDK is not different from Java libraries without KDM.**

## Elements vs Declarations

We have two kinds of documented elements to cover:

* Declarations \- coming from code (API): classes, functions, properties, parameters, etc,  
* module, package, topics(future)  
  * **Re: topics**. There are multiple issues in Dokka regarding support, including additional, separate documentation in Dokka HTML ([https://github.com/Kotlin/dokka/issues/2914](https://github.com/Kotlin/dokka/issues/2914)).  
    * E.g., getting started, migration docs, and so on.  
    * Javadoc has `doc-files` (HTML/Markdown) for this.  
    * Should it be part of the renderer or model?  
    * If part of the renderer, then it might be hard to provide it to [klibs.io](http://klibs.io) and use it for different AI stuff.  
    * Also, it might be nice to allow resolving links and embedding samples in these “topics”.  
  * From where should module and package docs be taken? A file, as in Dokka?  
    * Currently, those are also analyzed per-source set, as they might contain links to declarations, and so need some “context”.  
  * Javadoc supports documentation where Kotlin doesn’t:  
    * package-info.java \- for package docs and annotations.  
    * module-info.java \- for module docs.  
  * Should we allow documenting `modules` and `packages` in the future, not via an external doc file (a.k.a. module-info.java/package-info.java)?

Rename to "Kotlin API model"? :O

* [https://github.com/Kotlin/KEEP/discussions/484\#discussioncomment-17340556](https://github.com/Kotlin/KEEP/discussions/484#discussioncomment-17340556)  
* IMO: depends on how we work with package/module/topics.  
  * If we include them, it’s no longer an API model; it's focused solely on documentation.  
* IMO2: I don’t think that we need to focus on **non**\-documentation-related cases here.

# DRAFT: PoC model structure, ideas, and questions

Note: all of those examples are currently mostly in pseudo-Kotlin-code. Real spec should be described in terms of the final serialization format.  
Note 2: Some models here have minimal documentation and comments to assess how “self-documenting” the schema is.

## Root model

Root types, which represent the KDM artifact.

```kotlin
// KDM artifact content
class KdFragments(
    // schema-version and other meta fields could go here
    val fragments: List<KdFragment>
)

// per-source-set view
class KdFragment(
    val name: String,
    val elements: List<KdElement>
    val fragmentDependencies: List<KdFragmentDependency>
)

// NOTE: we can also do the de-duplication of `expect`/`actual` this way if everything else is the same - but then there will be no way to really say, if the declaration is `expect` or `actual`
// TBD, if it's really an issue for the final user?
class KdFragmentDependency(
    val name: String,
    // list of declaration ids, which are the same as in specified `dependsOn` fragment
    val elements: List<KdElementId> = emptyList()
)
```

TBD: where to put “unavailable declarations” on the level of fragment, whole artifact, or as a separate artifact.

Alternative `top-level` view, **only** if we can say that there will always be a single declaration with a unique ID (even for expect/actual).

```kotlin
class KdElements(
    // nested map contains only those declarations, which are different in source-sets
    val elements: Map<KdElementId, Map<KdSourceSetName, KdElement>>,
)
```

`KdElement` is a base type for all elements stored in the model:

- Declarations: classes, properties, functions  
- Packages  
- Modules  
- Topics (future)

`KdElementId` is from [https://youtrack.jetbrains.com/issue/KT-88237](https://youtrack.jetbrains.com/issue/KT-88237).   
Polymorphic serialization uses a `kind` field to distinguish between different elements:

- For callables:  
  - `constructor`  
  - `function`  
  - `property`  
  - `java-field`  
  - `enum-entry`  
  - TBD: `getter` and `setter`  
- For class likes:  
  - `class`  
- `enum-class`  
- `interface`  
- `object`  
- `annotation-class`  
- `java-record`  
- `typealias`  
- Others:  
  - `module`  
  - `package`  
  - `topic`  
  - Etc.

Elements refer to other elements by ids.

```kotlin
// in class (or package)
class KdClass(
    val declarations: List<KdElementId>
)

// or for callable overrides
class KdCallable(
    // declaration, which are overriden, by this callable
    val overrides: List<KdElementId>
)
// and in all other places, where references are needed
```

## Supporting models

### Documentation

Will be covered by [https://youtrack.jetbrains.com/issue/KT-88346](https://youtrack.jetbrains.com/issue/KT-88346), here, it’s represented by placeholder type `KdDocumentation`.

### Annotations

Note: only `MustBeDocumented` annotated annotations should be included.

- `useSiteTarget` is not needed, as it’s represented based on the declaration, on which the annotation is present.

```kotlin
class KdAnnotation(
    val classLikeId: KdElementId,
    val arguments: List<KdAnnotationArgument>,
)

class KdAnnotationArgument(
    val name: String,
    val value: KdAnnotationArgumentValue,
)

sealed class KdAnnotationArgumentValue {
    class Const(public val value: KdConstValue)
    class EnumEntry(public val enumEntryId: KdElementId)
    class ClassLike(public val classLikeId: KdElementId)
    class Annotation(public val annotation: KdAnnotation)
    class Array(public val elements: List<KdAnnotationArgumentValue>)
}
```

### Constants

- For `const` properties:  
  - `const val x = 5` \-\> we could save `5` to the model.  
  - Dokka does this.  
- Annotations arguments (from above)  
- TBD: what to do with constant expressions: plain text, computed value, both?  
  - Similar for default values in parameters.  
  - Note that, theoretically, we can make resolvable declarations in those positions clickable: if the value is `”xx”.trim(),` we can resolve `trim` to a link to some function (see a similar experiment with [samples](https://jetbrains.slack.com/archives/C02H0GSH474/p1777046998751859)).  
- The main question is probably: are those a part of the “API” or not? (I think they are).

```kotlin
sealed class KdConstValue {
    object Null
    class String(public val value: kotlin.String)
    class Byte(public val value: kotlin.Byte)
    class UByte(public val value: kotlin.UByte)
    // Char, Int, Short, Long, Float, Double
}
```

### Types

TBD: definitely need some validation, if that’s correct. Constructed based on concepts in AA and Dokka.

Regarding DNN case (see [KDM spec](https://docs.google.com/document/d/18Qus_5Y5cbbrpVnx8xoo1-8nGhtqcZ9z-Q49bk1BzWE/edit?disco=AAACFlRjyqY)):

- It could represent both cases when `type-parameter` is marked as `T & Any` (including cases, where it’s a declaration coming from Java with `@NotNull` annotation \- [https://kotlinlang.org/docs/generics.html\#definitely-non-nullable-types](https://kotlinlang.org/docs/generics.html#definitely-non-nullable-types) )  
- as well as for mapped primitives, with `@NotNull` annotation:  
  - `foo(int x)`: kotlin.Int(NOT\_NULLABLE)  
  - `foo(Integer x)`: kotlin.Int(NULLABLE)  
  - `foo(@NotNull Integer x)`: [kotlin.Int](http://kotlin.Int)(DEFINITELY\_NOT\_NULLABLE)  
- For arrays:  
  - `int[]` \-\> `IntArray?`  
  - `Integer[]` \-\> `Array<Int?>?`  
  - `@NotNull Integer[]` \-\> `Array<Int>?` (see [Chapter 9\. Interfaces](https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-9.7.4) regarding the syntax for arrays annotations 🙂)  
- Is it abuse/confusing? DNN here can be renamed to something, which will not be DNN specific.

```kotlin
class KdTypeProjection(
    val type: KdType?, // if null -> star
    val variance: KdTypeVariance?
)

enum class KdTypeVariance { IN, OUT }

enum class KdTypeNullability {
    NULLABLE,
    NOT_NULLABLE,
    DEFINITELY_NOT_NULLABLE
}

sealed class KdType

class KdClassLikeType(
    val classLikeId: KdElementId,
    val typeArguments: List<KdTypeProjection>,

    val nullability: KdTypeNullability,
    val annotations: List<KdAnnotation>
) : KdType()

class KdFunctionalType(
    val returnType: KdTypeProjection,
    val receiverType: KdTypeProjection,
    val valueParameterTypes: List<KdTypeProjection>,
    val contextParameterTypes: List<KdTypeProjection>,

    val isSuspend: Boolean = false,

    val nullability: KdTypeNullability,
    val annotations: List<KdAnnotation>
) : KdType()

class KdTypeParameterType(
    val name: String,

    val nullability: KdTypeNullability,
    val annotations: List<KdAnnotation>
) : KdType()

object KdDynamicType : KdType()

class KdFlexibleType(
    val lowerBound: KdType,
    val upperBound: KdType
) : KdType()

// do we need it?
class KdUnresolvedType(
    val message: String,
) : KdType()
```

### Parameters

Should we have "parameters" as elements (`KdElement`) \- e.g., so that it might be possible to reference them in future versions of KDoc? Currently, only the parameters of the current declaration can be referenced in the declaration documentation.  
(There is some minor demand for it.)  
In case of parameters as elements, then parameters will need to have a stable ID: based on index or name? Neither will be that stable.

```kotlin
class KdTypeParameter(
    val name: String,

    val isReified: Boolean,

    val upperBounds: List<KdType>,
    val variance: KdTypeVariance?,

    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation
)

class KdReceiverParameter(
    val type: KdType,

    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation
)

class KdContextParameter(
    val name: String?,
    val type: KdType,

    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation
)

class KdValueParameter(
    val name: String,
    val type: KdType,

    val isNoinline: Boolean,
    val isCrossinline: Boolean,
    val isVararg: Boolean,

    val defaultValue: KdParameterDefaultValue?,

    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation
)

sealed class KdParameterDefaultValue {
    class Const(public val value: KdConstValue)

    // TBD if that's needed
    // complex expression - link to parameter, function call, etc
    // it's really hard to support all possible cases here
    //  but it should be rather easy to support simple expressions like `TYPE.FUNCTION(1, x)` or `TYPE.ENUM` or `CLASS::`
    class Expression(...)
}

// used in declarations
// receiver+value+context could be combined, but from the API side, it might be easier to have them separate
val receiverParameter: KdReceiverParameter?
val valueParameters: List<KdValueParameter>
val contextParameters: List<KdContextParameter>

val typeParameters: List<KdTypeParameter>
```

### Throws/returns

- Checked exceptions are combined with the `@throws` tag.  
  - OK if we represent only the Kotlin API.  
- Combines: return type \+ return tag, as well as checked exceptions and throw tag.  
- `@Throws` annotation also converted into it?  
  - 

```kotlin
// both kdoc/javadoc from @throws tag and java `throws` keyword
class KdThrows(
    val classLikeId: KdElementId,
    val documentation: KdDocumentation
)

// return type + @return tag
class KdReturns(
    val type: KdType,
    val documentation: KdDocumentation
)

// then in declaration
val returns: KdReturns
val throws: List<KdThrows>
```

### Deprecations

Based on the discussion in the comment above [KDM spec](https://docs.google.com/document/d/18Qus_5Y5cbbrpVnx8xoo1-8nGhtqcZ9z-Q49bk1BzWE/edit?disco=AAACFlRjyp0), it might make sense to represent the concept of deprecations as a separate entity rather than as an annotation (similar to `Throws/returns` handling) because it will combine both Java annotation, Javadoc tag, as well as Kotlin annotation.

- Java’s `forRemoval` maps into `level`.  
- Kotlin’s `message` is parsed as KDoc (?)

```kotlin

class KdDeprecationStatus(
    val since: String?,
    val message: KdDocumentation,
    val level: KdDeprecationLevel,
    val replaceWith: KdReplaceWith?,
)

// no hidden, as it should be filter-out
enum class KdDeprecationLevel { WARNING, ERROR }

// TBD: this is very similar to the representation of the sample, so we might want to think, if we can re-use the same concept for both.
// also, potentially, we can make code clickable there too
class KdReplaceWith(
    val imports: List<String>,
    val code: ...
)

// and used in declarations
val deprecationStatus: KdDeprecationStatus? // null if not deprecated
```

Note that Kotlin also has OptIn/RequiresOptIn/SubclassOptIn, which will still be represented by annotations, but might need to have their “message” to also have rich text, like with a deprecation message.

### Since

There are three ways to mark something as available starting from some “version”:

- `@since` tag in both Javadoc and KDoc  
- `@SinceKotlin` annotation, which is an internal annotation for stdlib, which is affected by `api-version` compiler argument \- for “documentation” purposes, it’s the same as `@since`  
- `@IntroducedAt` annotation (for parameters), which says, that some parameter was introduced in version X and so during compilation overloads will be generated to preserve ABI \- for “documentation” purposes, it’s the same as `@since`

TBD: it might make sense to combine all three places for declaring “versions” in a single field for consistency and easier further processing on the rendering side.

### Source information

- How will the renderer generate source links for declarations?  
- Should we link declarations in kdoc.jar to declarations in sources.jar?  
- No **absolute** paths in KDM.

```kotlin
class KdSource(
    val language: KdSourceLanguage,
    // filePath/line/column are only needed if we want to references to sources.jar
    // or, `fileName` might be used for Java-api approximation
    val filePath: String?,
    val line: Int,
    val column: Int,
    // e.g. GitHub sources URL - for source links feature
    val url: String?
)

enum class KdSourceLanguage {
    KOTLIN, JAVA
    // in future, in case we will start support direct interop with other languages
    // C/OBJ_C - cinterop
    // TYPE_SCRIPT - dukat generated
}
```

An alternative for source links is to generate an additional mapping and put it alongside the main KDM artifact:

```kotlin
class KdSourceLinks(
    val links: Map<KdElementId, URI>
)
```

A similar approach with a map can be used for external links on the rendering side (related to [https://youtrack.jetbrains.com/issue/KT-88421](https://youtrack.jetbrains.com/issue/KT-88421)).

### Minor things

```kotlin
enum class KdVisibility {
    PUBLIC, PROTECTED, INTERNAL, PRIVATE,

    // java specific visibilities
    // should we prefix them with `JAVA_*`?
    PACKAGE_PROTECTED, PACKAGE_PRIVATE
}

enum class KdModality {
    FINAL, SEALED, OPEN, ABSTRACT;
    // non-sealed in java?
}
```

- `isExpect` / `isActual` flags? Do we need them?

## Element models

### Class/interface/object/enum/annotation

- Kind(class/interface/etc) vs flags(value/data/etc) \- is there a clear distinction between them?

```kotlin
class KdClass(
    val id: KdElementId,
    val name: String,

    val isCompanion: Boolean,
    val isData: Boolean,
    val isValue: Boolean,
    val isFun: Boolean,
    val isInner: Boolean,
    val isExternal: Boolean,

    val superTypes: List<KdType>,
    val declarations: List<KdElementId>,
    val typeParameters: List<KdTypeParameter>,

    val visibility: KdVisibility,
    val modality: KdModality,

    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation,
    val source: KdSource,
)
```

### Typealias

```kotlin
class KdTypealias(
    val id: KdElementId,
    val name: String,

    val underlyingType: KdType,
    val typeParameters: List<KdTypeParameter>,

    val visibility: KdVisibility,

    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation,
    val source: KdSource,
)
```

### Property (and Java synthetic property):

- How to represent that “property is from the primary constructor”?  
  - The primary constructor and its properties are rendered differently in HTML: inline with class declaration.  
  - Java records also have “canonical constructors” \- same?  
- Properties & java synthetic properties & fields:  
  - Getters and setters could have different visibility levels, so should we have them? They could also have separate annotations.  
  - We can't explicitly document getters or setters in Kotlin.  
    - In Java, we only have getters and setters, with potentially different documentation for each.  
  - A Java synthetic property can have a field and get/set methods. Kotlin with EBH can also have different field types.  
    - Should we store the `field`? It’s private, so probably not.  
  - How to correctly work with java field+getField+setField based on different visibilities \- does FE know enough about it?  
- What should go into the property, and what should go into `getter`/`setter`:  
- Can parameters/returns/throws be accessed only via getters/setters?

```kotlin
class KdProperty(
    val id: KdElementId,
    val name: String,
    
    val isCompanion: Boolean,
    val isExternal: Boolean,
    // flag vs reference to constructor?
    val isFromPrimaryConstructor: Boolean, // and java records canonical constructor

    val getter: KdElementId,
    val setter: KdElementId?,
    val overrides: List<KdElementId>,
    val typeParameters: List<KdTypeParameter>,

    val visibility: KdVisibility,
    val modality: KdModality,

    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation,
    val source: KdSource,
)
```

### Java field/enum entry/`const` property

- similar to a property, but without a getter/setter?

```kotlin
class KdVariable(
    val id: KdElementId,
    val name: String,

    val isCompanion: Boolean,

    val constValue: KdConstValue?,

    // no parameters at all
    val returns: KdReturns,
    val typeParameters: List<KdTypeParameter>,

    val visibility: KdVisibility,
    // val modality: KdModality, - always final?

    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation,
    val source: KdSource,
)
```

### Constructor

```kotlin
class KdConstructor(
    val id: KdElementId,
    val name: String,

    // also covers `isCanonical` for java records
    val isPrimary: Boolean,
    val isExternal: Boolean,

    val returns: KdReturns, // might be confusing, but we can have `@return` there
    val throws: List<KdThrows>,
    val valueParameters: List<KdValueParameter>,
    val typeParameters: List<KdTypeParameter>,

    val visibility: KdVisibility,
    val modality: KdModality,

    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation,
    val source: KdSource,
)
```

### Function (+ getter/setter?)

```kotlin
class KdFunction(
    val id: KdElementId,
    // in case we will have `getter` and `setter` - should it have a name?
    val name: String,

    val isCompanion: Boolean,
    val isExternal: Boolean,
    val isSuspend: Boolean,
    val isOperator: Boolean,
    val isInfix: Boolean,
    val isInline: Boolean,
    val isTailRec: Boolean,

    val overrides: List<KdElementId>,
    val returns: KdReturns,
    val throws: List<KdThrows>,
    val valueParameters: List<KdValueParameter>,
    val receiverParameter: KdReceiverParameter?,
    val contextParameters: List<KdContextParameter>,
    val typeParameters: List<KdTypeParameter>,

    val visibility: KdVisibility,
    val modality: KdModality,

    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation,
    val source: KdSource,
)
```

### Package

```kotlin
class KdPackage(
    val id: KdElementId,
    val name: String,

    val declarations: List<KdElementId>,

    // in theory, we can also expose here information if the packages is exposed or not in `package-info.java`, though, no one asked about it yet

    val annotations: List<KdAnnotation>, // java only right now
    val documentation: KdDocumentation,
    val source: KdSource, // package-info.java?
)
```

### Module

```kotlin
class KdModule(
    val id: KdElementId,
    val name: String,

    val packages: List<KdElementId>,

    val annotations: List<KdAnnotation>, // java only right now
    val documentation: KdDocumentation,
    val source: KdSource, // module-info.java?
)
```

### Topics (future)

```kotlin
class KdTopic(
    val id: KdElementId,
    val name: String,

    val documentation: KdDocumentation,
    val source: KdSource, // ???
)
```

## Backward/forward compatibility guarantees

- Similar to Kotlin: *always* backward, 1 forward.  
- The model includes a version field.  
  - Just a number, like with compiler args JSON.  
  - TBD: how others do this.  
- Adding new optional fields is a non-breaking change.  
- Adding a “enum”/”kind” is a non-breaking change (should be documented).  
- Removing or changing the semantics of existing fields is a breaking change that requires a version bump (ideally, never).  
- The renderer should be able to work with models produced by both older and newer analyzers (within the same major schema version).  
  - Old renderers ignore unknown fields.  
  - backward/forward compat tests needed.

Need to think through scenarios:

- What happens when a new language feature adds a new declaration kind?  
- When is a new KDoc tag introduced?  
- When does the model structure need to be reorganized?  
- Think about future features, like rich errors, and how they will affect the model \- ask the LE team.

