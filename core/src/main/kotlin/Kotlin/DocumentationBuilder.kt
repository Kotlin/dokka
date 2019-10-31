package org.jetbrains.dokka

import com.google.inject.Inject
import com.intellij.psi.PsiJavaFile
import org.jetbrains.dokka.DokkaConfiguration.PassConfiguration
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.findTopMostOverriddenDescriptors
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.util.supertypesWithAny
import kotlin.reflect.KClass
import com.google.inject.name.Named as GuiceNamed

private fun isExtensionForExternalClass(
    extensionFunctionDescriptor: DeclarationDescriptor,
    extensionReceiverDescriptor: DeclarationDescriptor,
    allFqNames: Collection<FqName>
): Boolean {
    val extensionFunctionPackage =
        DescriptorUtils.getParentOfType(extensionFunctionDescriptor, PackageFragmentDescriptor::class.java)
    val extensionReceiverPackage =
        DescriptorUtils.getParentOfType(extensionReceiverDescriptor, PackageFragmentDescriptor::class.java)
    return extensionFunctionPackage != null && extensionReceiverPackage != null &&
            extensionFunctionPackage.fqName != extensionReceiverPackage.fqName &&
            extensionReceiverPackage.fqName !in allFqNames
}

interface PackageDocumentationBuilder {
    fun buildPackageDocumentation(
        documentationBuilder: DocumentationBuilder,
        packageName: FqName,
        packageNode: DocumentationNodes.Package,
        declarations: List<DeclarationDescriptor>,
        allFqNames: Collection<FqName>
    )
}

interface DefaultPlatformsProvider {
    fun getDefaultPlatforms(descriptor: DeclarationDescriptor): List<String>
}

val ignoredSupertypes = setOf(
    "kotlin.Annotation", "kotlin.Enum", "kotlin.Any"
)

