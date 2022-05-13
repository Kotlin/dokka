package org.jetbrains.dokka.model

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties

interface AnnotationTarget

abstract class Documentable : WithChildren<Documentable>,
    AnnotationTarget {
    abstract val name: String?
    abstract val dri: DRI
    abstract val documentation: SourceSetDependent<DocumentationNode>
    abstract val sourceSets: Set<DokkaSourceSet>
    abstract val expectPresentInSet: DokkaSourceSet?
    abstract override val children: List<Documentable>

    override fun toString(): String =
        "${javaClass.simpleName}($dri)"

    override fun equals(other: Any?) =
        other is Documentable && this.dri == other.dri // TODO: https://github.com/Kotlin/dokka/pull/667#discussion_r382555806

    override fun hashCode() = dri.hashCode()
}

typealias SourceSetDependent<T> = Map<DokkaSourceSet, T>

interface WithSources {
    val sources: SourceSetDependent<DocumentableSource>
}

interface WithScope {
    val functions: List<DFunction>
    val properties: List<DProperty>
    val classlikes: List<DClasslike>
}

interface WithVisibility {
    val visibility: SourceSetDependent<Visibility>
}

interface WithType {
    val type: Bound
}

interface WithAbstraction {
    val modifier: SourceSetDependent<Modifier>
}

sealed class Modifier(val name: String)
sealed class KotlinModifier(name: String) : Modifier(name) {
    object Abstract : KotlinModifier("abstract")
    object Open : KotlinModifier("open")
    object Final : KotlinModifier("final")
    object Sealed : KotlinModifier("sealed")
    object Empty : KotlinModifier("")
}

sealed class JavaModifier(name: String) : Modifier(name) {
    object Abstract : JavaModifier("abstract")
    object Final : JavaModifier("final")
    object Empty : JavaModifier("")
}

interface WithCompanion {
    val companion: DObject?
}

interface WithConstructors {
    val constructors: List<DFunction>
}

interface WithGenerics {
    val generics: List<DTypeParameter>
}

interface WithSupertypes {
    val supertypes: SourceSetDependent<List<TypeConstructorWithKind>>
}

interface WithIsExpectActual {
    val isExpectActual: Boolean
}

interface Callable : WithVisibility, WithType, WithAbstraction, WithSources, WithIsExpectActual {
    val receiver: DParameter?
}

sealed class DClasslike : Documentable(), WithScope, WithVisibility, WithSources, WithIsExpectActual

data class DModule(
    override val name: String,
    val packages: List<DPackage>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet? = null,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DModule> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<DModule> {
    override val dri: DRI = DRI.topLevel
    override val children: List<Documentable>
        get() = packages

    override fun withNewExtras(newExtras: PropertyContainer<DModule>) = copy(extra = newExtras)
}

data class DPackage(
    override val dri: DRI,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    val typealiases: List<DTypeAlias>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet? = null,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DPackage> = PropertyContainer.empty()
) : Documentable(), WithScope, WithExtraProperties<DPackage> {

    val packageName: String = dri.packageName.orEmpty()

    /**
     * !!! WARNING !!!
     * This name is not guaranteed to be a be a canonical/real package name.
     * e.g. this will return a human readable version for root packages.
     * Use [packageName] or `dri.packageName` instead to obtain the real packageName
     */
    override val name: String = packageName.ifBlank { "[root]" }

    override val children: List<Documentable> = properties + functions + classlikes + typealiases

    override fun withNewExtras(newExtras: PropertyContainer<DPackage>) = copy(extra = newExtras)
}

data class DClass(
    override val dri: DRI,
    override val name: String,
    override val constructors: List<DFunction>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val visibility: SourceSetDependent<Visibility>,
    override val companion: DObject?,
    override val generics: List<DTypeParameter>,
    override val supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val modifier: SourceSetDependent<Modifier>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DClass> = PropertyContainer.empty()
) : DClasslike(), WithAbstraction, WithCompanion, WithConstructors, WithGenerics, WithSupertypes,
    WithExtraProperties<DClass> {

    override val children: List<Documentable>
        get() = (functions + properties + classlikes + constructors)

    override fun withNewExtras(newExtras: PropertyContainer<DClass>) = copy(extra = newExtras)
}

data class DEnum(
    override val dri: DRI,
    override val name: String,
    val entries: List<DEnumEntry>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: SourceSetDependent<Visibility>,
    override val companion: DObject?,
    override val constructors: List<DFunction>,
    override val supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DEnum> = PropertyContainer.empty()
) : DClasslike(), WithCompanion, WithConstructors, WithSupertypes, WithExtraProperties<DEnum> {
    override val children: List<Documentable>
        get() = (entries + functions + properties + classlikes + constructors)

    override fun withNewExtras(newExtras: PropertyContainer<DEnum>) = copy(extra = newExtras)
}

data class DEnumEntry(
    override val dri: DRI,
    override val name: String,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DEnumEntry> = PropertyContainer.empty()
) : Documentable(), WithScope, WithExtraProperties<DEnumEntry>, WithSources {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes)

    override fun withNewExtras(newExtras: PropertyContainer<DEnumEntry>) = copy(extra = newExtras)
}

