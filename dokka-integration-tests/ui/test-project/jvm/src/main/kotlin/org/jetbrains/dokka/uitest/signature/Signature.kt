package org.jetbrains.dokka.uitest.signature

import org.jetbrains.dokka.uitest.types.SimpleKotlinAnnotationClass
import org.jetbrains.dokka.uitest.types.SimpleKotlinClass
import org.jetbrains.dokka.uitest.types.SimpleKotlinInterface

class Signature(
    firstParam: String = "hello",
    secondParam: Int = 0,
    input: SimpleKotlinClass,
    lastLambda: SimpleKotlinClass.(param: @SimpleKotlinAnnotationClass String) -> SimpleKotlinClass
) : @SimpleKotlinAnnotationClass SimpleKotlinInterface

@SimpleKotlinAnnotationClass
fun signature(
    @SimpleKotlinAnnotationClass firstParam: String,
    secondParam: Int = 0,
    input: SimpleKotlinClass = SimpleKotlinClass(),
    lastLambda: SimpleKotlinClass.(param: String) -> SimpleKotlinClass
): Pair<SimpleKotlinClass, @SimpleKotlinAnnotationClass SimpleKotlinClass> {
    return input to input
}

fun signatureSimple(i: Int, f: Float) {}

public suspend inline operator fun <T> SimpleKotlinClass.invoke(
    noinline block: suspend SimpleKotlinClass.() -> T
): T = TODO()
