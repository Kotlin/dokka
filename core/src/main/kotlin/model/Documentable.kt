package org.jetbrains.dokka.model

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.kotlin.descriptors.Visibility

class Module(override val name: String, val packages: List<Package>) : Documentable() {
    override val dri: DRI = DRI.topLevel
    override val children: List<Package> = packages
    override val extra: MutableSet<Extra> = mutableSetOf()
}

class Package(
    override val dri: DRI,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val extra: MutableSet<Extra> = mutableSetOf()
) : ScopeNode() {
    override val name = dri.packageName.orEmpty()
}

class Class(
    override val dri: DRI,
    override val name: String,
    override val kind: ClassKind,
    val constructors: List<Function>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classlikes: List<Classlike>,
    override val expected: ClassPlatformInfo?,
    override val actual: List<ClassPlatformInfo>,
    override val extra: MutableSet<Extra> = mutableSetOf(),
    override val visibility: Map<PlatformData, Visibility>
) : Classlike(
    name = name,
    dri = dri,
    kind = kind,
    functions = functions,
    properties = properties,
    classlikes = classlikes,
    expected = expected,
    actual = actual,
    extra = extra
), WithVisibility

class Enum(
    override val dri: DRI,
    override val name: String,
    val entries: List<EnumEntry>,
    val constructors: List<Function>,
    override val functions: List<Function> = emptyList(),
    override val properties: List<Property> = emptyList(),
    override val classlikes: List<Classlike> = emptyList(),
    override val expected: ClassPlatformInfo? = null,
    override val actual: List<ClassPlatformInfo>,
    override val extra: MutableSet<Extra> = mutableSetOf(),
    override val visibility: Map<PlatformData, Visibility>
) : Classlike(dri = dri, name = name, kind = KotlinClassKindTypes.ENUM_CLASS, actual = actual), WithVisibility {
    constructor(c: Classlike, entries: List<EnumEntry>, ctors: List<Function>) : this(
        dri = c.dri,
        name = c.name,
        entries = entries,
        constructors = ctors,
        functions = c.functions,
        properties = c.properties,
        classlikes = c.classlikes,
        expected = c.expected,
        actual = c.actual,
        extra = c.extra,
        visibility = c.visibility
    )

    override val children: List<Documentable>
        get() = entries
}

class EnumEntry(
    override val dri: DRI,
    override val name: String,
    override val expected: ClassPlatformInfo? = null,
    override val actual: List<ClassPlatformInfo>,
    override val extra: MutableSet<Extra> = mutableSetOf(),
    override val visibility: Map<PlatformData, Visibility>
) : Classlike(
    dri = dri,
    name = name,
    actual = actual,
    expected = expected,
    extra = extra,
    kind = KotlinClassKindTypes.ENUM_ENTRY
) {
    constructor(c: Classlike) : this(
        dri = c.dri,
        name = c.name,
        actual = c.actual,
        expected = c.expected,
        extra = c.extra,
        visibility = c.visibility
    )

    override val children: List<Parameter>
        get() = emptyList()
}

class Function(
    override val dri: DRI,
    override val name: String,
    val returnType: TypeWrapper?,
    val isConstructor: Boolean,
    override val receiver: Parameter?,
    val parameters: List<Parameter>,
    override val expected: PlatformInfo?,
    override val actual: List<PlatformInfo>,
    override val extra: MutableSet<Extra> = mutableSetOf(),
    override val visibility: Map<PlatformData, Visibility>
) : CallableNode(), WithVisibility {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver) + parameters
}

class Property(
    override val dri: DRI,
    override val name: String,
    override val receiver: Parameter?,
    override val expected: PlatformInfo?,
    override val actual: List<PlatformInfo>,
    override val extra: MutableSet<Extra> = mutableSetOf(),
    val accessors: List<Function>,
    override val visibility: Map<PlatformData, Visibility>
) : CallableNode(), WithVisibility {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver)
}

// TODO: treat named Parameters and receivers differently
class Parameter(
    override val dri: DRI,
    override val name: String?,
    val type: TypeWrapper,
    override val expected: PlatformInfo?,
    override val actual: List<PlatformInfo>,
    override val extra: MutableSet<Extra> = mutableSetOf()
) : Documentable() {
    override val children: List<Documentable>
        get() = emptyList()
}

interface PlatformInfo {
    val documentationNode: DocumentationNode
    val platformData: List<PlatformData>
}

class BasePlatformInfo(
    override val documentationNode: DocumentationNode,
    override val platformData: List<PlatformData>
) : PlatformInfo {

    override fun equals(other: Any?): Boolean =
        other is PlatformInfo && documentationNode == other.documentationNode

    override fun hashCode(): Int =
        documentationNode.hashCode()
}

class ClassPlatformInfo(
    val info: PlatformInfo,
    val inherited: List<DRI>
) : PlatformInfo by info

abstract class Documentable {
    open val expected: PlatformInfo? = null
    open val actual: List<PlatformInfo> = emptyList()
    open val name: String? = null
    val platformInfo by lazy { listOfNotNull(expected) + actual }
    val platformData by lazy { platformInfo.flatMap { it.platformData }.toSet() }
    abstract val dri: DRI

    abstract val children: List<Documentable>

    override fun toString(): String {
        return "${javaClass.simpleName}($dri)" + briefDocTagString.takeIf { it.isNotBlank() }?.let { " [$it]" }.orEmpty()
    }

    override fun equals(other: Any?) = other is Documentable && this.dri == other.dri

    override fun hashCode() = dri.hashCode()

    val briefDocTagString: String by lazy {
        platformInfo
            .firstOrNull()
            ?.documentationNode
            ?.children
            ?.firstOrNull()
            ?.root
            ?.docTagSummary()
            ?.shorten(40) ?: ""
    }

    open val extra: MutableSet<Extra> = mutableSetOf()
}

abstract class Classlike(
    override val dri: DRI,
    override val name: String,
    open val kind: ClassKind,
    override val functions: List<Function> = emptyList(),
    override val properties: List<Property> = emptyList(),
    override val classlikes: List<Classlike> = emptyList(),
    override val expected: ClassPlatformInfo? = null,
    override val actual: List<ClassPlatformInfo>,
    override val extra: MutableSet<Extra> = mutableSetOf()
) : ScopeNode(), WithVisibility {
    val inherited by lazy { platformInfo.mapNotNull { (it as? ClassPlatformInfo)?.inherited }.flatten() }
}

abstract class ScopeNode : Documentable() {
    abstract val functions: List<Function>
    abstract val properties: List<Property>
    abstract val classlikes: List<Classlike>

    override val children: List<Documentable> // It is written so awkwardly because of type inference being lost here
        get() = mutableListOf<Documentable>().apply {
            addAll(functions)
            addAll(properties)
            addAll(classlikes)
        }
}

abstract class CallableNode : Documentable() {
    abstract val receiver: Parameter?
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

interface WithVisibility {
    val visibility: Map<PlatformData, Visibility>
}