package org.jetbrains.dokka.model

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.kotlin.toSourceElement

abstract class Documentable {
    abstract val name: String?
    abstract val dri: DRI
    abstract val children: List<Documentable>
    abstract val documentation: PlatformDependent<DocumentationNode>
    abstract val platformData: List<PlatformData>

    override fun toString(): String =
        "${javaClass.simpleName}($dri)"

    override fun equals(other: Any?) =
        other is Documentable && this.dri == other.dri // TODO: https://github.com/Kotlin/dokka/pull/667#discussion_r382555806

    override fun hashCode() = dri.hashCode()

    val briefDocTagString: String by lazy {
        // TODO > utils
        documentation.values
            .firstOrNull()
            ?.children
            ?.firstOrNull()
            ?.root
            ?.docTagSummary()
            ?.shorten(40) ?: ""
    }
}

data class PlatformDependent<out T>(
    val map: Map<PlatformData, T>,
    val expect: T? = null
) : Map<PlatformData, T> by map {
    val prevalentValue: T?
        get() = map.values.distinct().singleOrNull()

    companion object {
        fun <T> empty(): PlatformDependent<T> = PlatformDependent(emptyMap())
        fun <T> from(platformData: PlatformData, element: T) = PlatformDependent(mapOf(platformData to element))
    }
}

interface WithExpectActual {
    val sources: PlatformDependent<DocumentableSource>
}

interface WithScope {
    val functions: List<Function>
    val properties: List<Property>
    val classlikes: List<Classlike>
}

interface WithPackages {
    val packages: List<Package>
}

interface WithVisibility {
    val visibility: PlatformDependent<Visibility>
}

interface WithType {
    val type: TypeWrapper
}

interface WithAbstraction {
    val modifier: Modifier
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
    val companion: Object?
}

interface WithConstructors {
    val constructors: List<Function>
}

interface WithGenerics {
    val generics: List<TypeParameter>
}

interface WithSupertypes {
    val supertypes: PlatformDependent<List<DRI>>
}

interface Callable : WithVisibility, WithType, WithAbstraction, WithExpectActual {
    val receiver: Parameter?
}

abstract class Classlike : Documentable(), WithScope, WithVisibility, WithExpectActual

data class Module(
    override val name: String,
    override val packages: List<Package>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<Module> = PropertyContainer.empty()
) : Documentable(), WithPackages, WithExtraProperties<Module> {
    override val dri: DRI = DRI.topLevel
    override val children: List<Documentable>
        get() = packages

    override fun withNewExtras(newExtras: PropertyContainer<Module>) = copy(extra = newExtras)
}

data class Package(
    override val dri: DRI,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val packages: List<Package>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<Package> = PropertyContainer.empty()
) : Documentable(), WithScope, WithPackages, WithExtraProperties<Package> {
    override val name = dri.packageName.orEmpty()
    override val children: List<Documentable>
        get() = (properties + functions + classlikes + packages) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<Package>) = copy(extra = newExtras)
}

data class Class(
    override val dri: DRI,
    override val name: String,
    override val constructors: List<Function>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val visibility: PlatformDependent<Visibility>,
    override val companion: Object?,
    override val generics: List<TypeParameter>,
    override val supertypes: PlatformDependent<List<DRI>>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val modifier: Modifier,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<Class> = PropertyContainer.empty()
) : Classlike(), WithAbstraction, WithCompanion, WithConstructors, WithGenerics, WithSupertypes,
    WithExtraProperties<Class> {

    override val children: List<Documentable>
        get() = (functions + properties + classlikes + constructors) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<Class>) = copy(extra = newExtras)
}

data class Enum(
    override val dri: DRI,
    override val name: String,
    val entries: List<EnumEntry>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val visibility: PlatformDependent<Visibility>,
    override val companion: Object?,
    override val constructors: List<Function>,
    override val supertypes: PlatformDependent<List<DRI>>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<Enum> = PropertyContainer.empty()
) : Classlike(), WithCompanion, WithConstructors, WithSupertypes, WithExtraProperties<Enum> {
    override val children: List<Documentable>
        get() = (entries + functions + properties + classlikes + constructors) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<Enum>) = copy(extra = newExtras)
}

