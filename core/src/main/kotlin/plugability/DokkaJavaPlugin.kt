package org.jetbrains.dokka.plugability

import org.jetbrains.dokka.DokkaConfiguration

internal fun defaultExtensionName(ext: ExtensionPoint<*>) = "${ext.pointName}Impl"

class ExtensionBuilderStart internal constructor(val plugin: DokkaJavaPlugin){
    fun <T: Any>  extensionPoint(ext: ExtensionPoint<T>): ProvidedExtension<T> =
        ProvidedExtension(ext, defaultExtensionName(ext), plugin)
}

class ProvidedExtension<T: Any> internal constructor(
    val ext: ExtensionPoint<T>,
    val name: String,
    val plugin: DokkaJavaPlugin){
    fun name(name: String): ProvidedExtension<T> = ProvidedExtension(ext, name, plugin)

    fun fromInstance(inst: T): ExtensionBuilder<T> = createBuilder(
        LazyEvaluated.fromInstance(
            inst
        )
    )
    fun fromRecipe(recipe: (DokkaContext) -> T): ExtensionBuilder<T> = createBuilder(
        LazyEvaluated.fromRecipe(recipe)
    )
    private fun createBuilder(action: LazyEvaluated<T>) =
        ExtensionBuilder(name, ext, action,
            emptyList(), emptyList(),
            OverrideKind.None, emptyList()).also { plugin.checkBuilder(it) }
}

data class ExtensionBuilder<T: Any> internal constructor(
    internal val name: String,
    internal val ext: ExtensionPoint<T>,
    private val action: LazyEvaluated<T>,
    private val before: List<Extension<*, *, *>>,
    private val after: List<Extension<*, *, *>>,
    private val override: OverrideKind = OverrideKind.None,
    private val conditions: List<(DokkaConfiguration) -> Boolean>
){
    fun build(clazz: Class<*>): Extension<T, *, *>  = Extension(
        ext,
        clazz.name,
        name,
        action,
        OrderingKind.ByDsl {
            before(*before.toTypedArray())
            after(*after.toTypedArray())
        },
        override,
        conditions
    )

    fun overrideExtension(extension: Extension<T, *, *>): ExtensionBuilder<T> = copy(override = OverrideKind.Present(extension))

    fun before(vararg ext: Extension<*, *, *>): ExtensionBuilder<T> = copy(before = before + ext)

    fun after(vararg ext: Extension<*, *, *>): ExtensionBuilder<T> = copy(after = after + ext)

    fun addCondition(c: (DokkaConfiguration) -> Boolean): ExtensionBuilder<T> = copy(conditions = conditions + c)
}

abstract class DokkaJavaPlugin: DokkaPlugin() {
    private var declaredSoFar: Set<Pair<String, ExtensionPoint<*>>> = emptySet()

    internal fun checkBuilder(builder: ExtensionBuilder<*>) {
        val descr = Pair(builder.name, builder.ext)
        if (declaredSoFar.contains(descr)) {
            if (builder.name == defaultExtensionName(builder.ext))
                throw AssertionError(
                    "Duplicated extension for ${builder.ext} using default name (${builder.name}). " +
                        "Use 'name(<newName>)' to fix this problem")
            else throw AssertionError("Duplicated extension for ${builder.ext} named ${builder.name}")
        }
        declaredSoFar = declaredSoFar + descr
    }

    fun <T: DokkaPlugin> plugin(clazz: Class<T>): T? =
        context?.plugin(clazz.kotlin) ?: throwIllegalQuery()


    fun <T: Any> extend(func: (ExtensionBuilderStart) -> ExtensionBuilder<T>): Lazy<Extension<T, *, *>> =
        lazy { func(ExtensionBuilderStart()).build(this.javaClass) }.also { unsafeInstall(it) }

}