data class DFunction(
    override val dri: DRI,
    override val name: String,
    val isConstructor: Boolean,
    val parameters: List<DParameter>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val visibility: SourceSetDependent<Visibility>,
    override val type: Bound,
    override val generics: List<DTypeParameter>,
    override val receiver: DParameter?,
    override val modifier: SourceSetDependent<Modifier>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DFunction> = PropertyContainer.empty()
) : Documentable(), Callable, WithGenerics, WithExtraProperties<DFunction> {
    override val children: List<Documentable>
        get() = parameters

    override fun withNewExtras(newExtras: PropertyContainer<DFunction>) = copy(extra = newExtras)
}

data class DInterface(
    override val dri: DRI,
    override val name: String,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: SourceSetDependent<Visibility>,
    override val companion: DObject?,
    override val generics: List<DTypeParameter>,
    override val supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DInterface> = PropertyContainer.empty()
) : DClasslike(), WithCompanion, WithGenerics, WithSupertypes, WithExtraProperties<DInterface> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes)

    override fun withNewExtras(newExtras: PropertyContainer<DInterface>) = copy(extra = newExtras)
}

data class DObject(
    override val name: String?,
    override val dri: DRI,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: SourceSetDependent<Visibility>,
    override val supertypes: SourceSetDependent<List<TypeConstructorWithKind>>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DObject> = PropertyContainer.empty()
) : DClasslike(), WithSupertypes, WithExtraProperties<DObject> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes)

    override fun withNewExtras(newExtras: PropertyContainer<DObject>) = copy(extra = newExtras)
}

data class DAnnotation(
    override val name: String,
    override val dri: DRI,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: SourceSetDependent<Visibility>,
    override val companion: DObject?,
    override val constructors: List<DFunction>,
    override val generics: List<DTypeParameter>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DAnnotation> = PropertyContainer.empty()
) : DClasslike(), WithCompanion, WithConstructors, WithExtraProperties<DAnnotation>, WithGenerics {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes + constructors)

    override fun withNewExtras(newExtras: PropertyContainer<DAnnotation>) = copy(extra = newExtras)
}

data class DProperty(
    override val dri: DRI,
    override val name: String,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val visibility: SourceSetDependent<Visibility>,
    override val type: Bound,
    override val receiver: DParameter?,
    val setter: DFunction?,
    val getter: DFunction?,
    override val modifier: SourceSetDependent<Modifier>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val generics: List<DTypeParameter>,
    override val isExpectActual: Boolean,
    override val extra: PropertyContainer<DProperty> = PropertyContainer.empty()
) : Documentable(), Callable, WithExtraProperties<DProperty>, WithGenerics {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DProperty>) = copy(extra = newExtras)
}

// TODO: treat named Parameters and receivers differently
data class DParameter(
    override val dri: DRI,
    override val name: String?,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val type: Bound,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DParameter> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<DParameter>, WithType, WithSources {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DParameter>) = copy(extra = newExtras)
}