data class EnumEntry(
    override val dri: DRI,
    override val name: String,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<EnumEntry> = PropertyContainer.empty()
) : Documentable(), WithScope, WithExtraProperties<EnumEntry> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<EnumEntry>) = copy(extra = newExtras)
}

data class Function(
    override val dri: DRI,
    override val name: String,
    val isConstructor: Boolean,
    val parameters: List<Parameter>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val visibility: PlatformDependent<Visibility>,
    override val type: TypeWrapper,
    override val generics: List<TypeParameter>,
    override val receiver: Parameter?,
    override val modifier: Modifier,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<Function> = PropertyContainer.empty()
) : Documentable(), Callable, WithGenerics, WithExtraProperties<Function> {
    override val children: List<Documentable>
        get() = parameters

    override fun withNewExtras(newExtras: PropertyContainer<Function>) = copy(extra = newExtras)
}

data class Interface(
    override val dri: DRI,
    override val name: String,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val visibility: PlatformDependent<Visibility>,
    override val companion: Object?,
    override val generics: List<TypeParameter>,
    override val supertypes: PlatformDependent<List<DRI>>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<Interface> = PropertyContainer.empty()
) : Classlike(), WithCompanion, WithGenerics, WithSupertypes, WithExtraProperties<Interface> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<Interface>) = copy(extra = newExtras)
}

data class Object(
    override val name: String?,
    override val dri: DRI,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val visibility: PlatformDependent<Visibility>,
    override val supertypes: PlatformDependent<List<DRI>>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<Object> = PropertyContainer.empty()
) : Classlike(), WithSupertypes, WithExtraProperties<Object> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<Object>) = copy(extra = newExtras)
}

data class Annotation(
    override val name: String,
    override val dri: DRI,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val visibility: PlatformDependent<Visibility>,
    override val companion: Object?,
    override val constructors: List<Function>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<Annotation> = PropertyContainer.empty()
) : Classlike(), WithCompanion, WithConstructors, WithExtraProperties<Annotation> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes + constructors) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<Annotation>) = copy(extra = newExtras)
}

data class Property(
    override val dri: DRI,
    override val name: String,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val visibility: PlatformDependent<Visibility>,
    override val type: TypeWrapper,
    override val receiver: Parameter?,
    val setter: Function?,
    val getter: Function?,
    override val modifier: Modifier,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<Property> = PropertyContainer.empty()
) : Documentable(), Callable, WithExtraProperties<Property> {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<Property>) = copy(extra = newExtras)
}

// TODO: treat named Parameters and receivers differently
data class Parameter(
    override val dri: DRI,
    override val name: String?,
    override val documentation: PlatformDependent<DocumentationNode>,
    val type: TypeWrapper,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<Parameter> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<Parameter> {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<Parameter>) = copy(extra = newExtras)
}

data class TypeParameter(
    override val dri: DRI,
    override val name: String,
    override val documentation: PlatformDependent<DocumentationNode>,
    val bounds: List<Bound>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<TypeParameter> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<TypeParameter> {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<TypeParameter>) = copy(extra = newExtras)
}

sealed class Projection
sealed class Bound : Projection()
data class OtherParameter(val name: String) : Bound()
object Star : Projection()
data class TypeConstructor(val dri: DRI, val projections: List<Projection>) : Bound()
data class Nullable(val inner: Bound) : Bound()
data class Variance(val kind: Kind, val inner: Bound) : Projection() {
    enum class Kind { In, Out }
}

enum class ExtraModifiers {
    STATIC, INLINE, INFIX, SUSPEND, REIFIED, CROSSINLINE, NOINLINE,
    OVERRIDE, DATA, CONST, DYNAMIC, EXTERNAL, INNER, LATEINIT, OPERATOR, TAILREC, VARARG
}

private fun String.shorten(maxLength: Int) = lineSequence().first().let {
    if (it.length != length || it.length > maxLength) it.take(maxLength - 3) + "..." else it
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

fun <T> PlatformDependent<T>?.orEmpty(): PlatformDependent<T> = this ?: PlatformDependent.empty()
sealed class DocumentableSource(val path: String)

class DescriptorDocumentableSource(val descriptor: DeclarationDescriptor) :
    DocumentableSource(descriptor.toSourceElement.containingFile.toString())

class PsiDocumentableSource(val psi: PsiNamedElement) : DocumentableSource(psi.containingFile.virtualFile.path)
