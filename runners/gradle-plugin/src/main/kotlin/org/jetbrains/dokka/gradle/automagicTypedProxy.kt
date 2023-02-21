package org.jetbrains.dokka.gradle

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


/**
 * Warning! Hard reflection magic used here.
 *
 * Creates [java.lang.reflect.Proxy] with pass through invocation algorithm,
 * to create access proxy for [delegate].
 */
internal inline fun <reified T : Any> dynamicCast(
    noinline delegate: () -> Any,
): DynamicCastDelegate<T> =
    DynamicCastDelegate(T::class, delegate)


internal class DynamicCastDelegate<out T : Any>(
    private val cls: KClass<T>,
    delegateProvider: () -> Any,
) : InvocationHandler {

    private val delegate = delegateProvider()

    private val delegateName get() = delegate.javaClass.name
    private val delegateMethods get() = delegate.javaClass.methods

    private val proxy: T by lazy {
        val proxy = Proxy.newProxyInstance(
            delegate.javaClass.classLoader,
            arrayOf(cls.java),
            this,
        )
        @Suppress("UNCHECKED_CAST")
        proxy as T
    }

    override fun invoke(
        proxy: Any,
        method: Method,
        args: Array<out Any?>?
    ): Any? {
        val delegateMethod = delegateMethods.firstOrNull { it matches method }
            ?: throw UnsupportedOperationException("$delegateName : $method args:${args?.joinToString()}")

        try {
            return when (args) {
                null -> delegateMethod.invoke(delegate)
                else -> delegateMethod.invoke(delegate, *args)
            }
        } catch (ex: InvocationTargetException) {
            throw ex.targetException
        }
    }

    /** Delegated value provider */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = proxy

    companion object {
        private infix fun Method.matches(other: Method): Boolean =
            this.name == other.name && this.parameterTypes.contentEquals(other.parameterTypes)
    }
}