class DocumentationBuilder
@Inject constructor(
    val resolutionFacade: DokkaResolutionFacade,
    val passConfiguration: DokkaConfiguration.PassConfiguration,
    val logger: DokkaLogger
) {

    private fun DocumentationNodes.Class.appendSupertype(descriptor: ClassDescriptor, superType: KotlinType, backref: Boolean) {
        val unwrappedType = superType.unwrap()
        if (unwrappedType is AbbreviatedType) {
            appendSupertype(descriptor, unwrappedType.abbreviation, backref)
        } else {
            appendType(unwrappedType, descriptor)
        }
    }

    private fun DocumentationNodes.Class.appendType(
        kotlinType: KotlinType?,
        descriptor: ClassDescriptor?
    ) {
        if (kotlinType == null)
            return
        (kotlinType.unwrap() as? AbbreviatedType)?.let {
            return appendType(it.abbreviation, descriptor)
        }

        if (kotlinType.isDynamic()) {
            append(kind.createNode("dynamic", descriptor), RefKind.Detail)
            return
        }

        val classifierDescriptor = kotlinType.constructor.declarationDescriptor
        val name = when (classifierDescriptor) {
            is ClassDescriptor -> {
                if (classifierDescriptor.isCompanionObject) {
                    classifierDescriptor.containingDeclaration.name.asString() +
                            "." + classifierDescriptor.name.asString()
                } else {
                    classifierDescriptor.name.asString()
                }
            }
            is Named -> classifierDescriptor.name.asString()
            else -> "<anonymous>"
        }
        val node = kind.createNode(name, descriptor)

        append(node, RefKind.Detail)
        for (typeArgument in kotlinType.arguments) {
            node.appendProjection(typeArgument, null)
        }
    }

    fun DocumentationNode<*>.appendChild(descriptor: DeclarationDescriptor) {
        if (!descriptor.isGenerated() && descriptor.isDocumented(passConfiguration)) {
            val node = descriptor.build()
            append(node, kind)
        }
    }

    fun DocumentationNode.appendMember(descriptor: DeclarationDescriptor) {
        if (!descriptor.isGenerated() && descriptor.isDocumented(passConfiguration)) {
            val existingNode = members.firstOrNull { it.descriptor?.fqNameSafe == descriptor.fqNameSafe }
            if (existingNode != null) {
                if (descriptor is ClassDescriptor) {
                    val membersToDocument = descriptor.collectMembersToDocument()
                    for ((memberDescriptor, _, _) in membersToDocument) {
                        if (memberDescriptor is ClassDescriptor) {
                            existingNode.appendMember(memberDescriptor)   // recurse into nested classes
                        } else {
                            if (members.any { it.descriptor?.fqNameSafe == memberDescriptor.fqNameSafe }) {
                                existingNode.appendClassMember(memberDescriptor)
                            }
                        }
                    }
                }
            } else {
                appendChild(descriptor, RefKind.Member)
            }
        }
    }

    private fun DocumentationNode.appendClassMember(descriptor: DeclarationDescriptor) {
        if (descriptor !is CallableMemberDescriptor || descriptor.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            val descriptorToUse = if (descriptor is ConstructorDescriptor) descriptor else descriptor.original
            appendChild(descriptorToUse, RefKind.Member)
        }
    }

    fun DocumentationNodes.Module.appendFragments(
        fragments: Collection<PackageFragmentDescriptor>,
        packageDocumentationBuilder: PackageDocumentationBuilder
    ) {
        val allFqNames = fragments.filter { it.isDocumented(passConfiguration) }.map { it.fqName }.distinct()

        for (packageName in allFqNames) {
            if (packageName.isRoot && !passConfiguration.includeRootPackage) continue
            val declarations = fragments.filter { it.fqName == packageName }
                .flatMap { it.getMemberScope().getContributedDescriptors() }

            if (passConfiguration.skipEmptyPackages && declarations.none { it.isDocumented(passConfiguration) }) continue
            logger.info("  package $packageName: ${declarations.count()} declarations")
            val packageNode = findOrCreatePackageNode(this, packageName.asString())
            packageDocumentationBuilder.buildPackageDocumentation(
                this@DocumentationBuilder, packageName, packageNode,
                declarations, allFqNames
            )
        }
    }

    fun findOrCreatePackageNode(
        module: DocumentationNodes.Module,
        packageName: String
    ): DocumentationNode<*> {
        val node = module?.member(DocumentationNodes.Package::class) ?: DocumentationNodes.Package(packageName)
        if (module != null && node !in module.members) {
            module.append(node, RefKind.Member)
        }
        return node
    }

    fun propagateExtensionFunctionsToSubclasses(
        fragments: Collection<PackageFragmentDescriptor>,
        resolutionFacade: DokkaResolutionFacade
    ) {

        val moduleDescriptor = resolutionFacade.moduleDescriptor

        // Wide-collect all view descriptors
        val allPackageViewDescriptors = generateSequence(listOf(moduleDescriptor.getPackage(FqName.ROOT))) { packages ->
            packages
                .flatMap { pkg ->
                    moduleDescriptor.getSubPackagesOf(pkg.fqName) { true }
                }.map { fqName ->
                    moduleDescriptor.getPackage(fqName)
                }.takeUnless { it.isEmpty() }
        }.flatten()

        val allDescriptors =
            if (passConfiguration.collectInheritedExtensionsFromLibraries) {
                allPackageViewDescriptors.map { it.memberScope }
            } else {
                fragments.asSequence().map { it.getMemberScope() }
            }.flatMap {
                it.getDescriptorsFiltered(
                    DescriptorKindFilter.CALLABLES
                ).asSequence()
            }

        val allExtensionFunctions =
            allDescriptors
                .filterIsInstance<CallableMemberDescriptor>()
                .filter { it.extensionReceiverParameter != null }
        val extensionFunctionsByName = allExtensionFunctions.groupBy { it.name }

        fun isIgnoredReceiverType(type: KotlinType) =
            type.isDynamic() ||
                    type.isAnyOrNullableAny() ||
                    (type.isTypeParameter() && type.immediateSupertypes().all { it.isAnyOrNullableAny() })


        for (extensionFunction in allExtensionFunctions) {
            val extensionReceiverParameter = extensionFunction.extensionReceiverParameter!!
            if (extensionFunction.dispatchReceiverParameter != null) continue
            val possiblyShadowingFunctions = extensionFunctionsByName[extensionFunction.name]
                ?.filter { fn -> fn.canShadow(extensionFunction) }
                    ?: emptyList()

            if (isIgnoredReceiverType(extensionReceiverParameter.type)) continue
        }
    }

    private fun CallableMemberDescriptor.canShadow(other: CallableMemberDescriptor): Boolean {
        if (this == other) return false
        if (this is PropertyDescriptor && other is PropertyDescriptor) {
            return true
        }
        if (this is FunctionDescriptor && other is FunctionDescriptor) {
            val parameters1 = valueParameters
            val parameters2 = other.valueParameters
            if (parameters1.size != parameters2.size) {
                return false
            }
            for ((p1, p2) in parameters1 zip parameters2) {
                if (p1.type != p2.type) {
                    return false
                }
            }
            return true
        }
        return false
    }

    fun DeclarationDescriptor.build(): DocumentationNode = when (this) {
        is ClassifierDescriptor -> build()
        is ConstructorDescriptor -> build()
        is PropertyDescriptor -> build()
        is FunctionDescriptor -> build()
        is ValueParameterDescriptor -> build()
        is ReceiverParameterDescriptor -> build()
        else -> throw IllegalStateException("Descriptor $this is not known")
    }

    fun ClassifierDescriptor.build(external: Boolean = false): DocumentationNode = when (this) {
        is ClassDescriptor -> build(this, external)
        is TypeAliasDescriptor -> build()
        is TypeParameterDescriptor -> build()
        else -> throw IllegalStateException("Descriptor $this is not known")
    }

    fun TypeAliasDescriptor.build(): DocumentationNode {
        val node = DocumentationNodes.TypeAlias(name.asString(), this)
        node.appendType(underlyingType, this, DocumentationNodes.TypeAliasUnderlyingType::class)
        return node
    }

    fun ClassDescriptor.build(descriptor: DeclarationDescriptor, external: Boolean = false): DocumentationNode {
        val node = when {
            kind == ClassKind.OBJECT -> DocumentationNodes.Object(descriptor.name.asString(), descriptor)
            kind == ClassKind.INTERFACE -> DocumentationNodes.Interface(descriptor.name.asString(), descriptor)
            kind == ClassKind.ENUM_CLASS -> DocumentationNodes.Enum(descriptor.name.asString(), descriptor)
            kind == ClassKind.ANNOTATION_CLASS -> DocumentationNodes.AnnotationClass(descriptor.name.asString(), descriptor)
            kind == ClassKind.ENUM_ENTRY -> DocumentationNodes.EnumItem(descriptor.name.asString(), descriptor)
            isSubclassOfThrowable() -> DocumentationNodes.Exception(descriptor.name.asString(), descriptor)
            else -> DocumentationNodes.Class(descriptor.name.asString(), descriptor)
        }
        supertypesWithAnyPrecise().forEach {
            node.appendSupertype(this, it, !external)
        }
        if (!external) {
            for ((membersDescriptor, _, _) in collectMembersToDocument()) {
                node.appendClassMember(membersDescriptor)
            }
        }
        return node
    }

    data class ClassMember(
        val descriptor: DeclarationDescriptor,
        val inheritedLinkKind: RefKind = RefKind.InheritedMember,
        val extraModifier: String? = null
    )

    private fun ClassDescriptor.collectMembersToDocument(): List<ClassMember> {
        val result = arrayListOf<ClassMember>()
        if (kind != ClassKind.OBJECT && kind != ClassKind.ENUM_ENTRY) {
            val constructorsToDocument = if (kind == ClassKind.ENUM_CLASS)
                constructors.filter { it.valueParameters.size > 0 }
            else
                constructors
            constructorsToDocument.mapTo(result) { ClassMember(it) }
        }

        defaultType.memberScope.getContributedDescriptors()
            .filter { it != companionObjectDescriptor }
            .mapTo(result) { ClassMember(it) }

        staticScope.getContributedDescriptors()
            .mapTo(result) { ClassMember(it, extraModifier = "static") }

        val companionObjectDescriptor = companionObjectDescriptor
        if (companionObjectDescriptor != null && companionObjectDescriptor.isDocumented(passConfiguration)) {
            val descriptors = companionObjectDescriptor.defaultType.memberScope.getContributedDescriptors()
            val descriptorsToDocument = descriptors.filter { it !is CallableDescriptor || !it.isInheritedFromAny() }
            descriptorsToDocument.mapTo(result) {
                ClassMember(it, inheritedLinkKind = RefKind.InheritedCompanionObjectMember)
            }

            if (companionObjectDescriptor.getAllSuperclassesWithoutAny().isNotEmpty()
                || companionObjectDescriptor.getSuperInterfaces().isNotEmpty()) {
                result += ClassMember(companionObjectDescriptor)
            }
        }
        return result
    }

    private fun CallableDescriptor.isInheritedFromAny(): Boolean {
        return findTopMostOverriddenDescriptors().any {
            DescriptorUtils.getFqNameSafe(it.containingDeclaration).asString() == "kotlin.Any"
        }
    }

    private fun ClassDescriptor.isSubclassOfThrowable(): Boolean =
        defaultType.supertypes().any { it.constructor.declarationDescriptor == builtIns.throwable }

    fun ConstructorDescriptor.build(): DocumentationNode =
        DocumentationNodes.Constructor(name.asString(), this)

    private fun CallableMemberDescriptor.inCompanionObject(): Boolean {
        val containingDeclaration = containingDeclaration
        if ((containingDeclaration as? ClassDescriptor)?.isCompanionObject == true) {
            return true
        }
        val receiver = extensionReceiverParameter
        return (receiver?.type?.constructor?.declarationDescriptor as? ClassDescriptor)?.isCompanionObject ?: false
    }

    fun FunctionDescriptor.build(): DocumentationNode =
        if (inCompanionObject())
            DocumentationNodes.CompanionObjectFunction(name.asString(),this)
        else
            DocumentationNodes.Function(name.asString(),this)

    fun PropertyDescriptor.build(): DocumentationNode =
        DocumentationNodes.Property(name.asString(), this)

    fun ValueParameterDescriptor.build(): DocumentationNode =
        DocumentationNodes.Parameter(name.asString(), this)

    fun TypeParameterDescriptor.build(): DocumentationNode =
        DocumentationNodes.TypeParameter(name.asString(), this)

    fun ReceiverParameterDescriptor.build(): DocumentationNode =
        DocumentationNodes.Receiver(name.asString(), this)
}

