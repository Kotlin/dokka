/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.translator

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.CompilerDescriptorAnalysisPlugin
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.analysis.kotlin.documentable.ExternalDocumentableProvider
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

internal class DescriptorExternalDocumentablesProvider(context: DokkaContext) : ExternalDocumentableProvider {
    private val analysis = context.plugin<CompilerDescriptorAnalysisPlugin>().querySingle { kotlinAnalysis }

    private val translator: ExternalClasslikesTranslator = DefaultDescriptorToDocumentableTranslator(context)

    override fun getClasslike(dri: DRI, sourceSet: DokkaSourceSet): DClasslike? {
        val pkg = dri.packageName?.let { FqName(it) } ?: FqName.ROOT
        val names = dri.classNames?.split('.') ?: return null

        val packageDsc = analysis[sourceSet].moduleDescriptor.getPackage(pkg)
        val classDsc = names.fold<String, DeclarationDescriptor?>(packageDsc) { dsc, name ->
            dsc?.scope?.getDescriptorsFiltered { it.identifier == name }
                ?.filterIsInstance<ClassDescriptor>()
                ?.firstOrNull()
        }

        return (classDsc as? ClassDescriptor)?.let { translator.translateClassDescriptor(it, sourceSet) }
    }

    private val DeclarationDescriptor.scope: MemberScope
        get() = when (this) {
            is PackageViewDescriptor -> memberScope
            is ClassDescriptor -> unsubstitutedMemberScope
            else -> throw IllegalArgumentException("Unexpected type of descriptor: ${this::class}")
        }
}
