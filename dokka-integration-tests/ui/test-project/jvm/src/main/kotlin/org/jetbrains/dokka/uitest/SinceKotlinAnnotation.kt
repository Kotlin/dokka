package org.jetbrains.dokka.uitest

import kotlin.SinceKotlin

/**
 * This demonstrates the [SinceKotlin] annotation
 */
@SinceKotlin("1.4")
class SinceKotlinAnnotation {

    /**
     * Description for one
     */
    @SinceKotlin("1.9.0")
    fun one() {}

    /**
     * Description for two with more tags
     *
     * @author someone
     * @since 1.7.20
     */
    @SinceKotlin("1.7.20")
    fun two() {}

    @SinceKotlin("1.5")
    fun three() {}
}