fun DeclarationDescriptor.isDocumented(passConfiguration: DokkaConfiguration.PassConfiguration): Boolean {
    return (passConfiguration.effectivePackageOptions(fqNameSafe).includeNonPublic
            || this !is MemberDescriptor
            || this.visibility.isPublicAPI)
            && !isDocumentationSuppressed(passConfiguration)
            && (!passConfiguration.effectivePackageOptions(fqNameSafe).skipDeprecated || !isDeprecated())
}

private fun DeclarationDescriptor.isGenerated() =
    this is CallableMemberDescriptor && kind != CallableMemberDescriptor.Kind.DECLARATION

class KotlinPackageDocumentationBuilder : PackageDocumentationBuilder {

    override fun buildPackageDocumentation(
        documentationBuilder: DocumentationBuilder,
        packageName: FqName,
        packageNode: DocumentationNode,
        declarations: List<DeclarationDescriptor>,
        allFqNames: Collection<FqName>
    ) {
        declarations.forEach { descriptor ->
            with(documentationBuilder) {
                if (descriptor.isDocumented(passConfiguration)) {
                    packageNode.appendMember(descriptor)
                }
            }
        }
    }
}

class KotlinJavaDocumentationBuilder
@Inject constructor(
    val resolutionFacade: DokkaResolutionFacade,
    val documentationBuilder: DocumentationBuilder,
    val passConfiguration: DokkaConfiguration.PassConfiguration,
    val logger: DokkaLogger
) : JavaDocumentationBuilder {
    override fun appendFile(file: PsiJavaFile, module: DocumentationModule, packageContent: Map<String, Content>) {
        val classDescriptors = file.classes.map {
            it.getJavaClassDescriptor(resolutionFacade)
        }

        if (classDescriptors.any { it != null && it.isDocumented(passConfiguration) }) {
            val packageNode = documentationBuilder.findOrCreatePackageNode(module, file.packageName)

            for (descriptor in classDescriptors.filterNotNull()) {
                with(documentationBuilder) {
                    packageNode.appendChild(descriptor, RefKind.Member)
                }
            }
        }
    }
}

