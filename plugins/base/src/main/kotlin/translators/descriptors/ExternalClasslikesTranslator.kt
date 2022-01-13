package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.kotlin.descriptors.ClassDescriptor

interface ExternalClasslikesTranslator {
    fun translateDescriptor(descriptor: ClassDescriptor, sourceSet: DokkaSourceSet): DClasslike
}