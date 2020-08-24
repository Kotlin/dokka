package org.jetbrains.dokka.test

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <reified T> assertIsInstance(obj: Any?): T {
    contract {
        returns() implies (obj is T)
    }

    if (obj is T) {
        return obj
    }

    throw AssertionError("Expected instance of type ${T::class.qualifiedName} but found $obj")
}