fun DeclarationDescriptor.isDocumentationSuppressed(passConfiguration: DokkaConfiguration.PassConfiguration): Boolean {

    if (passConfiguration.effectivePackageOptions(fqNameSafe).suppress) return true

    val path = this.findPsi()?.containingFile?.virtualFile?.path
    if (path != null) {
        if (path in passConfiguration.suppressedFiles) return true
    }

    val doc = findKDoc()
    if (doc is KDocSection && doc.findTagByName("suppress") != null) return true

    return hasSuppressDocTag(sourcePsi())
}

fun DeclarationDescriptor.sourcePsi() =
    ((original as? DeclarationDescriptorWithSource)?.source as? PsiSourceElement)?.psi

fun DeclarationDescriptor.isDeprecated(): Boolean = annotations.any {
    DescriptorUtils.getFqName(it.type.constructor.declarationDescriptor!!).asString() == "kotlin.Deprecated"
} || (this is ConstructorDescriptor && containingDeclaration.isDeprecated())

fun DeclarationDescriptor.signature(): String {
    if (this != original) return original.signature()
    return when (this) {
        is ClassDescriptor,
        is PackageFragmentDescriptor,
        is PackageViewDescriptor,
        is TypeAliasDescriptor -> DescriptorUtils.getFqName(this).asString()

        is PropertyDescriptor -> containingDeclaration.signature() + "$" + name + receiverSignature()
        is FunctionDescriptor -> containingDeclaration.signature() + "$" + name + parameterSignature()
        is ValueParameterDescriptor -> containingDeclaration.signature() + "/" + name
        is TypeParameterDescriptor -> containingDeclaration.signature() + "*" + name
        is ReceiverParameterDescriptor -> containingDeclaration.signature() + "/" + name
        else -> throw UnsupportedOperationException("Don't know how to calculate signature for $this")
    }
}