data class DTypeParameter(
    val variantTypeParameter: Variance<TypeParameter>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    val bounds: List<Bound>,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val extra: PropertyContainer<DTypeParameter> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<DTypeParameter>, WithSources {

    constructor(
        dri: DRI,
        name: String,
        presentableName: String?,
        documentation: SourceSetDependent<DocumentationNode>,
        expectPresentInSet: DokkaSourceSet?,
        bounds: List<Bound>,
        sources: SourceSetDependent<DocumentableSource>,
        sourceSets: Set<DokkaSourceSet>,
        extra: PropertyContainer<DTypeParameter> = PropertyContainer.empty()
    ) : this(
        Invariance(TypeParameter(dri, name, presentableName, sources)),
        documentation,
        expectPresentInSet,
        bounds,
        sources,
        sourceSets,
        extra
    )

    override val dri: DRI by variantTypeParameter.inner::dri
    override val name: String by variantTypeParameter.inner::name

    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DTypeParameter>) = copy(extra = newExtras)
}

data class DTypeAlias(
    override val dri: DRI,
    override val name: String,
    override val type: Bound,
    val underlyingType: SourceSetDependent<Bound>,
    override val visibility: SourceSetDependent<Visibility>,
    override val documentation: SourceSetDependent<DocumentationNode>,
    override val expectPresentInSet: DokkaSourceSet?,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val sourceSets: Set<DokkaSourceSet>,
    override val generics: List<DTypeParameter>,
    override val extra: PropertyContainer<DTypeAlias> = PropertyContainer.empty()
) : Documentable(), WithType, WithVisibility, WithExtraProperties<DTypeAlias>, WithGenerics, WithSources {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DTypeAlias>) = copy(extra = newExtras)
}

sealed class Projection
sealed class Bound : Projection()
val Bound.sources get() = (this as WithSources).sources
data class TypeParameter(
    val dri: DRI,
    val name: String,
    val presentableName: String? = null,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val extra: PropertyContainer<TypeParameter> = PropertyContainer.empty()
) : Bound(), AnnotationTarget, WithExtraProperties<TypeParameter>, WithSources {
    override fun withNewExtras(newExtras: PropertyContainer<TypeParameter>): TypeParameter =
        copy(extra = extra)
}

sealed class TypeConstructor : Bound(), AnnotationTarget, WithSources {
    abstract val dri: DRI
    abstract val projections: List<Projection>
    abstract val presentableName: String?
    abstract override val sources: SourceSetDependent<DocumentableSource>
}

data class GenericTypeConstructor(
    override val dri: DRI,
    override val projections: List<Projection>,
    override val presentableName: String? = null,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val extra: PropertyContainer<GenericTypeConstructor> = PropertyContainer.empty()
) : TypeConstructor(), WithExtraProperties<GenericTypeConstructor> {
    override fun withNewExtras(newExtras: PropertyContainer<GenericTypeConstructor>): GenericTypeConstructor =
        copy(extra = newExtras)
}

data class FunctionalTypeConstructor(
    override val dri: DRI,
    override val projections: List<Projection>,
    val isExtensionFunction: Boolean = false,
    val isSuspendable: Boolean = false,
    override val presentableName: String? = null,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val extra: PropertyContainer<FunctionalTypeConstructor> = PropertyContainer.empty(),
) : TypeConstructor(), WithExtraProperties<FunctionalTypeConstructor> {
    override fun withNewExtras(newExtras: PropertyContainer<FunctionalTypeConstructor>): FunctionalTypeConstructor =
        copy(extra = newExtras)
}

// kotlin.annotation.AnnotationTarget.TYPEALIAS
data class TypeAliased(
    val typeAlias: Bound,
    val inner: Bound,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val extra: PropertyContainer<TypeAliased> = PropertyContainer.empty()
) : Bound(), AnnotationTarget, WithExtraProperties<TypeAliased>, WithSources {
    override fun withNewExtras(newExtras: PropertyContainer<TypeAliased>): TypeAliased =
        copy(extra = newExtras)
}

data class PrimitiveJavaType(
    val name: String,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val extra: PropertyContainer<PrimitiveJavaType> = PropertyContainer.empty()
) : Bound(), AnnotationTarget, WithExtraProperties<PrimitiveJavaType>, WithSources {
    override fun withNewExtras(newExtras: PropertyContainer<PrimitiveJavaType>): PrimitiveJavaType =
        copy(extra = newExtras)
}

