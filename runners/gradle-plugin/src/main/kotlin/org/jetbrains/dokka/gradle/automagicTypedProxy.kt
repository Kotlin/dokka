package org.jetbrains.dokka.gradle

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty


/**
 * Warning! Hard reflection magic used here.
 *
 * Creates [java.lang.reflect.Proxy] with pass through invocation algorithm,
 * to create access proxy for [delegate].
 */
internal inline fun <reified T : Any> dynamicCast(
    classLoader: ClassLoader = T::class.java.classLoader,
    noinline delegate: () -> Any,
): DynamicCastDelegate<T> =
    DynamicCastDelegate(T::class, classLoader, delegate)


internal class DynamicCastDelegate<out T : Any>(
    private val cls: KClass<T>,
    private val classLoader: ClassLoader,
    delegateProvider: () -> Any,
) : InvocationHandler, ReadOnlyProperty<Any?, T> {

    private val delegate = delegateProvider()

    private val delegateName get() = delegate.javaClass.name
    private val delegateMethods get() = delegate.javaClass.methods

    private val proxy: T by lazy {
        val proxy = Proxy.newProxyInstance(
            classLoader,
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
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = proxy

    companion object {
        private infix fun Method.matches(other: Method): Boolean =
            this.name == other.name && this.parameterTypes.contentEquals(other.parameterTypes)
    }
}