fun PropertyDescriptor.receiverSignature(): String {
    val receiver = extensionReceiverParameter
    if (receiver != null) {
        return "#" + receiver.type.signature()
    }
    return ""
}

fun CallableMemberDescriptor.parameterSignature(): String {
    val params = valueParameters.map { it.type }.toMutableList()
    val extensionReceiver = extensionReceiverParameter
    if (extensionReceiver != null) {
        params.add(0, extensionReceiver.type)
    }
    return params.joinToString(prefix = "(", postfix = ")") { it.signature() }
}

fun KotlinType.signature(): String {
    val visited = hashSetOf<KotlinType>()

    fun KotlinType.signatureRecursive(): String {
        if (this in visited) {
            return ""
        }
        visited.add(this)

        val declarationDescriptor = constructor.declarationDescriptor ?: return "<null>"
        val typeName = DescriptorUtils.getFqName(declarationDescriptor).asString()
        if (arguments.isEmpty()) {
            return typeName
        }
        return typeName + arguments.joinToString(prefix = "((", postfix = "))") { it.type.signatureRecursive() }
    }

    return signatureRecursive()
}

fun DeclarationDescriptor.signatureWithSourceLocation(): String {
    val signature = signature()
    val sourceLocation = sourceLocation()
    return if (sourceLocation != null) "$signature ($sourceLocation)" else signature
}

fun DeclarationDescriptor.sourceLocation(): String? {
    val psi = sourcePsi()
    if (psi != null) {
        val fileName = psi.containingFile.name
        val lineNumber = psi.lineNumber()
        return if (lineNumber != null) "$fileName:$lineNumber" else fileName
    }
    return null
}

fun ClassDescriptor.supertypesWithAnyPrecise(): Collection<KotlinType> {
    if (KotlinBuiltIns.isAny(this)) {
        return emptyList()
    }
    return typeConstructor.supertypesWithAny()
}

fun PassConfiguration.effectivePackageOptions(pack: String): DokkaConfiguration.PackageOptions {
    val rootPackageOptions = PackageOptionsImpl("", includeNonPublic, reportUndocumented, skipDeprecated, false)
    return perPackageOptions.firstOrNull { pack == it.prefix || pack.startsWith(it.prefix + ".") } ?: rootPackageOptions
}

fun PassConfiguration.effectivePackageOptions(pack: FqName): DokkaConfiguration.PackageOptions =
    effectivePackageOptions(pack.asString())

