package utils

import org.junit.jupiter.api.Tag


/**
 * Run a test only for descriptors, not symbols.
 *
 * In theory, these tests can be fixed
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("onlyDescriptors")
annotation class OnlyDescriptors(val reason: String = "")

/**
 * Run a test only for descriptors, not symbols.
 *
 * These tests cannot be fixed until Analysis API does not support MPP
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("onlyDescriptorsMPP")
annotation class OnlyDescriptorsMPP(val reason: String = "")


/**
 * For test containing .java code
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("javaCode")
annotation class JavaCode

/**
 * For test containing .java code
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
@Tag("usingJDK")
annotation class  UsingJDK