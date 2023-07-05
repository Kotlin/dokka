package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.translator

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.kotlin.descriptors.ClassDescriptor

/**
 * Service translating [ClassDescriptor]s of symbols defined outside of documented project to [DClasslike]s.
 */
internal fun interface ExternalClasslikesTranslator {
    fun translateClassDescriptor(descriptor: ClassDescriptor, sourceSet: DokkaSourceSet): DClasslike
}
