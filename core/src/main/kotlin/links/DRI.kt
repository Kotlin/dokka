package org.jetbrains.dokka.links

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.text.ParseException

/**
 * [DRI] stands for DokkaResourceIdentifier
 */
data class DRI(
    val packageName: String? = null,
    val classNames: String? = null,
    val callable: Callable? = null,
    val target: Int? = null,
    val extra: String? = null
) {

    constructor(
        packageName: String? = null,
        classNames: String? = null,
        callableName: String? = null,
        signature: String? = null,
        target: Int? = null,
        extra: String? = null
    ) : this(packageName, classNames, Callable.from(callableName, signature), target, extra)

    override fun toString(): String =
        "${packageName.orEmpty()}/${classNames.orEmpty()}/${callable?.name.orEmpty()}/${callable?.signature().orEmpty()}/${target?.toString().orEmpty()}/${extra.orEmpty()}"

    companion object {
        fun from(s: String): DRI = try {
            s.split('/')
                .map { it.takeIf(String::isNotBlank) }
                .let { (packageName, classNames, callableName, callableSignature, target, ext) ->
                    DRI(
                        packageName,
                        classNames,
                        try {
                            Callable.from(callableName, callableSignature)
                        } catch (e: ParseException) {
                            null
                        },
                        target?.toInt(),
                        ext
                    )
                }
        } catch (e: Throwable) {
            throw ParseException(s, 0)
        }

        fun from(descriptor: DeclarationDescriptor) = descriptor.parentsWithSelf.run {
            val callable = firstIsInstanceOrNull<CallableDescriptor>()
            val params = callable?.let { listOfNotNull(it.extensionReceiverParameter) + it.valueParameters }.orEmpty()
            DRI(
                firstIsInstanceOrNull<PackageFragmentDescriptor>()?.fqName?.asString(),
                filterIsInstance<ClassDescriptor>().toList().takeIf { it.isNotEmpty() }?.asReversed()
                    ?.joinToString(separator = ".") { it.name.asString() },
                callable?.let { Callable.from(it) },
                firstIsInstanceOrNull<ParameterDescriptor>()?.let { params.indexOf(it) },
                null
            )
        }

        val topLevel = DRI()
    }
}

fun DRI.withClass(name: String) = copy(classNames = classNames.orEmpty() + ".$name")

val DRI.parent: DRI
    get() = when {
        extra != null -> copy(extra = null)
        target != null -> copy(target = null)
        callable != null -> copy(callable = null)
        classNames != null -> copy(classNames = classNames.substringBeforeLast('.').takeIf { it.isNotBlank() })
        else -> DRI.topLevel
    }

data class Callable(val name: String, val receiver: String, val returnType: String, val params: List<String>) {
    fun signature() = "$receiver#$returnType#${params.joinToString("#")}"

    companion object {
        fun from(name: String?, signature: String?): Callable = try {
            signature.toString()
                .split('#', ignoreCase = false, limit = 3)
                .let { (receiver, returnType, params) ->
                    Callable(
                        name.toString(),
                        receiver,
                        returnType,
                        params.split('#')
                    )
                }
        } catch (e: Throwable) {
            throw ParseException(signature, 0)
        }

        fun from(s: String): Callable = try {
            s.split('/').let { (name, signature) -> from(name, signature) }
        } catch (e: Throwable) {
            throw ParseException(s, 0)
        }

        fun from(descriptor: CallableDescriptor) = with(descriptor) {
            Callable(
                name.asString(),
                extensionReceiverParameter?.value?.type?.constructorName.orEmpty(),
                returnType?.constructorName.orEmpty(),
                valueParameters.map { it.type.constructorName.orEmpty() }
            )
        }
    }
}

data class ClassReference(val dri: DRI, val subs: MutableList<ClassReference> = mutableListOf()) {
    private val subsText = subs.takeIf { it.isNotEmpty() }?.toString().orEmpty()
    override fun toString() = "$dri$subsText"
}

private operator fun <T> List<T>.component6(): T = get(5)

private val KotlinType.constructorName
    get() = constructor.declarationDescriptor?.name?.asString()

