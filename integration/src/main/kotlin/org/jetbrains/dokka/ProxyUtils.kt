package org.jetbrains.dokka

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.function.Predicate


/**
 * Warning! Hard reflection magic used here.
 *
 * Creates [java.lang.reflect.Proxy] with pass through invocation algorithm,
 * to create access proxy for [delegate] from [targetClassLoader] to [delegateClassLoader].
 *
 * Every object type contained in method calls will be translated to proxy, if [filter.test] will success for it's class
 *
 */
@Suppress("UNCHECKED_CAST")
fun <T> automagicTypedProxy(targetClassLoader: ClassLoader, targetType: Class<T>,
                            delegateClassLoader: ClassLoader, delegate: Any,
                            filter: Predicate<Class<*>>): T =
        automagicProxy(targetClassLoader, targetType, delegateClassLoader, delegate, filter) as T


/**
 * Warning! Hard reflection magic used here.
 *
 * Creates [java.lang.reflect.Proxy] with pass through invocation algorithm,
 * to create access proxy for [delegate] from [targetClassLoader] to [delegateClassLoader].
 *
 * Every object type contained in method calls will be translated to proxy, if [filter.test] will success for it's class
 *
 */
fun automagicProxy(targetClassLoader: ClassLoader, targetType: Class<*>,
                   delegateClassLoader: ClassLoader, delegate: Any,
                   filter: Predicate<Class<*>>): Any =
        Proxy.newProxyInstance(
                targetClassLoader,
                arrayOf(targetType),
                DelegatedInvocationHandler(
                        delegate,
                        delegateClassLoader,
                        filter
                )
        )

class DelegatedInvocationHandler(private val delegate: Any,
                                 private val delegateClassLoader: ClassLoader,
                                 private val filter: Predicate<Class<*>>)
    : InvocationHandler {

    @Throws(Throwable::class)
    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any? {
        val (argTypes, argValues) = method.parameterTypes.zip(args ?: emptyArray()).map { typeAndValue ->
            val (type, value) = typeAndValue
            if (filter.test(type)) {
                val newType = delegateClassLoader.loadClass(type.name)
                val newValue = value?.let {
                    automagicProxy(delegateClassLoader, newType, type.classLoader, value, filter)
                }
                newType to newValue
            } else
                typeAndValue
        }.unzip()

        val delegateMethod = delegate.javaClass.getMethod(method.name, *argTypes.toTypedArray())
        try {
            delegateMethod.isAccessible = true
            return delegateMethod.invoke(delegate, *argValues.toTypedArray())
        } catch (ex: InvocationTargetException) {
            throw ex.targetException
        }

    }
}
