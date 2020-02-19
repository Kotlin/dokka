package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.kotlin.descriptors.Visibility

abstract class Documentable {
    abstract val name: String?
    abstract val dri: DRI
    abstract val children: List<Documentable>
    abstract val documentation: PlatformDependent<DocumentationNode>
    abstract val original: PlatformDependent<Documentable>

    override fun toString(): String =
        "${javaClass.simpleName}($dri)" //+ briefDocTagString.takeIf { it.isNotBlank() }?.let { " [$it]" }.orEmpty()

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

data class PlatformDependent<out T>(val map: Map<PlatformData, T>) : Map<PlatformData, T> by map {
    val prevalentValue: T?
        get() = if (map.all { values.first() == it.value }) values.first() else null
}

interface WithExpectActual {
    val expect: Documentable?
    val actual: PlatformDependent<Documentable>
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
    val visibility: PlatformDependent<Visibility> // TODO custom visibility
}

interface WithType {
    val type: PlatformDependent<TypeWrapper>
}

interface WithAbstraction {
    val modifier: PlatformDependent<Modifier>

    enum class Modifier {
        Abstract, Open, Final
    }
}

interface WithCompanion {
    val companion: Object?
}

interface WithConstructors {
    val constructors: List<Function>
}

interface WithGenerics {
    val typeParameters: List<GenericType>
}

interface Callable : WithVisibility, WithType, WithAbstraction, WithExpectActual {
    val receiver: PlatformDependent<Parameter>
}

interface CanBeSupertype

interface Classlike : WithScope, WithVisibility, WithExpectActual, CanBeSupertype {
    val supertypes: PlatformDependent<CanBeSupertype>
}

class Module(
    override val name: String,
    override val packages: List<Package>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val original: PlatformDependent<Module>
) : Documentable(), WithPackages {
    override val dri: DRI = DRI.topLevel
    override val children: List<Documentable>
        get() = packages
}

class Package(
    override val dri: DRI,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val packages: List<Package>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val original: PlatformDependent<Package>
) : Documentable(), WithScope, WithPackages {
    override val name = dri.packageName.orEmpty()
    override val children: List<Documentable>
        get() = (functions + properties + classlikes + packages) as List<Documentable>
}

class Class(
    override val dri: DRI,
    override val name: String,
    override val constructors: List<Function>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val expect: Class?,
    override val actual: PlatformDependent<Class>,
    override val visibility: PlatformDependent<Visibility>,
    override val companion: Object,
    override val generics: PlatformDependent<TypeWrapper>,
    override val supertypes: PlatformDependent<Classlike>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val original: PlatformDependent<Class>,
    override val modifier: PlatformDependent<WithAbstraction.Modifier>,
    override val typeParameters: List<GenericType>
) : Documentable(), Classlike, WithAbstraction, WithCompanion, WithConstructors, WithGenerics {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes + companion + constructors) as List<Documentable>
}

class Enum(
    override val dri: DRI,
    override val name: String,
    val entries: List<EnumEntry>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val expect: Enum?,
    override val actual: PlatformDependent<Enum>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val visibility: PlatformDependent<Visibility>,
    override val companion: Object,
    override val constructors: List<Function>,
    override val supertypes: PlatformDependent<Classlike>,
    override val original: PlatformDependent<Enum>
) : Documentable(), Classlike, WithCompanion, WithConstructors {
    override val children: List<Documentable>
        get() = (entries + functions + properties + classlikes + listOf(companion) + constructors) as List<Documentable>
}

class EnumEntry(
    override val name: String?,
    override val dri: DRI,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val original: PlatformDependent<EnumEntry>
) : Documentable(), WithScope {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes) as List<Documentable>
}

class Function(
    override val dri: DRI,
    override val name: String,
    val isConstructor: Boolean,
    val returnType: TypeWrapper?,
    val parameters: List<Parameter>,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val expect: Function?,
    override val actual: PlatformDependent<Function>,
    override val visibility: PlatformDependent<Visibility>,
    override val type: PlatformDependent<TypeWrapper>,
    override val receiver: PlatformDependent<Parameter>,
    override val original: PlatformDependent<Function>,
    override val modifier: PlatformDependent<WithAbstraction.Modifier>,
    override val typeParameters: List<GenericType>
) : Documentable(), Callable, WithGenerics {
    override val children: List<Documentable>
        get() = parameters
}

class Interface(
    override val name: String?,
    override val dri: DRI,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val original: PlatformDependent<Interface>,
    override val expect: Interface?,
    override val actual: PlatformDependent<Interface>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val visibility: PlatformDependent<Visibility>,
    override val companion: Object,
    override val supertypes: PlatformDependent<Classlike>,
    override val typeParameters: List<GenericType>
) : Documentable(), Classlike, WithCompanion, WithGenerics {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes + companion) as List<Documentable>
}

class Object(
    override val name: String?,
    override val dri: DRI,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val original: PlatformDependent<Object>,
    override val expect: Object?,
    override val actual: PlatformDependent<Object>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val visibility: PlatformDependent<Visibility>,
    override val supertypes: PlatformDependent<Classlike>
) : Documentable(), Classlike {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes) as List<Documentable>
}

class Annotation(
    override val name: String?,
    override val dri: DRI,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val original: PlatformDependent<Annotation>,
    override val expect: Annotation?,
    override val actual: PlatformDependent<Annotation>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val visibility: PlatformDependent<Visibility>,
    override val companion: Object,
    override val constructors: List<Function>
) : Documentable(), WithScope, WithVisibility, WithCompanion, WithConstructors, WithExpectActual, CanBeSupertype {
    override val children: List<Documentable>
        get() = (functions + properties + classlikes + constructors + companion) as List<Documentable>
}

class Property(
    override val dri: DRI,
    override val name: String,
    override val documentation: PlatformDependent<DocumentationNode>,
    override val expect: Property?,
    override val actual: PlatformDependent<Property>,
    override val visibility: PlatformDependent<Visibility>,
    override val type: PlatformDependent<TypeWrapper>,
    override val receiver: PlatformDependent<Parameter>,
    val accessors: PlatformDependent<Function>, // TODO > extra
    override val original: PlatformDependent<Property>,
    override val modifier: PlatformDependent<WithAbstraction.Modifier>
) : Documentable(), Callable {
    override val children: List<Documentable>
        get() = emptyList()
}

// TODO: treat named Parameters and receivers differently
class Parameter(
    override val dri: DRI,
    override val name: String?,
    override val documentation: PlatformDependent<DocumentationNode>,
    val type: TypeWrapper,
    override val original: PlatformDependent<Parameter>
) : Documentable() {
    override val children: List<Documentable>
        get() = emptyList()
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

interface Extra
object STATIC : Extra