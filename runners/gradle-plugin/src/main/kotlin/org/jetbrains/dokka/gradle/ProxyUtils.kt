package org.jetbrains.dokka.gradle

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy


/**
 * Warning! Hard reflection magic used here.
 *
 * Creates [java.lang.reflect.Proxy] with pass through invocation algorithm,
 * to create access proxy for [delegate] into [targetClassLoader].
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> automagicTypedProxy(targetClassLoader: ClassLoader, delegate: Any): T =
        automagicProxy(targetClassLoader, T::class.java, delegate) as T


/**
 * Warning! Hard reflection magic used here.
 *
 * Creates [java.lang.reflect.Proxy] with pass through invocation algorithm,
 * to create access proxy for [delegate] into [targetClassLoader].
 *
 */
fun automagicProxy(targetClassLoader: ClassLoader, targetType: Class<*>, delegate: Any): Any =
        Proxy.newProxyInstance(
            targetClassLoader,
            arrayOf(targetType),
            DelegatedInvocationHandler(delegate)
        )

class DelegatedInvocationHandler(private val delegate: Any) : InvocationHandler {

    @Throws(Throwable::class)
    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
        val delegateMethod = delegate.javaClass.getMethod(method.name, *method.parameterTypes)
        try {
            delegateMethod.isAccessible = true
            return delegateMethod.invoke(delegate, *(args ?: emptyArray()))
        } catch (ex: InvocationTargetException) {
            throw ex.targetException
        }
    }
}
