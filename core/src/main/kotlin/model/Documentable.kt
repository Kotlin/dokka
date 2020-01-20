package org.jetbrains.dokka.model

import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.transformers.descriptors.KotlinTypeWrapper
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.PlatformData

class Module(override val name: String, val packages: List<Package>) : Documentable() {
    override val dri: DRI = DRI.topLevel
    override val children: List<Package> = packages
    override val extra: MutableSet<Extra> = mutableSetOf()
}

class Package(
    override val dri: DRI,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classes: List<Class>,
    override val extra: MutableSet<Extra> = mutableSetOf()
) : ScopeNode() {
    override val name = dri.packageName.orEmpty()
}

class Class(
    override val dri: DRI,
    override val name: String,
    val kind: ClassKind,
    val constructors: List<Function>,
    override val functions: List<Function>,
    override val properties: List<Property>,
    override val classes: List<Class>,
    override val expected: ClassPlatformInfo?,
    override val actual: List<ClassPlatformInfo>,
    override val extra: MutableSet<Extra> = mutableSetOf()
) : ScopeNode() {
    val inherited by lazy { platformInfo.mapNotNull { (it as? ClassPlatformInfo)?.inherited }.flatten() }
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
    override val extra: MutableSet<Extra> = mutableSetOf()
) : CallableNode() {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver) + parameters
}

class Property(
    override val dri: DRI,
    override val name: String,
    override val receiver: Parameter?,
    override val expected: PlatformInfo?,
    override val actual: List<PlatformInfo>,
    override val extra: MutableSet<Extra> = mutableSetOf()
) : CallableNode() {
    override val children: List<Parameter>
        get() = listOfNotNull(receiver)
}

// TODO: treat named Parameters and receivers differently
class Parameter(
    override val dri: DRI,
    override val name: String?,
    val type: TypeWrapper,
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
    override val platformData: List<PlatformData>) : PlatformInfo {

    override fun equals(other: Any?): Boolean =
        other is PlatformInfo && documentationNode == other.documentationNode

    override fun hashCode(): Int =
        documentationNode.hashCode()
}

class ClassPlatformInfo(
    val info: PlatformInfo,
    val inherited: List<DRI>) : PlatformInfo by info

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



    val commentsData: List<DocumentationNode>
        get() = platformInfo.map { it.documentationNode }

    val briefDocTagString: String
        get() =
            platformInfo
            .firstOrNull()
            ?.documentationNode
            ?.children
            ?.firstOrNull()
            ?.root
            ?.docTagSummary()
            ?.shorten(40) ?: ""

    open val extra: MutableSet<Extra> = mutableSetOf()
}

abstract class ScopeNode : Documentable() {
    abstract val functions: List<Function>
    abstract val properties: List<Property>
    abstract val classes: List<Class>

    override val children: List<Documentable>
        get() = functions + properties + classes
}

abstract class CallableNode : Documentable() {
    abstract val receiver: Parameter?
}

private fun String.shorten(maxLength: Int) = lineSequence().first().let {
    if (it.length != length || it.length > maxLength) it.take(maxLength - 3) + "..." else it
}

interface TypeWrapper {
    val constructorFqName: String?
    val constructorNamePathSegments: List<String>
    val arguments: List<TypeWrapper>
    val dri: DRI?
}
interface ClassKind

fun Documentable.dfs(predicate: (Documentable) -> Boolean): Documentable? =
    if (predicate(this)) {
        this
    } else {
        this.children.asSequence().mapNotNull { it.dfs(predicate) }.firstOrNull()
    }

interface Extra