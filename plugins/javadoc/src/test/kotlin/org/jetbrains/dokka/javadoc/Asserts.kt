package org.jetbrains.dokka.javadoc

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// TODO replace with assertIs<T> from kotlin-test as part of #2924
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
