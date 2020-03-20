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
}

data class PlatformDependent<out T>(
    val map: Map<PlatformData, T>,
    val expect: T? = null
) : Map<PlatformData, T> by map {
    val prevalentValue: T?
        get() = map.values.distinct().singleOrNull()

    val allValues: Sequence<T> = sequence {
        expect?.also { yield(it) }
        yieldAll(map.values)
    }

    companion object {
        fun <T> empty(): PlatformDependent<T> = PlatformDependent(emptyMap())
        fun <T> from(platformData: PlatformData, element: T) = PlatformDependent(mapOf(platformData to element))
        fun <T> expectFrom(element: T) = PlatformDependent(map = emptyMap(), expect = element)
    }
}

interface WithExpectActual {
    val sources: PlatformDependent<DocumentableSource>
}

interface WithScope {
    val functions: List<DFunction>
    val properties: List<DProperty>
    val classlikes: List<DClasslike>
}

interface WithVisibility {
    val visibility: PlatformDependent<Visibility>
}

interface WithType {
    val type: Bound
}

interface WithAbstraction {
    val modifier: PlatformDependent<Modifier>
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
    val supertypes: PlatformDependent<List<DRI>>
}

interface Callable : WithVisibility, WithType, WithAbstraction, WithExpectActual {
    val receiver: DParameter?
}

abstract class DClasslike : Documentable(), WithScope, WithVisibility, WithExpectActual

data class DModule(
    override val name: String,
    val packages: List<DPackage>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val platformData: List<PlatformData>,
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
    override val documentation: PlatformDependent<DocumentationNode>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<DPackage> = PropertyContainer.empty()
) : Documentable(), WithScope, WithExtraProperties<DPackage> {
    override val name = dri.packageName.orEmpty()
    override val children: List<Documentable>
        get() = (properties + functions + classlikes) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<DPackage>) = copy(extra = newExtras)
}

data class DClass(
    override val dri: DRI,
    override val name: String,
    override val constructors: List<DFunction>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val visibility: PlatformDependent<Visibility>,
    override val companion: DObject?,
    override val generics: List<DTypeParameter>,
    override val supertypes: PlatformDependent<List<DRI>>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val modifier: PlatformDependent<Modifier>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<DClass> = PropertyContainer.empty()
) : DClasslike(), WithAbstraction, WithCompanion, WithConstructors, WithGenerics, WithSupertypes,
    WithExtraProperties<DClass> {

    override val children: List<Documentable>
        get() = (functions + properties + classlikes + constructors) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<DClass>) = copy(extra = newExtras)
}

data class DEnum(
    override val dri: DRI,
    override val name: String,
    val entries: List<DEnumEntry>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: PlatformDependent<Visibility>,
    override val companion: DObject?,
    override val constructors: List<DFunction>,
    override val supertypes: PlatformDependent<List<DRI>>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<DEnum> = PropertyContainer.empty()
) : DClasslike(), WithCompanion, WithConstructors, WithSupertypes, WithExtraProperties<DEnum> {
    override val children: List<Documentable>
        get() = (entries + functions + properties + classlikes + constructors) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<DEnum>) = copy(extra = newExtras)
}

data class DEnumEntry(
    override val dri: DRI,
    override val name: String,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<DEnumEntry> = PropertyContainer.empty()
) : Documentable(), WithScope, WithExtraProperties<DEnumEntry> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<DEnumEntry>) = copy(extra = newExtras)
}

data class DFunction(
    override val dri: DRI,
    override val name: String,
    val isConstructor: Boolean,
    val parameters: List<DParameter>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val visibility: PlatformDependent<Visibility>,
    override val type: Bound,
    override val generics: List<DTypeParameter>,
    override val receiver: DParameter?,
    override val modifier: PlatformDependent<Modifier>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<DFunction> = PropertyContainer.empty()
) : Documentable(), Callable, WithGenerics, WithExtraProperties<DFunction> {
    override val children: List<Documentable>
        get() = parameters

    override fun withNewExtras(newExtras: PropertyContainer<DFunction>) = copy(extra = newExtras)
}

