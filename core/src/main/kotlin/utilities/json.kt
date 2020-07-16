package org.jetbrains.dokka.utilities

import com.google.gson.Gson
import kotlin.reflect.KType
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

private val gson = Gson()

@Suppress("unused")
@PublishedApi
internal class TypeReference<T> private constructor(
    internal val type: KType
) {
    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        internal inline operator fun <reified T> invoke(): TypeReference<T> = TypeReference(typeOf<T>())
    }
}

@PublishedApi
internal fun toJsonString(value: Any?): String {
    return gson.toJson(value)
}

@PublishedApi
internal inline fun <reified T : Any> parseJson(json: String): T {
    return parseJson(json, TypeReference())
}

@OptIn(ExperimentalStdlibApi::class)
@PublishedApi
internal fun <T : Any> parseJson(json: String, typeReference: TypeReference<T>): T {
    return gson.fromJson(json, typeReference.type.javaType)
}
