package org.jetbrains.dokka.base.translators.descriptors

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

class DefaultExternalDocumentablesProvider(context: DokkaContext) : ExternalDocumentablesProvider {
    private val analysis = context.plugin<DokkaBase>().querySingle { kotlinAnalysis }

    private val translator = context.plugin<DokkaBase>().querySingle { externalClasslikesTranslator }

    override fun findClasslike(dri: DRI, sourceSet: DokkaSourceSet): DClasslike? {
        val pkg = dri.packageName?.let { FqName(it) } ?: FqName.ROOT
        val names = dri.classNames?.split('.') ?: return null

        val packageDsc = analysis[sourceSet].facade.moduleDescriptor.getPackage(pkg)
        val classDsc = names.fold<String, DeclarationDescriptor?>(packageDsc) { dsc, name ->
            dsc?.scope?.getDescriptorsFiltered { it.identifier == name }
                ?.filterIsInstance<ClassDescriptor>()
                ?.firstOrNull()
        }

        return (classDsc as? ClassDescriptor)?.let { translator.translateDescriptor(it, sourceSet) }
    }

    private val DeclarationDescriptor.scope: MemberScope
        get() = when (this) {
            is PackageViewDescriptor -> memberScope
            is ClassDescriptor -> unsubstitutedMemberScope
            else -> throw IllegalArgumentException("Unexpected type of descriptor: ${this::class}")
        }
}