data class DInterface(
    override val dri: DRI,
    override val name: String,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: PlatformDependent<Visibility>,
    override val companion: DObject?,
    override val generics: List<DTypeParameter>,
    override val supertypes: PlatformDependent<List<DRI>>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<DInterface> = PropertyContainer.empty()
) : DClasslike(), WithCompanion, WithGenerics, WithSupertypes, WithExtraProperties<DInterface> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<DInterface>) = copy(extra = newExtras)
}

data class DObject(
    override val name: String?,
    override val dri: DRI,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: PlatformDependent<Visibility>,
    override val supertypes: PlatformDependent<List<DRI>>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<DObject> = PropertyContainer.empty()
) : DClasslike(), WithSupertypes, WithExtraProperties<DObject> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<DObject>) = copy(extra = newExtras)
}

data class DAnnotation(
    override val name: String,
    override val dri: DRI,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val functions: List<DFunction>,
    override val properties: List<DProperty>,
    override val classlikes: List<DClasslike>,
    override val visibility: PlatformDependent<Visibility>,
    override val companion: DObject?,
    override val constructors: List<DFunction>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<DAnnotation> = PropertyContainer.empty()
) : DClasslike(), WithCompanion, WithConstructors, WithExtraProperties<DAnnotation> {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes + constructors) as List<Documentable>

    override fun withNewExtras(newExtras: PropertyContainer<DAnnotation>) = copy(extra = newExtras)
}

data class DProperty(
    override val dri: DRI,
    override val name: String,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val sources: PlatformDependent<DocumentableSource>,
    override val visibility: PlatformDependent<Visibility>,
    override val type: Bound,
    override val receiver: DParameter?,
    val setter: DFunction?,
    val getter: DFunction?,
    override val modifier: PlatformDependent<Modifier>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<DProperty> = PropertyContainer.empty()
) : Documentable(), Callable, WithExtraProperties<DProperty> {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DProperty>) = copy(extra = newExtras)
}

// TODO: treat named Parameters and receivers differently
data class DParameter(
    override val dri: DRI,
    override val name: String?,
    override val documentation: PlatformDependent<DocumentationNode>,
    val type: Bound,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<DParameter> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<DParameter> {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DParameter>) = copy(extra = newExtras)
}

data class DTypeParameter(
    override val dri: DRI,
    override val name: String,
    override val documentation: PlatformDependent<DocumentationNode>,
    val bounds: List<Bound>,
    override val platformData: List<PlatformData>,
    override val extra: PropertyContainer<DTypeParameter> = PropertyContainer.empty()
) : Documentable(), WithExtraProperties<DTypeParameter> {
    override val children: List<Nothing>
        get() = emptyList()

    override fun withNewExtras(newExtras: PropertyContainer<DTypeParameter>) = copy(extra = newExtras)
}

sealed class Projection
sealed class Bound : Projection()
data class OtherParameter(val name: String) : Bound()
object Star : Projection()
data class TypeConstructor(val dri: DRI, val projections: List<Projection>, val modifier: FunctionModifiers = FunctionModifiers.NONE) : Bound()
data class Nullable(val inner: Bound) : Bound()
data class Variance(val kind: Kind, val inner: Bound) : Projection() {
    enum class Kind { In, Out }
}
data class PrimitiveJavaType(val name: String): Bound()
object Void : Bound()
object JavaObject : Bound()

enum class FunctionModifiers {
    NONE, FUNCTION, EXTENSION
}

enum class ExtraModifiers {
    STATIC, INLINE, INFIX, SUSPEND, REIFIED, CROSSINLINE, NOINLINE,
    OVERRIDE, DATA, CONST, DYNAMIC, EXTERNAL, INNER, LATEINIT, OPERATOR, TAILREC, VARARG,
    NATIVE, SYNCHRONIZED, STRICTFP, TRANSIENT, VOLATILE, TRANSITIVE
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
