package org.jetbrains.dokka.plugability

internal class LazyEvaluated<T : Any> private constructor(private val recipe: ((DokkaBaseContext) -> T)? = null, private var value: T? = null) {

    internal fun get(context: DokkaBaseContext): T {
        if(value == null) {
            value = recipe?.invoke(context)
        }
        return value ?: throw AssertionError("Incorrect initialized LazyEvaluated instance")
    }

    companion object {
        fun <T : Any> fromInstance(value: T) = LazyEvaluated(value = value)
        fun <T : Any> fromRecipe(recipe: (DokkaBaseContext) -> T) = LazyEvaluated(recipe = recipe)
    }
}
