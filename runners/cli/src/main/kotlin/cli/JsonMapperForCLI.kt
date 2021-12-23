package org.jetbrains.dokka

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import java.io.File

// THIS IS COPIED FROM BASE SINCE IT NEEDS TO BE INSTANTIATED ON THE SAME CLASS LOADER AS PLUGINS

private val objectMapper = run {
    val module = SimpleModule().apply {
        addSerializer(FileSerializer)
    }
    jacksonObjectMapper()
        .registerModule(module)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

@PublishedApi
internal class TypeReference<T> private constructor(
    internal val jackson: com.fasterxml.jackson.core.type.TypeReference<T>
) {
    companion object {
        internal inline operator fun <reified T> invoke(): TypeReference<T> = TypeReference(jacksonTypeRef())
    }
}

inline fun <reified T : Any> parseJson(json: String): T = parseJson(json, TypeReference())

@PublishedApi
internal fun <T : Any> parseJson(json: String, typeReference: TypeReference<T>): T =
    objectMapper.readValue(json, typeReference.jackson)

private object FileSerializer : StdScalarSerializer<File>(File::class.java) {
    override fun serialize(value: File, g: JsonGenerator, provider: SerializerProvider) {
        g.writeString(value.path)
    }
}

data class GlobalDokkaConfiguration(
    val perPackageOptions: List<PackageOptionsImpl>?,
    val externalDocumentationLinks: List<ExternalDocumentationLinkImpl>?,
    val sourceLinks: List<SourceLinkDefinitionImpl>?
)
