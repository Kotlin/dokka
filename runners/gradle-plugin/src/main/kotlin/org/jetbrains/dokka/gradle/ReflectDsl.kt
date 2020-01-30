package org.jetbrains.dokka

import kotlin.reflect.*
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

object ReflectDsl {

    class CallOrPropAccess(private val receiver: Any?,
                           private val clz: KClass<*>,
                           private val selector: String) {

        @Suppress("UNCHECKED_CAST")
        operator fun <T : Any?> invoke(vararg a: Any?): T {
            return func!!.call(receiver, *a) as T
        }

        operator fun get(s: String): CallOrPropAccess {
            return v<Any?>()!![s]
        }

        val func: KFunction<*>? by lazy { clz.memberFunctions.find { it.name == selector } }
        val prop: KProperty<*>? by lazy { clz.memberProperties.find { it.name == selector } }

        fun takeIfIsFunc(): CallOrPropAccess? = if (func != null) this else null

        fun takeIfIsProp(): CallOrPropAccess? = if (prop != null) this else null

        @Suppress("UNCHECKED_CAST")
        fun <T : Any?> v(): T {
            val prop = prop!!
            return try {
                prop.getter.apply { isAccessible = true }.call(receiver) as T
            } catch (e: KotlinNullPointerException) {
                // Hack around kotlin-reflect bug KT-18480
                val jclass = clz.java
                val customGetterName = prop.getter.name
                val getterName = if (customGetterName.startsWith("<")) "get" + prop.name.capitalize() else customGetterName
                val getter = jclass.getDeclaredMethod(getterName)
                getter.isAccessible = true

                getter.invoke(receiver) as T

            }
        }

        @Suppress("UNCHECKED_CAST")
        fun v(x: Any?) {
            (prop as KMutableProperty).setter.apply { isAccessible = true }.call(receiver, x)
        }


    }

    operator fun Any.get(s: String): CallOrPropAccess {
        val clz = this.javaClass.kotlin
        return CallOrPropAccess(this, clz, s)
    }

    operator fun Any.get(s: String, clz: Class<*>): CallOrPropAccess {
        val kclz = clz.kotlin
        return CallOrPropAccess(this, kclz, s)
    }

    operator fun Any.get(s: String, clz: KClass<*>): CallOrPropAccess {
        return CallOrPropAccess(this, clz, s)
    }

    inline infix fun Any.isInstance(clz: Class<*>?): Boolean = clz != null && clz.isAssignableFrom(this.javaClass)
    inline infix fun Any.isNotInstance(clz: Class<*>?): Boolean = !(this isInstance clz)
}