package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
interface SampleProviderFactory {
    fun build(): SampleProvider
}

/**
 * It's closeable. Otherwise, there is a chance of memory leak.
 */
@InternalDokkaApi
interface SampleProvider: AutoCloseable {
    class SampleSnippet(val imports: String, val body:String)


    /**
     * @return [SampleSnippet] or null if it has not found by [fqLink]
     */
    fun getSample(sourceSet: DokkaConfiguration.DokkaSourceSet, fqLink: String): SampleSnippet?
}