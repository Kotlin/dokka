package org.jetbrains.dokka.uitest.types

/**
 * This is just a simple class
 */
class SimpleKotlinClass : SimpleKotlinInterface {

    /**
     * This is just a simple function
     */
    fun simpleJvmFunction() {}

    /**
     * This is just a simple extension
     */
    fun String.simpleJvmExtensionWithinClass() {}

    class InnerClass {}

    companion object CompanionObject {
        class NestedClass {}
    }
}