data class JavaObject(
    override val sources: SourceSetDependent<DocumentableSource>,
    override val extra: PropertyContainer<JavaObject> = PropertyContainer.empty()
) :
    Bound(), AnnotationTarget, WithExtraProperties<JavaObject>, WithSources {
    override fun withNewExtras(newExtras: PropertyContainer<JavaObject>): JavaObject =
        copy(extra = newExtras)
}

data class UnresolvedBound(
    val name: String,
    override val sources: SourceSetDependent<DocumentableSource>,
    override val extra: PropertyContainer<UnresolvedBound> = PropertyContainer.empty()
) : Bound(), AnnotationTarget, WithExtraProperties<UnresolvedBound>, WithSources {
    override fun withNewExtras(newExtras: PropertyContainer<UnresolvedBound>): UnresolvedBound =
        copy(extra = newExtras)
}

// The following Projections are not AnnotationTargets; they cannot be annotated.
data class Nullable(val inner: Bound) : Bound(), WithSources {
    override val sources: SourceSetDependent<DocumentableSource>
        get() = when (inner) {
            is Nullable -> throw RuntimeException("Nullable Nullable is not a valid type")
            is Dynamic -> throw RuntimeException("Nullable Dynamic is not a valid type")
            is JavaObject -> inner.sources
            is PrimitiveJavaType -> inner.sources
            is TypeAliased -> inner.sources
            is TypeConstructor -> inner.sources
            is TypeParameter -> inner.sources
            is UnresolvedBound -> inner.sources
            is Void -> inner.sources
        }
}

sealed class Variance<out T : Bound> : Projection(), WithSources {
    abstract val inner: T

    override val sources: SourceSetDependent<DocumentableSource> get() = when (inner) {
        is WithSources -> (inner as WithSources).sources
        is Dynamic -> TODO()
        is Void -> TODO()
        else -> throw RuntimeException("Impossible inner Bound for Variance: $inner")
    }
}

data class Covariance<out T : Bound>(override val inner: T) : Variance<T>() {
    override fun toString() = "out"
}

data class Contravariance<out T : Bound>(override val inner: T) : Variance<T>() {
    override fun toString() = "in"
}

data class Invariance<out T : Bound>(override val inner: T) : Variance<T>() {
    override fun toString() = ""
}

// Void cannot be an object because not all Void types are the same. For example, a Void declared in Java is of
// platform nullability, while a (non-wrapped) Void declared in Kotlin is non-null.
class Void(override val sources: SourceSetDependent<DocumentableSource>) : Bound(), WithSources
// TODO: should these be WithSources? (that would require they be classes)
object Dynamic : Bound()
object Star : Projection()


fun Variance<TypeParameter>.withDri(dri: DRI, sources: SourceSetDependent<DocumentableSource>) = when (this) {
    is Contravariance -> Contravariance(TypeParameter(dri, inner.name, inner.presentableName, sources))
    is Covariance -> Covariance(TypeParameter(dri, inner.name, inner.presentableName, sources))
    is Invariance -> Invariance(TypeParameter(dri, inner.name, inner.presentableName, sources))
}

fun Documentable.dfs(predicate: (Documentable) -> Boolean): Documentable? =
    if (predicate(this)) {
        this
    } else {
        this.children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull()
    }

sealed class Visibility(val name: String)
sealed class KotlinVisibility(name: String) : Visibility(name) {
    object Public : KotlinVisibility("public")
    object Private : KotlinVisibility("private")
    object Protected : KotlinVisibility("protected")
    object Internal : KotlinVisibility("internal")
}

sealed class JavaVisibility(name: String) : Visibility(name) {
    object Public : JavaVisibility("public")
    object Private : JavaVisibility("private")
    object Protected : JavaVisibility("protected")
    object Default : JavaVisibility("")
}

fun <T> SourceSetDependent<T>?.orEmpty(): SourceSetDependent<T> = this ?: emptyMap()

interface DocumentableSource {
    val path: String
    val language: Language
}

data class TypeConstructorWithKind(val typeConstructor: TypeConstructor, val kind: ClassKind)

public fun WithSources.sourceLanguage(sourceSet: DokkaSourceSet) = sources[sourceSet]!!.language

public val WithSources.sourceLanguage: Language get() = this.sources.entries.single().value.language
