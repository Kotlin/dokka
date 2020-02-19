package org.jetbrains.dokka.model

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjection

abstract class Type(open val name: String, open val bounds: List<Type>) {
    open fun resolve(lookup: Map<String, GenericType.GenericTypeMutable>): Type = this

    open fun asImmutable(
        cache: MutableMap<String, GenericType>,
        origName: String?
    ): Type = this

    open fun genericCount(root: String): Int = 0
    protected open fun setSelfOf(self: GenericType) {
        bounds.filter { !(it is GenericType) || it is GenericType.Self }.forEach { it.setSelfOf(self) }
    }
}

open class TypeConstructor(override val name: String, override val bounds: List<Type>) : Type(name, bounds) {
    override fun genericCount(root: String): Int = 0
    override fun toString(): String = "$name<${bounds.joinToString(separator = "") { "$it" }}>"
}

data class TypeConstructorMutable(override val name: String, override var bounds: List<Type>) :
    TypeConstructor(name, bounds) {
    override fun genericCount(root: String): Int =
        bounds.filterNot { it is GenericType && it.name == root }.fold(0, { a, t -> a + t.genericCount(root) })

    override fun resolve(lookup: Map<String, GenericType.GenericTypeMutable>): Type =
        also { bounds = bounds.map { it.resolve(lookup) } }

    override fun asImmutable(
        cache: MutableMap<String, GenericType>,
        origName: String?
    ): Type =
        TypeConstructor(name, bounds.map { it.asImmutable(cache, origName) })
}

open class GenericType(override val name: String, override val bounds: List<Type>) : Type(name, bounds) {
    class Self(override val name: String) : GenericType(name, emptyList()) {
        override fun toString(): String = "($name)"

        private var _selfOf: GenericType? = null

        val selfOf: GenericType
            get() = _selfOf ?: run { throw IllegalStateException("Reference before initialization") }

        override fun setSelfOf(self: GenericType) {
            if (_selfOf == null) {
                _selfOf = self
            } else {
                throw IllegalStateException("_selfOf may be initialized only once")
            }
        }
    }

    data class GenericTypeMutable(
        override val name: String,
        override var bounds: List<Type>,
        var toResolve: Boolean
    ) : GenericType(name, bounds) {
        override fun genericCount(root: String): Int =
            bounds.filterNot { it is GenericType && it.name == root }
                .fold(0, { a, t -> a + t.genericCount(root) }) + 1

        override fun resolve(lookup: Map<String, GenericTypeMutable>): GenericTypeMutable = if (toResolve) {
            lookup.getValue(name)
        } else {
            this.also { bounds = bounds.map { it.resolve(lookup) } }
        }

        override fun asImmutable(
            cache: MutableMap<String, GenericType>,
            origName: String?
        ): GenericType = when {
            origName == name -> Self(name)
            cache.containsKey(name) -> cache.getValue(name)
            else -> GenericType(
                name,
                bounds.map { it.asImmutable(cache, origName ?: name) }).also { cache += it.name to it }
        }
    }

    override fun toString(): String = "$name: ${bounds.joinToString(separator = "") { "$it" }}"

    companion object {
        private fun fromTypeParameter(t: TypeParameterDescriptor): GenericTypeMutable = GenericTypeMutable(
            t.name.asString(),
            t.upperBounds.map { from(it) },
            toResolve = false
        )

        private fun from(t: KotlinType): Type =
            when (val d = t.constructor.declarationDescriptor) {
                is TypeParameterDescriptor -> GenericTypeMutable(
                    d.name.toString(),
                    emptyList(),
                    toResolve = true
                )
                else -> TypeConstructorMutable(
                    t.constructorName.orEmpty(),
                    t.arguments.map { fromProjection(it) }
                )
            }


        private fun fromProjection(t: TypeProjection): Type =
            if (t.isStarProjection) {
                TypeConstructorMutable("kotlin.Any", emptyList())
            } else {
                from(t.type)
            }

        fun List<TypeParameterDescriptor>.toGenericTypes(): List<GenericType> =
            this.map(::fromTypeParameter).associateBy { it.name }.let { lookup ->
                lookup.map { (_, v) -> v.resolve(lookup) }
                    .sortedBy { it.genericCount(it.name) }.let { it.asImmutable() }
            }

        private val KotlinType.constructorName
            get() = constructor.declarationDescriptor?.fqNameSafe?.asString()

        private fun List<GenericTypeMutable>.asImmutable(): List<GenericType> =
            HashMap<String, GenericType>().let { cache -> this.map { it.asImmutable(cache, null) } }
                .also { it.forEach { g -> g.setSelfOf(g) } }
    }
}