package org.jetbrains.dokka.kotlinAsJava.conversions

import org.jetbrains.dokka.kotlinAsJava.JavaTypeWrapper
import org.jetbrains.dokka.links.*
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Function
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType

fun String.getAsPrimitive(): JvmPrimitiveType? = org.jetbrains.kotlin.builtins.PrimitiveType.values()
    .find { it.typeFqName.asString() == this }
    ?.let { JvmPrimitiveType.get(it) }

fun TypeWrapper.getAsType(classId: ClassId, fqName: String, top: Boolean): TypeWrapper {
    val ctorFqName = fqName.takeIf { top }?.getAsPrimitive()?.name?.toLowerCase()
    return JavaTypeWrapper(
        constructorFqName = ctorFqName ?: classId.asString(),
        arguments = arguments.mapNotNull { it.asJava(false) },
        dri = classId.toDRI(dri),
        isPrimitive = ctorFqName != null
    )
}

fun TypeWrapper?.asJava(top: Boolean = true): TypeWrapper? = this?.constructorFqName
    ?.takeUnless { it.endsWith(".Unit") }
    ?.let { fqName ->
        fqName.mapToJava()
            ?.let { getAsType(it, fqName, top) } ?: this
    }

fun Class.asJava(): Class = Class(
    dri, name, kind,
    constructors.map { it.asJava() },
    (functions + properties.flatMap { it.accessors }).map { it.asJava() },
    properties, classes.map { it.asJava() }, expected, actual, extra
)

fun tcAsJava(tc: TypeConstructor): TypeReference =
    tc.fullyQualifiedName.mapToJava()
        ?.let { tc.copy(
            fullyQualifiedName = it.asString(),
            params = tc.params.map { it.asJava() }
        ) } ?: tc

fun tpAsJava(tp: TypeParam): TypeReference =
    tp.copy(bounds = tp.bounds.map{it.asJava()})

fun TypeReference.asJava(): TypeReference = when (this) {
    is TypeConstructor -> tcAsJava(this)
    is TypeParam -> tpAsJava(this)
    else -> this
}

fun Callable.asJava(): Callable = this.let {
    it.copy(params = it.params.mapNotNull { (it as? TypeConstructor)?.asJava() }) ?: this
}

fun Parameter.asJava(): Parameter = Parameter(
    dri.copy(callable = dri.callable?.asJava()),
    name,
    type.asJava()!!,
    actual,
    extra
)

fun Function.asJava(): Function {
    val newName = when {
        isConstructor -> "init"
        else -> name
    }
    return Function(
        dri,
        newName,
        returnType.asJava(),
        isConstructor,
        receiver,
        parameters.map { it.asJava() },
        expected,
        actual,
        extra,
        sourceLocation
    )
}

private fun String.mapToJava(): ClassId? =
    JavaToKotlinClassMap.mapKotlinToJava(FqName(this).toUnsafe())

fun ClassId.toDRI(dri: DRI?): DRI = DRI(
    packageName = packageFqName.asString(),
    classNames = classNames(),
    callable = dri?.callable?.asJava(),
    extra = null,
    target = null
)

fun ClassId.classNames(): String =
    shortClassName.identifier + (outerClassId?.classNames()?.let { ".$it" } ?: "")

fun Function.asStatic(): Function = also { it.extra.add(STATIC) }

fun Property.withClass(className: String, dri: DRI): Property {
    val nDri = dri.withClass(className).copy(
        callable = getDescriptor()?.let { Callable.from(it) }
    )
    return Property(
        nDri, name, receiver, expected, actual, extra, type, accessors, isVar, sourceLocation
    )
}

fun Function.withClass(className: String, dri: DRI): Function {
    val nDri = dri.withClass(className).copy(
        callable = getDescriptor()?.let { Callable.from(it) }
    )
    return Function(
        nDri, name, returnType, isConstructor, receiver, parameters, expected, actual, extra, sourceLocation
    )
}

fun Function.getDescriptor(): FunctionDescriptor? = platformInfo.mapNotNull { it.descriptor }
    .firstOrNull()?.let { it as? FunctionDescriptor }

fun Property.getDescriptor(): PropertyDescriptor? = platformInfo.mapNotNull { it.descriptor }
    .firstOrNull()?.let { it as? PropertyDescriptor }
