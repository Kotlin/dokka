KDM model proposal
KEEP: https://github.com/Kotlin/KEEP/discussions/484

All of those code samples are currently mostly in pseudo-Kotlin-code. Real spec should be described in terms of the final serialization format.

There are two candidates: JSON and ProtoBuf.
There are two main factors that we need from the serialization format:
It should have decent enough performance/size/memory characteristics, so that it will not be a bottleneck for both consumption and generation.
It should have schema support so that any consumer can integrate with it.

Both of the formats are good enough here, and other differences are mostly about taste:
JSON
Easy to read, easy to use.
A lot of tools (and agents) know how to work with it
Literally all languages support it.
Stable kotlinx-serialization support.
Good size when compressed.
There is JSON schema support, though it’s not as popular as proto schemas.
Encoding of strings is not an issue (e.g. from CJK), as JSON per spec (https://www.rfc-editor.org/info/rfc8259/#section-8) works with UTF-8
ProtoBuf
A bit smaller than JSON, but since the biggest contributors of the information are text (documentation, ids), the win isn't that big: Data from experiments:
JSON without docs = 0.2 MB (just models, baseline)
JSON = 1.3 MB, zipped = 0.14 MB
ProtoBuf = 0.8 MB, zipped = 0.16 MB
Potentially faster and uses less memory to encode/decode than JSON.
Schema-based - most of the tools can use a schema to generate parsing.
There is also ProtoJSON, which could be used for serialization, while using the proto schema.
See also https://github.com/bufbuild/protoschema-plugins as an example of an option, where the proto spec could be a source or truth, while the JSON schema could be generated based on it.

In both cases, kotlinx-serialization can be used as an implementation detail, though because of differences in how polymorphism is handled, it’s more verbose with ProtoBuf, so using the same model as in JSON will require custom serializer implementations.

So far, JSON seems like a better choice because it’s simpler, maps more closely to kotlinx-serialization, and integrates more easily with tools.

Open questions:
Well, is JSON good enough, or are there any concerns?
Core model
KdElement is a base type for all elements stored in the model:
Declarations: classes, properties, functions
Packages
Modules
Topics (future)
KdElementId is from https://youtrack.jetbrains.com/issue/KT-88237 - a unique, opaque text ID used to reference elements. See KDM spec

Polymorphic serialization uses a kind field to distinguish between different elements (e.g., JSON, Protobuf oneof). Preliminary kinds (could be renamed before release):
For callables:
constructor
function
property & java-synthetic-property
java-field
Enum-entry
getter & setter
For class likes:
class
enum-class
interface
object
annotation-class
java-record
typealias
Specific elements:
refinedElement
inheritedMember
Others:
module
package
topic
Etc.

KdBundle is a root type and so represents a KDM artifact content. It represents a so-called “source-set-tree”.

class KdBundle(
val schemaVersion: String, // aligned with Kotlin version produced the bundle
val fragments: List<KdFragment>,
val expectActualMappings: List<KdExpectActualMapping>
)

Every declaration is part of KdFragment, which is a per-source-set view of all declarations accessible in that source set. E.g., all declarations from commonMain will also be present in the jvmMain fragment.
typealias KdFragmentName = String

class KdFragment(
val name: KdFragmentName,
val elements: Map<KdElementId, KdElement>,
val embeddedElements: Map<KdElementId, KdElement>
)

Open questions:
Should the fragment include information about the platform/target (e.g., jvm, wasm-wasi, ios-arm64, etc) or should consumers use information from PSM JSON?
E.g., if we pack KDM into .kar, it’s already there.
Fragment names = source-set names for the KMP case.

The default source-set-tree is main, which includes source sets such as commonMain, jvmMain, nativeMain, etc. Though there are cases in which users want to generate an API reference website for test trees (test = commonTest, jvmTest, nativeTest, etc.) to share information with colleagues, or those might even include some custom processing, like inclusion of links to screenshots via custom Dokka plugins, to show a “preview” of what the test does. There is no real example for this usage, though there are also users who generate API reference for Android apps with similar “use-cases”.
Source-sets from different trees should not be included in a single KdBundle.

Apart from KMP, there are also Android flavors/variants, which follow similar code-sharing capabilities via source-sets, but without the expect-actual mechanism:
E.g., AGP has a classpath only for final variants, not for intermediate source sets.
From the AGP perspective, they compile sources for each variant independently.
Automatic deduplication based on the source-set structure could be nice here, as most of the code will still be in “main” (see also https://github.com/Kotlin/dokka/issues/4472).
So this should be natively supported by the current model.
De-duplication of elements
Because of the fact that we need to store every declaration in each fragment to be correct KMP-wise, it will increase the size of the final artifact a lot. Though in most of the cases, the same declaration in all fragments will be the same. Apart from reducing the size, it will also significantly simplify the model's consumption, as there will be no need to check whether anything differs between fragments when the declaration is identical. That’s why we can easily de-duplicate this information by having a specific KdElement which only has a reference to the other dependsOn fragment:

class KdRefinedElement(
val fragmentName: KdFragmentName
): KdElement

In addition to this, we can also perform the same de-duplication for inherited members that were not explicitly overridden, and have only a single overridden declaration, as well as for “aliased” declarations, as for typealiases, we need to have all of the declarations present explicitly in the model for expect-actual mapping.

class KdInheritedMember(
val originalId: KdElementId
): KdElement

Open questions:
How to properly name those elements for serialization?
Embedded elements
embeddedElements represent elements that are not directly available in this fragment, but come from dependencies. Those are required in case no KDM artifact exists for those declarations: Java libraries, the JDK, or Kotlin libraries without a KDM published. Those declarations should be used only to retrieve additional information, not to render them on the page; that’s why they are in a separate map.
Examples of such embedded declarations are:
Supertypes - to render their names or relations, as well as generate external links
Inherited member links - to render/link overrides, as well as generate external links
Declarations linked in KDoc - to generate an external link to e.g., Javadoc, which requires knowledge of parameter types of functions.

As a concrete example, java.util/List/addFirst could be an embedded declaration added in JDK 21. It might not be available in the JDK that the stdlib was built against, so it would be missing from the generated KDM artifact.

Open questions:
Are there any other alternative ways to solve the problems?
Example:

// stdlib KDM for List will be like this if built over JDK 8
elements: {
"kotlin/List": {
"kind": "class",
"mappedTo": "java.util/List",
"declarations": [ "prop:kotlin/List/size", "fun:kotlin/List/get", ...]
},
"prop:kotlin/List/size": {
"kind": "prop",
"mappedTo": "fun:java.util/List/size"
},
"fun:kotlin/List/get": {
"kind": "fun",
"mappedTo": "fun:java.util/List/get",
}
}

// library built by JDK 21 and extends List
elements: {
"org.example/List": {
"kind": "class",
"superTypes": [{"kind": "classlike", "classLikeId": "kotlin/List"})
"declarations": [
"prop:org.example/List/size",
"fun :org.example/List/get",
"fun :org.example/List/addFirst" // comes from JDK 21
]
},
"prop:org.example/List/size": {
"kind": "prop",
"overridens": ["prop:kotlin/List/size"]
},
"fun:org.example/List/get": {
"kind": "fun",
"overrides": ["fun:kotlin/List/get"]
},
"fun:org.example/List/addFirst": {
"kind": "fun",
// links to embbeded element, which is added in JDK 21
"overridens": ["fun:java.util/List/addFirst"]
}
},
embeddedElements: {
"fun:java.util/List/addFirst": {
"kind": "fun",
...
}
}

Expect-actual mapping
An ID of an element is unique in a fragment, but could be different across different fragments for the same declaration in some cases of expect/actual matching.
KdExpectActualMapping represents the mapping of expect and actual declarations across fragments. While in most of the cases, there will be a one-to-one mapping with the same ID, it’s not always the case:
class KdExpectActualMapping(
val expectIds: Map<KdFragmentName, KdElementId>,
val actualIds: Map<KdFragmentName, KdElementId>,
)

An alternative serialization representation could be used to minimize ID repetition, since it’s the same in most cases. Though it might be less clear for complex cases:
typealias KdExpectActualMapping =
Map<KdElementId, Map<KdFragmentName, KdExpectActualRelation>>

enum class KdExpectActualRelation {
EXPECT, ACTUAL
}

Open questions:
See examples and help to decide on representation (or propose another one).
Examples
Note: the following examples are using simple IDs, not the ones that will be used in the final model.
Example for a simple case:
// common
expect fun foo(arg: Int)
// apple
actual fun foo(arg: Int) {}
// jvm
actual fun foo(arg: Int) {}

// KDM mapping
{
expectIds: {
common: "foo/Int",
},
actualIds: {
apple: "foo/Int",
jvm: "foo/Int",
}
}
// alternative mapping
{
"foo/Int": {
common: "EXPECT",
apple: "ACTUAL",
jvm: "ACTUAL"
}
}
Example for a case with actual typealias and different IDs:
// common
expect class A
expect fun foo(arg: A)
// apple
actual class A
actual fun foo(arg: A) {}
// jvm
actual typealias A = Int
actual fun foo(arg: Int) {}

//---
// what if, we actualize one declaration in shared source-set
// and then another one in the leaf one:
// native
actual typealias A = Int
// expect fun foo(arg: A) <- treated like `expect fun(A)`, no typealias expansion

// ios
actual fun foo(arg: Int)
//---

// KDM mapping for `fun foo`
{
expectIds: {
common: "foo/A",
},
actualIds: {
apple: "foo/A",
jvm: "foo/Int",
}
}
// alternative mapping
{
"foo/A": {
common: "EXPECT",
apple: "ACTUAL",
},
"foo/Int": {
jvm: "ACTUAL"
}
}

Example for multiple expect declarations (KT-73557):
// common
expect class A
// native
@ExpectRefinemnt expect class A
// apple
actual class A
// jvm
actual class A

// KDM mapping
{
expectIds: {
common: "A",
native: "A",
},
actualIds: {
apple: "A",
jvm: "A",
}
}
// alternative mapping
{
"foo/A": {
common: "EXPECT",
native: "EXPECT",
apple: "ACTUAL",
jvm: "ACTUAL"
}
}

Example for a case, with a single actual for multiple expect declarations (KT-88307)

// common
expect class A
expect class B
expect fun foo(arg: A)
expect fun foo(arg: B)
// apple
actual class A
actual class B
actual fun foo(arg: A) {}
actual fun foo(arg: B) {}
// jvm
actual typealias A = Int
actual typealias B = Int
actual fun foo(arg: Int) {}

// KDM mapping for `fun foo(A)`
{
expectIds: {
common: "foo/A",
},
actualIds: {
apple: "foo/A",
jvm: "foo/Int",
}
}
// alternative mapping
{
"foo/A": {
common: "EXPECT",
apple: "ACTUAL",
},
"foo/Int": {
jvm: "ACTUAL"
}
}

// KDM mapping for `fun foo(B)`
{
expectIds: {
common: "foo/B",
},
actualIds: {
apple: "foo/B",
jvm: "foo/Int", // same ID as for the mapping above
}
}
// alternative mapping
{
"foo/B": {
common: "EXPECT",
apple: "ACTUAL",
},
"foo/Int": { // same ID as for the mapping above
jvm: "ACTUAL"
}
}
Supporting models
Documentation
Will be covered by https://youtrack.jetbrains.com/issue/KT-88346 (KDoc), here, it’s represented by placeholder type KdDocumentation.
Modifiers
Visibility and modality are shared concepts across all of the declarations:
enum class KdVisibility {
PUBLIC, PROTECTED, INTERNAL, PRIVATE,
// java specific visibilities
PACKAGE_PROTECTED, PACKAGE_PRIVATE,

    // in future
    SHARED_INTERNAL
}

enum class KdModality {
FINAL, SEALED, OPEN, ABSTRACT
}

Open questions:
Should we include PRIVATE visibility? In general, for documentation purposes, it doesn’t make sense, though Javadoc and Dokka allow the inclusion of private declarations in the output.
OUT OF SCOPE in initial version.
Expressions and constants
In general, expressions (like bodies of functions) and their computed values are not a part of the API of a library, though there are some exceptions.
There are three different places where those are significant:
Constants: const val x = 5 - the value of the constant could be considered as part of an API.
Annotation arguments: those are values, which are similar to constants in a way, in that they are computed at compile-time, and so could also be considered as part of an API.
Default values of function parameters: in general, they can be used to avoid unnecessary overloads, especially for optional arguments, and, in the absence of a value, the default value of the parameter can be considered part of the API.

In addition, at least for default values, it might be beneficial to also allow linking to the declarations used there, such as constants, to provide a better experience when exploring the API with default parameters (see https://github.com/Kotlin/dokka/issues/4070).
There are also other places in the model where a similar concept of an expression is needed:
ReplaceWith.expression represents a code fragment that will be inlined by the IDE.
Sample/snippet body: Javadoc allows for manual inclusion of links in snippets, and it’s also possible to do automatic resolution of links to declarations in samples in Kotlin (see experiment).

KdExpression is a Kotlin code fragment with optional links attached, used for both places where constants are used, as well as for default parameters. Links are modeled as a separate entity so that the text will continue to represent real Kotlin code and can be easily highlighted as such, even without linking capabilities.
KdConstValue and KdConstExpression should be used only for compile-time-known values.
class KdExpression(
val text: String,
val links: List<KdExpressionLink>
)

class KdExpressionLink(
// offset in `KdExpression.text`
val start: Int,
val end: Int,
val elementId: KdElementId
)

sealed class KdConstValue {
object Null
class Boolean(val value: kotlin.Boolean)
class String(val value: kotlin.String)
class Byte(val value: kotlin.Byte)
class UByte(val value: kotlin.UByte)
// Char, Int, Short, Long, Float, Double
}

class KdConstExpression(
val value: KdConstValue,
val expression: KdExpression?,
)

Open questions:
Should we store expressions for constants, in addition to their computed values?
Should we have a flag to control the inclusion of expressions in the generated model, since those could contain “implementation details”/internal calls?
Should we support linking to resolved declarations in expressions?
Maybe only for replaceWith and samples?
How does linking here relate to linking in KdDocumentation?
Examples:

// expression = "HELLO".lowercase()
// value      = "hello"
const val x = "HELLO".lowercase()
// expression = 8 * 1024
// value      = 8192
const val BUFFER_SIZE = 8 * 1024
// non-const property initializer is an implementation detail
// expression = 5
// value      = null - not a constant, can't get computed value here
val y = 5

fun x(
// expression = "HELLO"
// value      = "HELLO" - literal
x1: String = "HELLO",

// expression = "HELLO".lowercase()
// value      = null - not a constant, can't get computed value here
x2: String = "HELLO".lowercase(),

// expression = BUFFER_SIZE
// value      = 8192 - uses constant value
x3: Int = BUFFER_SIZE, // ref to `const val`

// expression = BUFFER_SIZE * 2
// value      = null - not a constant, can't get computed value here
x4: Int = BUFFER_SIZE * 2,

// expression = bufferSize()
// value      = null - not a constant, can't get computed value here
x5: Int = bufferSize(),

// expression = 8 * 1024
// value      = null - not a constant, can't get computed value here
x6: Int = 8 * 1024,

// expression = 8192
// value      = 8192 - literal
x7: Int = 8192,

)

// expression = b
@Deprecated("", ReplaceWith(expression = "b"))
fun a()
Annotations
Only MustBeDocumented annotated annotations should be included in the KDM output.
useSiteTarget shouldn’t be stored, as it’s represented based on the declaration, on which the annotation is present.
class KdAnnotation(
val classLikeId: KdElementId,
val arguments: Map<String, KdAnnotationArgumentValue>,
)

sealed class KdAnnotationArgumentValue {
class Const(val value: KdConstExpression)
class EnumEntry(val enumEntryId: KdElementId)
class Classlike(val classLikeId: KdElementId)
class Annotation(val annotation: KdAnnotation)
class Array(val elements: List<KdAnnotationArgumentValue>)
}

Annotation arguments are represented as a map, and include only explicitly specified parameters in the sources. Default values of annotations might be missing during resolution, as well as those not used in the current Dokka rendering; this is expected.
⚠️Types
sealed class KdTypeProjection {
class Invariant(val type: KdType): KdTypeProjection
class In(val type: KdType): KdTypeProjection
class Out(val type: KdType): KdTypeProjection
object Star: KdTypeProjection
}

// for A<X>.B<Y>
class KdClassTypeQualifier(
val name: String,
val typeArguments: List<KdTypeProjection>,
)

sealed class KdType

class KdClassLikeType(
val qualifiers: List<KdClassTypeQualifier>,
val classLikeId: KdElementId,

    val isMarkedNullable: Boolean,
    // `true` in case it's a Java declaration
    // annotated with @NotNull/@Nullable
    // can be absent/false otherwise (absent, to reduce size)
    val isEnhancedNullability: Boolean,
    val annotations: List<KdAnnotation>
) : KdType()

// (A) -> B
class KdFunctionalType(
val returnType: KdTypeProjection,
val receiverType: KdTypeProjection?,
val valueParameterTypes: List<KdTypeProjection>,
val contextParameterTypes: List<KdTypeProjection>,

    val isSuspend: Boolean,

    val isMarkedNullable: Boolean,
    val annotations: List<KdAnnotation>
) : KdType()

class KdTypeParameterType(
val name: String,
// id of a class or a function, this type-parameter is part of
val containerElementId: KdElementId,

    val isMarkedNullable: Boolean,
    val annotations: List<KdAnnotation>
) : KdType()

// dynamic (JS)
object KdDynamicType : KdType()

// String! or (Mutable)List!
class KdFlexibleType(
val lowerBound: KdType,
val upperBound: KdType
) : KdType()

// T & Any
class KdDefinitelyNotNullType(
val original: KdType
): KdType()

// rich-errors
class KdErrorUnionType(
// in case of `Error1 | Error2` the primary type is `Nothing`
val primaryType: KdType,
val errorTypes: List<KdType>
): KdType()

Open questions:
Primitives (Java interop). How to represent the following:
foo(int x): KdPrimitiveType(kotlin.Int)
foo(Integer x): KdFlexibleType(kotlin.Int..kotlin.Int?)
foo(@NotNull Integer x): KdPrimitiveType(kotlin.Int)
The current proposal uses DNN for type parameters (T & Any) and KdPrimitiveType for  primitives (@NotNull Integer x) both coming from Java and Kotlin.
See examples above
Should flexible types have annotation, or is having them only on the lower/upper bound enough?
No need for having annotation there
Examples:
Java primitives and boxes are represented as a KdClassType with different flags, depending on the nullability involved:
// Java

// int
KdClassType(
kotlin.Int
isMarkedNullable      = false
isEnhancedNullability = false
)
// Integer
KdFlexiblType(
lowerBound = KdClassType(
kotlin.Int
isMarkedNullable      = false
isEnhancedNullability = false
)
upperBound = KdClassType(
kotlin.Int
isMarkedNullable      = true
isEnhancedNullability = false
)
)
// @NotNull Integer
KdClassType(
kotlin.Int
isMarkedNullable      = false
isEnhancedNullability = true
)
// @Nullable Integer
KdClassType(
kotlin.Int
isMarkedNullable      = true
isEnhancedNullability = true
)

// Kotlin

// Int
KdClassType(
kotlin.Int
isMarkedNullable      = false
isEnhancedNullability = false
)

// Int?
KdClassType(
kotlin.Int
isMarkedNullable      = true
isEnhancedNullability = false
)

Parameters
There are 4 kinds of parameters, which could be added to different declarations:
Type parameters
Extension receiver parameter
Context parameters
Regular value parameters
Those are not separate elements and can’t be referenced anywhere except in the declaration itself and its documentation, so they are embedded right in the declaration. Theoretically speaking, we can make them referencable in KDoc via some new syntax, but neither the name of the parameter nor its index in the parameter list is stable enough to be considered stable when referencing outside of the declaration itself.
Though they could have annotations or documentation (via @param tag) attached to them.

class KdTypeParameter(
val name: String,

    val isReified: Boolean,

    val upperBounds: List<KdType>,
    val variance: KdTypeVariance?,

    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation
)

class KdExtensionReceiverParameter(
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

    val defaultValueExpression: KdDefaultValueExpression?,

    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation
)

sealed class KdDefaultValueExpression {
class Defined(val expression: KdExpression)
class Inherited(val elementId: KdElementId)
}

Open questions:
What should be the value of the defaultValueExpression for inherited/overriden and actual declarations?
See also https://github.com/Kotlin/dokka/issues/4339
See KdDefaultValueExpression for inheritance handling. For expect/actual cases, we can copy the information from an expect in the same way; we copy documentation from expect.
Special tags and annotations
There are multiple things in Kotlin and Java that may appear both in KDoc/Javadoc and as annotations on declarations.
Throws
In Java, it’s possible to declare checked exceptions via throws X in the declaration signature, as well as later document them via @throws Javadoc tag.
In Kotlin, there are no checked exceptions, but the same concept can be represented with the @Throws annotation or documented separately via the @throws KDoc tag.
All those concepts for documentation purposes are represented as a property val throws: List<KdThrows> on callable declarations.
class KdThrows(
val classLikeId: KdElementId,
val documentation: KdDocumentation
)
Deprecations & Opt-ins
Deprecations follow a similar strategy. In Java, it’s possible to annotate declarations with @Deprecated as well as use the @deprecated Javadoc tag. In Kotlin, there is only the @Deprecated annotation. Though annotations have different arguments:
In Kotlin:
message - can be parsed using KDoc Markdown syntax (https://github.com/Kotlin/dokka/issues/2635).
level
replaceWith
In Java:
forRemoval - can be mapped to the Kotlin level property.
forRemoval doesn't influence the compiler behavior - WARNING should always be used.
since
In Kotlin stdlib, there is also  DeprecatedSinceKotlin(warningSince = "1.3", errorSince = "2.5"), which could be used for getting an additional since information.
Considering all this, the deprecation is represented as follows:

class KdDeprecated(
val since: String?,
val message: KdDocumentation,
val level: KdDeprecationLevel,
val replaceWith: KdReplaceWith?,
)

// no hidden, as it should be filter-out
enum class KdDeprecationLevel { WARNING, ERROR }

class KdReplaceWith(
val imports: List<String>,
val code: KdExpression
)

Open questions:
Should the opt-in mechanism also be represented similarly as deprecations?
NO NEED FOR THIS
There are three annotations overall: OptIn/RequiresOptIn/SubclassOptIn.
Related Dokka issue: https://github.com/Kotlin/dokka/issues/3439
We can parse the RequiresOptIn.message content as KDoc, allowing rich text there, as with Deprecated.message.
Though it might not bring many benefits, as annotations (like DelicateCryptographyApi) can have a KDoc attached to them.
Here comes the question: what is the difference between a message and a KDoc on annotation for the end user of the library?
The main difference between Opt-in and deprecations is that the Opt-in message is per annotation, whereas the deprecated message granularity is per declaration.
Since
There are three ways to mark something as available starting from some “version”:
@since tag in both Javadoc and KDoc.
@SinceKotlin annotation, which is an internal annotation for stdlib, which is affected by api-version compiler argument - for “documentation” purposes, it’s the same as @since
@IntroducedAt annotation (for parameters), which says that some parameter was introduced in version X, and so during compilation overloads will be generated to preserve ABI - for “documentation” purposes, it’s the same as @since
All of those concepts should be mapped to a single since: String? property on declarations and parameters.
Additionally, with published KDM artifacts, it will be possible to obtain historical information about every declaration by comparing the produced artifacts. We could also embed or validate the since value when building the next version of the KDM artifact based on this information.
⚠️Files & Source information
There are three potential use cases for file information to be included in the KDM:
Source links (currently supported by Dokka)
For this, we only need to store a URL like this: https://github.com/JetBrains/kotlin/tree/2.4.20/libraries/stdlib/src/kotlin/Annotations.kt#L79
Linking to declarations in sources.jar (not supported, but possible with Dokka)
E.g., this could allow optional embedding of source code in the generated API documentation website, as in Rust, without including the source code in the KDM itself. (as it’s already present and published in sources.jar)
Java API approximation (needed for Google Dackka)
Specifically, Kotlin filenames and annotations are required to generate file facades.
See more context about Google use-case, as well as other options in: KDM spec
In any case, there is no need to have an absolute path to a file stored in the KDM, and that’s important. Depending on what we would like to support, the design could differ.

Example of the potential model, which will cover all three use-cases:
class KdFragment(
...
// additional (optional) field in the fragment
val files: List<KdFile>
)

class KdFile(
val name: String,
val path: String, // relative to the root of `sources.jar` in fragment
val language: KdSourceLanguage,
val annotations: List<KdAnnotation>,
val sourceInformation: Map<KdElementId, KdElementSourceInformation>,
)

class KdElementSourceInformation(
// only needed for links to `sources.jar`
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
Elements models
Class/interface/object/enum/annotation/record
class KdClass(
val name: String,

    val isCompanion: Boolean,
    val isData: Boolean,
    val isValue: Boolean,
    val isFun: Boolean,
    val isInner: Boolean,
    val isExternal: Boolean,
    val isError: Boolean, // rich-errors

    // can only be present for some of the stdlib declarations
    val mappedTo: KdElementId?,

    val typeParameters: List<KdTypeParameter>,
    val superTypes: List<KdType>,

    // includes
    // - declared member/companion callables
    // - inherited callables
    // - nested classifiers (class, typealias, companion object, etc)
    val declarations: List<KdElementId>,

    val visibility: KdVisibility,
    val modality: KdModality,

    val since: String?,
    val deprecated: KdDeprecated?,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation,
): KdElement

Open questions:
Should we split callables, constructors, and classlikes into separate fields, or do single declarations work fine in a single field?
Should we have a single model (and kind) for classes/interfaces/objects, or split them somehow?
E.g., isCompanion makes sense only for an object
Constructors make sense only for a class
enum and annotation classes are very limited in what they allow to declare
etc.
Typealias
The main thing about type aliases is that we should include “aliased” declarations in the KDM output and have a declarations field, so that the expect-actual mapping will work correctly.
class KdTypealias(
val name: String,

    val typeParameters: List<KdTypeParameter>,
    val underlyingType: KdType,

    val declarations: List<KdElementId>,

    val visibility: KdVisibility,

    val since: String?,
    val deprecated: KdDeprecated?,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation,
): KdElement
Constructor
class KdConstructor(
// also covers `isCanonical` for java records
val isPrimary: Boolean,
val isExternal: Boolean,

    // can only be present for some of the stdlib declarations
    val mappedTo: KdElementId?,

    val typeParameters: List<KdTypeParameter>,
    val valueParameters: List<KdValueParameter>,

    val visibility: KdVisibility,

    val since: String?,
    val deprecated: KdDeprecated?,
    val throws: List<KdThrows>,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation,
): KdElement
Function
The same model will also represent getter and setter, when they are present. The getter/setter model will never contain overridens.
class KdFunction(
val name: String,

    val isCompanion: Boolean,
    val isExternal: Boolean,
    val isSuspend: Boolean,
    val isOperator: Boolean,
    val isInfix: Boolean,
    val isInline: Boolean,
    val isTailRec: Boolean,

    // can only be present for some of the stdlib declarations
    val mappedTo: KdElementId?,

    val returnType: KdType,
    val typeParameters: List<KdTypeParameter>,
    val valueParameters: List<KdValueParameter>,
    val extensionReceiverParameter: KdExtensionReceiverParameter?,
    val contextParameters: List<KdContextParameter>,

    // directly overriden declarations.
    val overridens: List<KdElementId>,

    val visibility: KdVisibility,
    val modality: KdModality,

    val since: String?,
    val deprecated: KdDeprecated?,
    val throws: List<KdThrows>,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation,
): KdElement

Open questions:
Should the getter and setter models be separate?
⚠️Property
There are different “kinds” of properties with which Kotlin users interact:
Ordinary kotlin properties: have a getter, a setter, annotations on them, etc.
const properties: no getter & setter, but have compile-time value.
As well as static Java fields with primitive types and literal values (TODO: link to Java spec)
Enum entries: a very special kind of const-like properties.
Java fields: like properties, but without a getter/setter, can’t be overridden.
Java synthetic properties: combine one or two Java functions into a Kotlin property.
Note that in any case, getters and setter functions coming from Java should be represented as functions, as those could be overridden in Kotlin as functions.

getter and setter have references, only if they are non-standard:
Have annotations
Have an explicit isInline flag
Have different visibility
(potentially) Have additional documentation: currently, it’s not possible to attach KDoc to accessors.
Represents the getter/setter of a Java synthetic property
Anything else?
Those will also always be null for const val, Java fields, and enum entries.

class KdProperty(
val name: String,

    val isLateinit: Boolean, // only for `val` - ???
    val isInline: Boolean,
    val isCompanion: Boolean,
    val isExternal: Boolean,
    val isSuspend: Boolean,

    val isMutable: Boolean,
    val isFromPrimaryConstructor: Boolean, // and java records canonical constructor

    val constExpression: KdConstExpression?,
    val getter: KdElementId?,
    val setter: KdElementId?,

    // can only be present for some of the stdlib declarations
    val mappedTo: KdElementId?,

    val type: KdType,
    val typeParameters: List<KdTypeParameter>,
    val extensionReceiverParameter: KdReceiverParameter?,
    val contextParameters: List<KdContextParameter>,

    val overridens: List<KdElementId>,

    // visibility of a `getter` if it have non-default getter/setter
    val visibility: KdVisibility,
    val modality: KdModality,

    val since: String?,
    val deprecated: KdDeprecated?,
    val throws: List<KdThrows>,
    val annotations: List<KdAnnotation>,
    val documentation: KdDocumentation,
): KdElement

Open questions:
Should isFromPrimaryConstructor be a flag or a reference to the constructor?
Is it okay not to materialize the default getter/setter?
If we allow inclusion of private declarations in the model, should we have backingField here?
Is it ok that changing the visibility of Java fields might affect the generation of a Java synthetic property?
E.g., if there will be field/getField/setField and field is private -> there will be a synthetic property; but if the visibility is changed to public, there will be none, and so “breaking change”.
Package & Module
Those are concepts that aren’t directly present in the compiler as referencable elements, but are present in KDoc.
KDoc allows linking to packages.
Javadoc allows linking to both packages and modules.
Java allows documenting and annotating packages and modules via package-info.java and module-info.java
Dokka allows documenting packages and modules via a separate file (see Dokka docs)

class KdPackage(
val name: String,

    val declarations: List<KdElementId>,

    val annotations: List<KdAnnotation>, // java only right now
    val documentation: KdDocumentation
): KdElement

class KdModule(
val name: String,

    val packages: List<KdElementId>,

    val annotations: List<KdAnnotation>, // java only right now
    val documentation: KdDocumentation
): KdElement

Open questions:
How to provide package and module documentation? Is the Dokka’s approach enough for the initial implementation?
Topic (potential future extension)
Note: those will not be included in the initial version of KDM.
There are multiple issues in Dokka regarding support, including additional, separate documentation in Dokka HTML (https://github.com/Kotlin/dokka/issues/2914).
E.g., getting started, migration docs, and so on.
Javadoc has doc-files (HTML/Markdown) for this.
It would be nice to have them as part of the model in the future, so that they are distributed and versioned alongside the APIs.
If those are not part of the renderer, it might be hard to provide them to klibs.io and use them for different AI stuff.
Also, it might be nice to allow resolving links and embedding samples in these “topics”, in the same way that you write KDoc.

Very raw sketch of the possible model:
class KdTopic(
val title: String,
val path: String,

    val content: KdDocumentation
): KdElement

Having a relative path in the model could allow referencing those topics from KDocs using a new syntax like [doc:path/to/topic].
Open questions
Handling of unresolved references
There are multiple places, in KDM (and previously in Dokka), where we might fail to resolve something, and so need a strategy for handling such situations:
Types
Class references - like an annotation id, or supertype id
Links in KDocs
Unsupported expressions used in the annotation value
Non-evaluatable expressions in const positions

Those could be “unresolved” because of multiple reasons, like:
Wrong classpath: e.g., KGP missconfiguration.
Wrong visibilities: e.g., link to internal type in public kdoc
Unknown parameter reference in kdoc: e.g., @param XXX xxx on function foo()
Special types prohibited in some positions: e.g., dynamic as a receiver or supertype
Incorrect code

The first idea, of course, is to just fail model generation, because all of those could be considered bugs: in user code or in KDM analysis itself. Though not all of them are always easy to fix/workaround.
Assuming that the compilation works fine, but KDM generation is not, the library author has a tough choice on what to do on library release: publish a library without documentation, or don’t publish the library at all until the fix is made in the KDM tooling.
So, it might make sense to allow some leniency during KDM generation and emit warnings (or errors with a flag, as in the current Dokka) for such cases.

It’s an open question on how this should work, as well as how the model should be represented for such cases.
One of the problems here is that if some class/type/etc is unresolved, we can’t compute the ID, and so the renderer will not be able to understand what to render, if there is no direct name present, like in the case of annotations.
Compatibility policy
We should design a proper compatibility policy. Starting points are:
Similar to Kotlin: always backward, 1 forward.
The model includes a version field.
Just a number, like with compiler args JSON.
Kotlin version used as a starter.
Adding new optional fields is a non-breaking change.
Adding a “enum”/”kind” is a non-breaking change (should be documented).
Removing or changing the semantics of existing fields is a breaking change that requires a version bump (ideally, never).
The renderer should be able to work with models produced by both older and newer analyzers (within the same major schema version).
Old renderers ignore unknown fields.
backward/forward compat tests needed.

Need to think through scenarios:
What happens when a new language feature adds a new declaration kind?
When is a new KDoc tag introduced?
When does the model structure need to be reorganized?
Think about future features, like rich errors, and how they will affect the model - ask the LE team.
Feel free to share your thoughts and ideas as comments here




----------


KDM unique declaration identifier
TODO: KT-88237 KDM: Introduce a unique declaration identifier

Definitions
Class ID
A structure containing the package name, all outer class names in case of nested classes, and the own class name.

Implemented in the compiler by https://github.com/JetBrains/kotlin/blob/62581399bd5b1287f34b89f8bdb402eb3ba922c0/core/names/src/org/jetbrains/kotlin/name/ClassId.kt#L28.

Its string serialization uses a dot . to separate package parts, a slash to separate the package name from the class name(s) and a dot . to separate outer class names.

Example:

foo.bar/Baz.Qux
corresponds to
package foo.bar

class Baz {
class Qux // <- this class
}
Type
Implemented in the Analysis API by KaType and its subtypes like KaFlexibleType.

Serialization to String is implemented by KaTypeRenderer using the following properties:

functionalTypeRenderer = KaFunctionalTypeRenderer.AS_FUNCTIONAL_TYPE
No parameter names
Alternatively AS_CLASS_TYPE
annotationsRenderer = … with KaRendererAnnotationsFilter.NONE
No annotations

The remaining properties are TBD.
Spec
Classes, Interfaces and Type Aliases
Class ID
it contains the package name, all outer class names in case of nested classes, and the own class name
Callables
Kind
Function
Constructor
Property (synthetic?)
Getter
Setter
Field
Enum Entry
Containing Class ID or Package for top-level
Name
Signature
Type Parameters and their Bounds
Names should be ignored
Bounds should be sorted by some stable ordering because bound order doesn't matter
Context Parameter Types
Extension Receiver Type
Value Parameter Types
Vararg Flag
Flags
suspend
companion/static
Extensions
Parameter Names for ObjC import and their overrides

Serialization to String
Requirements
Backward Compatible
Reasonably short
Human readable if possible
Deserialization is not a requirement
It's to be used as an ID in a database
Class ID, Package Names and Names need escaping for special characters to prevent collisions with separator characters
Classifiers
Class ID, possibly with a prefix of the kind, though it's not necessary for disambiguation.
Callables
[KIND]:[Containing Class ID or Package for top-level]#[NAME]#[HASH]

The hash is calculated from Signature, Flags and Extensions and needs to be stable.

Open Questions
Should signatures (from Java) contain flexible types and should nullability annotations make them non-flexible?
If either answer is yes, adding nullability/mutability annotations would change the ID
Solution 1: approximate
Seems not enough
If we approximate String? to lower bound and later it's annotated with @NotNull, the ID changes. Same the other way round.
Same with flexible collection (Mutable)List, it can be approximated to either MutableList or List. Either choice could lead to a changing ID if the type is annotated with @ReadOnly or @Mutable.
Solution 2: keep flexible types flexible, even if they're annotated
Need to check if AA supports turning off type enhancement
Should type aliases by fully expanded in signatures
If not, expect and actual declarations could have different IDs
Do we ever care about local classes? They can be return types of private members, but it looks like we are not documenting them anyway?
Should annotations on the declaration or argument, or types affect the ID?
They can anyway. You can do this


expect class A
expect fun foo(x: A)
actual typealias A = B
actual fun foo(x: B)


Which hash algorithm to use?
