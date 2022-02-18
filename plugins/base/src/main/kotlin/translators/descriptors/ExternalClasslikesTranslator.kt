package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.kotlin.descriptors.ClassDescriptor

/**
 * Service translating [ClassDescriptor]s of symbols defined outside of documented project to [DClasslike]s.
 */
fun interface ExternalClasslikesTranslator {
    fun translateClassDescriptor(descriptor: ClassDescriptor, sourceSet: DokkaSourceSet): DClasslike
}