package org.jetbrains.dokka

import com.google.inject.Inject
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiJavaFile
import org.jetbrains.dokka.DokkaConfiguration.PassConfiguration
import org.jetbrains.dokka.Kotlin.DescriptorDocumentationParser
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.coroutines.hasFunctionOrSuspendFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.idea.util.fuzzyExtensionReceiverType
import org.jetbrains.kotlin.idea.util.makeNotNullable
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.addRemoveModifier.MODIFIERS_ORDER
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.findTopMostOverriddenDescriptors
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes
import org.jetbrains.kotlin.types.typeUtil.isAnyOrNullableAny
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.util.supertypesWithAny
import com.google.inject.name.Named as GuiceNamed

private fun isExtensionForExternalClass(extensionFunctionDescriptor: DeclarationDescriptor,
                                        extensionReceiverDescriptor: DeclarationDescriptor,
                                        allFqNames: Collection<FqName>): Boolean {
    val extensionFunctionPackage = DescriptorUtils.getParentOfType(extensionFunctionDescriptor, PackageFragmentDescriptor::class.java)
    val extensionReceiverPackage = DescriptorUtils.getParentOfType(extensionReceiverDescriptor, PackageFragmentDescriptor::class.java)
    return extensionFunctionPackage != null && extensionReceiverPackage != null &&
            extensionFunctionPackage.fqName != extensionReceiverPackage.fqName &&
            extensionReceiverPackage.fqName !in allFqNames
}

interface PackageDocumentationBuilder {
    fun buildPackageDocumentation(documentationBuilder: DocumentationBuilder,
                                  packageName: FqName,
                                  packageNode: DocumentationNode,
                                  declarations: List<DeclarationDescriptor>,
                                  allFqNames: Collection<FqName>)
}

interface DefaultPlatformsProvider {
    fun getDefaultPlatforms(descriptor: DeclarationDescriptor): List<String>
}

val ignoredSupertypes = setOf(
    "kotlin.Annotation", "kotlin.Enum", "kotlin.Any"
)

class DocumentationBuilder
@Inject constructor(val resolutionFacade: DokkaResolutionFacade,
                    val descriptorDocumentationParser: DescriptorDocumentationParser,
                    val passConfiguration: DokkaConfiguration.PassConfiguration,
                    val refGraph: NodeReferenceGraph,
                    val platformNodeRegistry: PlatformNodeRegistry,
                    val logger: DokkaLogger,
                    val linkResolver: DeclarationLinkResolver,
                    val defaultPlatformsProvider: DefaultPlatformsProvider) {
    val boringBuiltinClasses = setOf(
            "kotlin.Unit", "kotlin.Byte", "kotlin.Short", "kotlin.Int", "kotlin.Long", "kotlin.Char", "kotlin.Boolean",
            "kotlin.Float", "kotlin.Double", "kotlin.String", "kotlin.Array", "kotlin.Any")
    val knownModifiers = setOf(
            KtTokens.PUBLIC_KEYWORD, KtTokens.PROTECTED_KEYWORD, KtTokens.INTERNAL_KEYWORD, KtTokens.PRIVATE_KEYWORD,
            KtTokens.OPEN_KEYWORD, KtTokens.FINAL_KEYWORD, KtTokens.ABSTRACT_KEYWORD, KtTokens.SEALED_KEYWORD,
            KtTokens.OVERRIDE_KEYWORD, KtTokens.INLINE_KEYWORD)

    fun link(node: DocumentationNode, descriptor: DeclarationDescriptor, kind: RefKind) {
        refGraph.link(node, descriptor.signature(), kind)
    }

    fun link(fromDescriptor: DeclarationDescriptor?, toDescriptor: DeclarationDescriptor?, kind: RefKind) {
        if (fromDescriptor != null && toDescriptor != null) {
            refGraph.link(fromDescriptor.signature(), toDescriptor.signature(), kind)
        }
    }

    fun register(descriptor: DeclarationDescriptor, node: DocumentationNode) {
        refGraph.register(descriptor.signature(), node)
    }

    fun <T> nodeForDescriptor(
        descriptor: T,
        kind: NodeKind,
        external: Boolean = false
    ): DocumentationNode where T : DeclarationDescriptor, T : Named {
        val (doc, callback) =
                if (external) {
                    Content.Empty to { node -> }
                } else {
                    descriptorDocumentationParser.parseDocumentationAndDetails(
                        descriptor,
                        kind == NodeKind.Parameter
                    )
                }
        val node = DocumentationNode(descriptor.name.asString(), doc, kind).withModifiers(descriptor)
        node.appendSignature(descriptor)
        callback(node)
        return node
    }

    private fun DocumentationNode.withModifiers(descriptor: DeclarationDescriptor): DocumentationNode {
        if (descriptor is MemberDescriptor) {
            appendVisibility(descriptor)
            if (descriptor !is ConstructorDescriptor) {
                appendModality(descriptor)
            }
        }
        return this
    }

    fun DocumentationNode.appendModality(descriptor: MemberDescriptor) {
        var modality = descriptor.modality
        if (modality == Modality.OPEN) {
            val containingClass = descriptor.containingDeclaration as? ClassDescriptor
            if (containingClass?.modality == Modality.FINAL) {
                modality = Modality.FINAL
            }
        }
        val modifier = modality.name.toLowerCase()
        appendTextNode(modifier, NodeKind.Modifier)
    }

    fun DocumentationNode.appendInline(descriptor: DeclarationDescriptor, psi: KtModifierListOwner) {
        if (!psi.hasModifier(KtTokens.INLINE_KEYWORD)) return
        if (descriptor is FunctionDescriptor
                && descriptor.valueParameters.none { it.hasFunctionOrSuspendFunctionType }) return
        appendTextNode(KtTokens.INLINE_KEYWORD.value, NodeKind.Modifier)
    }

    fun DocumentationNode.appendVisibility(descriptor: DeclarationDescriptorWithVisibility) {
        val modifier = descriptor.visibility.normalize().displayName
        appendTextNode(modifier, NodeKind.Modifier)
    }

    fun DocumentationNode.appendSupertype(descriptor: ClassDescriptor, superType: KotlinType, backref: Boolean) {
        val unwrappedType = superType.unwrap()
        if (unwrappedType is AbbreviatedType) {
            appendSupertype(descriptor, unwrappedType.abbreviation, backref)
        } else {
            appendType(unwrappedType, NodeKind.Supertype)
            val superclass = unwrappedType.constructor.declarationDescriptor
            if (backref) {
                link(superclass, descriptor, RefKind.Inheritor)
            }
            link(descriptor, superclass, RefKind.Superclass)
        }
    }

    fun DocumentationNode.appendProjection(projection: TypeProjection, kind: NodeKind = NodeKind.Type) {
        if (projection.isStarProjection) {
            appendTextNode("*", NodeKind.Type)
        } else {
            appendType(projection.type, kind, projection.projectionKind.label)
        }
    }

    fun DocumentationNode.appendType(kotlinType: KotlinType?, kind: NodeKind = NodeKind.Type, prefix: String = "") {
        if (kotlinType == null)
            return
        (kotlinType.unwrap() as? AbbreviatedType)?.let {
            return appendType(it.abbreviation)
        }

        if (kotlinType.isDynamic()) {
            append(DocumentationNode("dynamic", Content.Empty, kind), RefKind.Detail)
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
        val node = DocumentationNode(name, Content.Empty, kind)
        if (prefix != "") {
            node.appendTextNode(prefix, NodeKind.Modifier)
        }
        if (kotlinType.isNullabilityFlexible()) {
            node.appendTextNode("!", NodeKind.NullabilityModifier)
        } else if (kotlinType.isMarkedNullable) {
            node.appendTextNode("?", NodeKind.NullabilityModifier)
        }
        if (classifierDescriptor != null) {
            val externalLink =
                linkResolver.externalDocumentationLinkResolver.buildExternalDocumentationLink(classifierDescriptor)
            if (externalLink != null) {
                if (classifierDescriptor !is TypeParameterDescriptor) {
                    val targetNode =
                        refGraph.lookup(classifierDescriptor.signature()) ?: classifierDescriptor.build(true)
                    node.append(targetNode, RefKind.ExternalType)
                    node.append(DocumentationNode(externalLink, Content.Empty, NodeKind.ExternalLink), RefKind.Link)
                }
            } else {
                link(
                    node, classifierDescriptor,
                    if (classifierDescriptor.isBoringBuiltinClass()) RefKind.HiddenLink else RefKind.Link
                )
            }
            if (classifierDescriptor !is TypeParameterDescriptor) {
                node.append(
                    DocumentationNode(
                        classifierDescriptor.fqNameUnsafe.asString(),
                        Content.Empty,
                        NodeKind.QualifiedName
                    ), RefKind.Detail
                )
            }
        }


        append(node, RefKind.Detail)
        node.appendAnnotations(kotlinType)
        for (typeArgument in kotlinType.arguments) {
            node.appendProjection(typeArgument)
        }
    }

    fun ClassifierDescriptor.isBoringBuiltinClass(): Boolean =
            DescriptorUtils.getFqName(this).asString() in boringBuiltinClasses

    fun DocumentationNode.appendAnnotations(annotated: Annotated) {
        annotated.annotations.forEach {
            it.build()?.let { annotationNode ->
                if (annotationNode.isSinceKotlin()) {
                    appendSinceKotlin(annotationNode)
                }
                else {
                    val refKind = when {
                        it.isDocumented() ->
                            when {
                                annotationNode.isDeprecation() -> RefKind.Deprecation
                                else -> RefKind.Annotation
                            }
                        it.isHiddenInDocumentation() -> RefKind.HiddenAnnotation
                        else -> return@forEach
                    }
                    append(annotationNode, refKind)
                }

            }
        }
    }

    fun DocumentationNode.appendExternalLink(externalLink: String) {
        append(DocumentationNode(externalLink, Content.Empty, NodeKind.ExternalLink), RefKind.Link)
    }

    fun DocumentationNode.appendExternalLink(descriptor: DeclarationDescriptor) {
        val target = linkResolver.externalDocumentationLinkResolver.buildExternalDocumentationLink(descriptor)
        if (target != null) {
            appendExternalLink(target)
        }
    }

    fun DocumentationNode.appendSinceKotlin(annotation: DocumentationNode) {
        val kotlinVersion = annotation
                .detail(NodeKind.Parameter)
                .detail(NodeKind.Value)
                .name.removeSurrounding("\"")

        sinceKotlin = kotlinVersion
    }

    fun DocumentationNode.appendDefaultSinceKotlin() {
        if (sinceKotlin == null) {
            sinceKotlin = passConfiguration.sinceKotlin
        }
    }

    fun DocumentationNode.appendModifiers(descriptor: DeclarationDescriptor) {
        val psi = (descriptor as DeclarationDescriptorWithSource).source.getPsi() as? KtModifierListOwner ?: return
        KtTokens.MODIFIER_KEYWORDS_ARRAY.filter {
            it !in knownModifiers
        }.sortedBy {
            MODIFIERS_ORDER.indexOf(it)
        }.forEach {
            if (psi.hasModifier(it)) {
                appendTextNode(it.value, NodeKind.Modifier)
            }
        }
        appendInline(descriptor, psi)
    }

    fun DocumentationNode.appendDefaultPlatforms(descriptor: DeclarationDescriptor) {
        for (platform in defaultPlatformsProvider.getDefaultPlatforms(descriptor)) {
            append(platformNodeRegistry[platform], RefKind.Platform)
        }
    }

    fun DocumentationNode.isDeprecation() = name == "Deprecated" || name == "deprecated"

    fun DocumentationNode.isSinceKotlin() = name == "SinceKotlin" && kind == NodeKind.Annotation

    fun DocumentationNode.appendSourceLink(sourceElement: SourceElement) {
        appendSourceLink(sourceElement.getPsi(), passConfiguration.sourceLinks)
    }

    fun DocumentationNode.appendSignature(descriptor: DeclarationDescriptor) {
        appendTextNode(descriptor.signature(), NodeKind.Signature, RefKind.Detail)
    }

    fun DocumentationNode.appendChild(descriptor: DeclarationDescriptor, kind: RefKind): DocumentationNode? {
        if (!descriptor.isGenerated() && descriptor.isDocumented(passConfiguration)) {
            val node = descriptor.build()
            append(node, kind)
            return node
        }
        return null
    }

    fun createGroupNode(signature: String, nodes: List<DocumentationNode>) = (nodes.find { it.kind == NodeKind.GroupNode } ?:
            DocumentationNode(nodes.first().name, Content.Empty, NodeKind.GroupNode).apply {
                appendTextNode(signature, NodeKind.Signature, RefKind.Detail)
            })
            .also { groupNode ->
                nodes.forEach { node ->
                    if (node != groupNode) {
                        node.owner?.let { owner ->
                            node.dropReferences { it.to == owner && it.kind == RefKind.Owner }
                            owner.dropReferences { it.to == node && it.kind == RefKind.Member }
                            owner.append(groupNode, RefKind.Member)
                        }
                        groupNode.append(node, RefKind.Member)
                    }
                }
            }


    fun DocumentationNode.appendOrUpdateMember(descriptor: DeclarationDescriptor) {
        if (descriptor.isGenerated() || !descriptor.isDocumented(passConfiguration)) return

        val existingNode = refGraph.lookup(descriptor.signature())
        if (existingNode != null) {
            if (existingNode.kind == NodeKind.TypeAlias && descriptor is ClassDescriptor
                    || existingNode.kind == NodeKind.Class && descriptor is TypeAliasDescriptor) {
                val node = createGroupNode(descriptor.signature(), listOf(existingNode, descriptor.build()))
                register(descriptor, node)
                return
            }

            existingNode.updatePlatforms(descriptor)

            if (descriptor is ClassDescriptor) {
                val membersToDocument = descriptor.collectMembersToDocument()
                for ((memberDescriptor, inheritedLinkKind, extraModifier) in membersToDocument) {
                    if (memberDescriptor is ClassDescriptor) {
                        existingNode.appendOrUpdateMember(memberDescriptor)   // recurse into nested classes
                    }
                    else {
                        val existingMemberNode = refGraph.lookup(memberDescriptor.signature())
                        if (existingMemberNode != null) {
                            existingMemberNode.updatePlatforms(memberDescriptor)
                        }
                        else {
                            existingNode.appendClassMember(memberDescriptor, inheritedLinkKind, extraModifier)
                        }
                    }
                }
            }
        }
        else {
            appendChild(descriptor, RefKind.Member)
        }
    }

    private fun DocumentationNode.updatePlatforms(descriptor: DeclarationDescriptor) {
        for (platform in defaultPlatformsProvider.getDefaultPlatforms(descriptor) - platforms) {
            append(platformNodeRegistry[platform], RefKind.Platform)
        }
    }

    fun DocumentationNode.appendClassMember(descriptor: DeclarationDescriptor,
                                            inheritedLinkKind: RefKind = RefKind.InheritedMember,
                                            extraModifier: String?) {
        if (descriptor is CallableMemberDescriptor && descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            val baseDescriptor = descriptor.overriddenDescriptors.firstOrNull()
            if (baseDescriptor != null) {
                link(this, baseDescriptor, inheritedLinkKind)
            }
        } else {
            val descriptorToUse = if (descriptor is ConstructorDescriptor) descriptor else descriptor.original
            val child = appendChild(descriptorToUse, RefKind.Member)
            if (extraModifier != null) {
                child?.appendTextNode("static", NodeKind.Modifier)
            }
        }
    }

    fun DocumentationNode.appendInPageChildren(descriptors: Iterable<DeclarationDescriptor>, kind: RefKind) {
        descriptors.forEach { descriptor ->
            val node = appendChild(descriptor, kind)
            node?.addReferenceTo(this, RefKind.TopLevelPage)
        }
    }

    fun DocumentationModule.appendFragments(fragments: Collection<PackageFragmentDescriptor>,
                                            packageContent: Map<String, Content>,
                                            packageDocumentationBuilder: PackageDocumentationBuilder) {
        val allFqNames = fragments.filter { it.isDocumented(passConfiguration) }.map { it.fqName }.distinct()

        for (packageName in allFqNames) {
            if (packageName.isRoot && !passConfiguration.includeRootPackage) continue
            val declarations = fragments.filter { it.fqName == packageName }.flatMap { it.getMemberScope().getContributedDescriptors() }

            if (passConfiguration.skipEmptyPackages && declarations.none { it.isDocumented(passConfiguration) }) continue
            logger.info("  package $packageName: ${declarations.count()} declarations")
            val packageNode = findOrCreatePackageNode(this, packageName.asString(), packageContent, this@DocumentationBuilder.refGraph)
            packageDocumentationBuilder.buildPackageDocumentation(this@DocumentationBuilder, packageName, packageNode,
                    declarations, allFqNames)
        }

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


        val documentingDescriptors = fragments.flatMap { it.getMemberScope().getContributedDescriptors() }
        val documentingClasses = documentingDescriptors.filterIsInstance<ClassDescriptor>()

        val classHierarchy = buildClassHierarchy(documentingClasses)

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
            val subclasses =
                classHierarchy.filter { (key) -> key.isExtensionApplicable(extensionFunction) }
            if (subclasses.isEmpty()) continue
            subclasses.values.flatten().forEach { subclass ->
                if (subclass.isExtensionApplicable(extensionFunction) &&
                    possiblyShadowingFunctions.none { subclass.isExtensionApplicable(it) }) {

                    val hasExternalLink =
                        linkResolver.externalDocumentationLinkResolver.buildExternalDocumentationLink(
                            extensionFunction
                        ) != null
                    if (hasExternalLink) {
                        val containerDesc =
                            extensionFunction.containingDeclaration as? PackageFragmentDescriptor
                        if (containerDesc != null) {
                            val container = refGraph.lookup(containerDesc.signature())
                                    ?: containerDesc.buildExternal()
                            container.append(extensionFunction.buildExternal(), RefKind.Member)
                        }
                    }

                    refGraph.link(subclass.signature(), extensionFunction.signature(), RefKind.Extension)
                }
            }
        }
    }

    private fun ClassDescriptor.isExtensionApplicable(extensionFunction: CallableMemberDescriptor): Boolean {
        val receiverType = extensionFunction.fuzzyExtensionReceiverType()?.makeNotNullable()
        val classType = defaultType.toFuzzyType(declaredTypeParameters)
        return receiverType != null && classType.checkIsSubtypeOf(receiverType) != null
    }

    private fun buildClassHierarchy(classes: List<ClassDescriptor>): Map<ClassDescriptor, List<ClassDescriptor>> {
        val result = hashMapOf<ClassDescriptor, MutableList<ClassDescriptor>>()
        classes.forEach { cls ->
            TypeUtils.getAllSupertypes(cls.defaultType).forEach { supertype ->
                val classDescriptor = supertype.constructor.declarationDescriptor as? ClassDescriptor
                if (classDescriptor != null) {
                    val subtypesList = result.getOrPut(classDescriptor) { arrayListOf() }
                    subtypesList.add(cls)
                }
            }
        }
        return result
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

    fun PackageFragmentDescriptor.buildExternal(): DocumentationNode {
        val node = DocumentationNode(fqName.asString(), Content.Empty, NodeKind.Package)

        val externalLink = linkResolver.externalDocumentationLinkResolver.buildExternalDocumentationLink(this)
        if (externalLink != null) {
            node.append(DocumentationNode(externalLink, Content.Empty, NodeKind.ExternalLink), RefKind.Link)
        }
        register(this, node)
        return node
    }

    fun CallableDescriptor.buildExternal(): DocumentationNode = when(this) {
        is FunctionDescriptor -> build(true)
        is PropertyDescriptor -> build(true)
        else -> throw IllegalStateException("Descriptor $this is not known")
    }


    fun ClassifierDescriptor.build(external: Boolean = false): DocumentationNode = when (this) {
        is ClassDescriptor -> build(external)
        is TypeAliasDescriptor -> build(external)
        is TypeParameterDescriptor -> build()
        else -> throw IllegalStateException("Descriptor $this is not known")
    }

    fun TypeAliasDescriptor.build(external: Boolean = false): DocumentationNode {
        val node = nodeForDescriptor(this, NodeKind.TypeAlias)

        if (!external) {
            node.appendDefaultSinceKotlin()
            node.appendAnnotations(this)
        }
        node.appendModifiers(this)
        node.appendInPageChildren(typeConstructor.parameters, RefKind.Detail)

        node.appendType(underlyingType, NodeKind.TypeAliasUnderlyingType)

        if (!external) {
            node.appendSourceLink(source)
            node.appendDefaultPlatforms(this)
        }
        register(this, node)
        return node
    }

    fun ClassDescriptor.build(external: Boolean = false): DocumentationNode {
        val kind = when {
            kind == ClassKind.OBJECT -> NodeKind.Object
            kind == ClassKind.INTERFACE -> NodeKind.Interface
            kind == ClassKind.ENUM_CLASS -> NodeKind.Enum
            kind == ClassKind.ANNOTATION_CLASS -> NodeKind.AnnotationClass
            kind == ClassKind.ENUM_ENTRY -> NodeKind.EnumItem
            isSubclassOfThrowable() -> NodeKind.Exception
            else -> NodeKind.Class
        }
        val node = nodeForDescriptor(this, kind, external)
        register(this, node)
        supertypesWithAnyPrecise().forEach {
            node.appendSupertype(this, it, !external)
        }
        if (getKind() != ClassKind.OBJECT && getKind() != ClassKind.ENUM_ENTRY) {
            node.appendInPageChildren(typeConstructor.parameters, RefKind.Detail)
        }
        if (!external) {
            for ((descriptor, inheritedLinkKind, extraModifier) in collectMembersToDocument()) {
                node.appendClassMember(descriptor, inheritedLinkKind, extraModifier)
            }
            node.appendDefaultSinceKotlin()
            node.appendAnnotations(this)
        }
        node.appendModifiers(this)
        if (!external) {
            node.appendSourceLink(source)
            node.appendDefaultPlatforms(this)
        }
        return node
    }

    data class ClassMember(val descriptor: DeclarationDescriptor,
                           val inheritedLinkKind: RefKind = RefKind.InheritedMember,
                           val extraModifier: String? = null)

    fun ClassDescriptor.collectMembersToDocument(): List<ClassMember> {
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

    fun CallableDescriptor.isInheritedFromAny(): Boolean {
        return findTopMostOverriddenDescriptors().any {
            DescriptorUtils.getFqNameSafe(it.containingDeclaration).asString() == "kotlin.Any"
        }
    }

    fun ClassDescriptor.isSubclassOfThrowable(): Boolean =
            defaultType.supertypes().any { it.constructor.declarationDescriptor == builtIns.throwable }

    fun ConstructorDescriptor.build(): DocumentationNode {
        val node = nodeForDescriptor(this, NodeKind.Constructor)
        node.appendInPageChildren(valueParameters, RefKind.Detail)
        node.appendDefaultPlatforms(this)
        node.appendDefaultSinceKotlin()
        register(this, node)
        return node
    }

    private fun CallableMemberDescriptor.inCompanionObject(): Boolean {
        val containingDeclaration = containingDeclaration
        if ((containingDeclaration as? ClassDescriptor)?.isCompanionObject ?: false) {
            return true
        }
        val receiver = extensionReceiverParameter
        return (receiver?.type?.constructor?.declarationDescriptor as? ClassDescriptor)?.isCompanionObject ?: false
    }

    fun FunctionDescriptor.build(external: Boolean = false): DocumentationNode {
        if (ErrorUtils.containsErrorTypeInParameters(this) || ErrorUtils.containsErrorType(this.returnType)) {
            logger.warn("Found an unresolved type in ${signatureWithSourceLocation()}")
        }

        val node = nodeForDescriptor(this, if (inCompanionObject()) NodeKind.CompanionObjectFunction else NodeKind.Function, external)

        node.appendInPageChildren(typeParameters, RefKind.Detail)
        extensionReceiverParameter?.let { node.appendChild(it, RefKind.Detail) }
        node.appendInPageChildren(valueParameters, RefKind.Detail)
        node.appendType(returnType)
        if (!external) {
            node.appendDefaultSinceKotlin()
        }
        node.appendAnnotations(this)
        node.appendModifiers(this)
        if (!external) {
            node.appendSourceLink(source)
            node.appendDefaultPlatforms(this)
        } else {
            node.appendExternalLink(this)
        }

        overriddenDescriptors.forEach {
            addOverrideLink(it, this)
        }

        register(this, node)
        return node
    }

    fun addOverrideLink(baseClassFunction: CallableMemberDescriptor, overridingFunction: CallableMemberDescriptor) {
        val source = baseClassFunction.original.source.getPsi()
        if (source != null) {
            link(overridingFunction, baseClassFunction, RefKind.Override)
        } else {
            baseClassFunction.overriddenDescriptors.forEach {
                addOverrideLink(it, overridingFunction)
            }
        }
    }

    fun PropertyDescriptor.build(external: Boolean = false): DocumentationNode {
        val node = nodeForDescriptor(
            this,
            if (inCompanionObject()) NodeKind.CompanionObjectProperty else NodeKind.Property,
            external
        )
        node.appendInPageChildren(typeParameters, RefKind.Detail)
        extensionReceiverParameter?.let { node.appendChild(it, RefKind.Detail) }
        node.appendType(returnType)
        if (!external) {
            node.appendDefaultSinceKotlin()
        }
        node.appendAnnotations(this)
        node.appendModifiers(this)
        if (!external) {
            node.appendSourceLink(source)
            if (isVar) {
                node.appendTextNode("var", NodeKind.Modifier)
            }

            if (isConst) {
                this.compileTimeInitializer?.toDocumentationNode()?.let { node.append(it, RefKind.Detail) }
            }


            getter?.let {
                if (!it.isDefault) {
                    node.addAccessorDocumentation(descriptorDocumentationParser.parseDocumentation(it), "Getter")
                }
            }
            setter?.let {
                if (!it.isDefault) {
                    node.addAccessorDocumentation(descriptorDocumentationParser.parseDocumentation(it), "Setter")
                }
            }
            node.appendDefaultPlatforms(this)
        }
        if (external) {
            node.appendExternalLink(this)
        }

        overriddenDescriptors.forEach {
            addOverrideLink(it, this)
        }

        register(this, node)
        return node
    }

    fun DocumentationNode.addAccessorDocumentation(documentation: Content, prefix: String) {
        if (documentation == Content.Empty) return
        updateContent {
            if (!documentation.children.isEmpty()) {
                val section = addSection(prefix, null)
                documentation.children.forEach { section.append(it) }
            }
            documentation.sections.forEach {
                val section = addSection("$prefix ${it.tag}", it.subjectName)
                it.children.forEach { section.append(it) }
            }
        }
    }

    fun ValueParameterDescriptor.build(): DocumentationNode {
        val node = nodeForDescriptor(this, NodeKind.Parameter)
        node.appendType(varargElementType ?: type)
        if (declaresDefaultValue()) {
            val psi = source.getPsi() as? KtParameter
            if (psi != null) {
                val defaultValueText = psi.defaultValue?.text
                if (defaultValueText != null) {
                    node.appendTextNode(defaultValueText, NodeKind.Value)
                }
            }
        }
        node.appendDefaultSinceKotlin()
        node.appendAnnotations(this)
        node.appendModifiers(this)
        if (varargElementType != null && node.details(NodeKind.Modifier).none { it.name == "vararg" }) {
            node.appendTextNode("vararg", NodeKind.Modifier)
        }
        register(this, node)
        return node
    }

    fun TypeParameterDescriptor.build(): DocumentationNode {
        val doc = descriptorDocumentationParser.parseDocumentation(this)
        val name = name.asString()
        val prefix = variance.label

        val node = DocumentationNode(name, doc, NodeKind.TypeParameter)
        if (prefix != "") {
            node.appendTextNode(prefix, NodeKind.Modifier)
        }
        if (isReified) {
            node.appendTextNode("reified", NodeKind.Modifier)
        }

        for (constraint in upperBounds) {
            if (KotlinBuiltIns.isDefaultBound(constraint)) {
                continue
            }
            node.appendType(constraint, NodeKind.UpperBound)
        }
        register(this, node)
        return node
    }

    fun ReceiverParameterDescriptor.build(): DocumentationNode {
        var receiverClass: DeclarationDescriptor = type.constructor.declarationDescriptor!!
        if ((receiverClass as? ClassDescriptor)?.isCompanionObject ?: false) {
            receiverClass = receiverClass.containingDeclaration!!
        } else if (receiverClass is TypeParameterDescriptor) {
            val upperBoundClass = receiverClass.upperBounds.singleOrNull()?.constructor?.declarationDescriptor
            if (upperBoundClass != null) {
                receiverClass = upperBoundClass
            }
        }

        if ((containingDeclaration as? FunctionDescriptor)?.dispatchReceiverParameter == null) {
            link(receiverClass, containingDeclaration, RefKind.Extension)
        }

        val node = DocumentationNode(name.asString(), Content.Empty, NodeKind.Receiver)
        node.appendType(type)
        register(this, node)
        return node
    }

    fun AnnotationDescriptor.build(): DocumentationNode? {
        val annotationClass = type.constructor.declarationDescriptor
        if (annotationClass == null || ErrorUtils.isError(annotationClass)) {
            return null
        }
        val node = DocumentationNode(annotationClass.name.asString(), Content.Empty, NodeKind.Annotation)
        allValueArguments.forEach { (name, value) ->
            val valueNode = value.toDocumentationNode()
            if (valueNode != null) {
                val paramNode = DocumentationNode(name.asString(), Content.Empty, NodeKind.Parameter)
                paramNode.append(valueNode, RefKind.Detail)
                node.append(paramNode, RefKind.Detail)
            }
        }
        return node
    }

    fun ConstantValue<*>.toDocumentationNode(): DocumentationNode? = value.let { value ->
        val text = when (value) {
            is String ->
                "\"" + StringUtil.escapeStringCharacters(value) + "\""
            is EnumEntrySyntheticClassDescriptor ->
                value.containingDeclaration.name.asString() + "." + value.name.asString()
            is Pair<*, *> -> {
                val (classId, name) = value
                if (classId is ClassId && name is Name) {
                    classId.shortClassName.asString() + "." + name.asString()
                } else {
                    value.toString()
                }
            }
            else -> "$value"
        }
        DocumentationNode(text, Content.Empty, NodeKind.Value)
    }


    fun DocumentationNode.getParentForPackageMember(
        descriptor: DeclarationDescriptor,
        externalClassNodes: MutableMap<FqName, DocumentationNode>,
        allFqNames: Collection<FqName>,
        packageName: FqName
    ): DocumentationNode {
        if (descriptor is CallableMemberDescriptor) {
            val extensionClassDescriptor = descriptor.getExtensionClassDescriptor()
            if (extensionClassDescriptor != null && isExtensionForExternalClass(descriptor, extensionClassDescriptor, allFqNames) &&
                !ErrorUtils.isError(extensionClassDescriptor)) {
                val fqName = DescriptorUtils.getFqNameSafe(extensionClassDescriptor)
                return externalClassNodes.getOrPut(fqName) {
                    val newNode = DocumentationNode(fqName.asString(), Content.Empty, NodeKind.ExternalClass)
                    val externalLink = linkResolver.externalDocumentationLinkResolver.buildExternalDocumentationLink(extensionClassDescriptor)
                    if (externalLink != null) {
                        newNode.append(DocumentationNode(externalLink, Content.Empty, NodeKind.ExternalLink), RefKind.Link)
                    }
                    append(newNode, RefKind.Member)
                    refGraph.register("${packageName.asString()}:${extensionClassDescriptor.signature()}", newNode)
                    newNode
                }
            }
        }
        return this
    }

}

fun DeclarationDescriptor.isDocumented(passConfiguration: DokkaConfiguration.PassConfiguration): Boolean {
    return (passConfiguration.effectivePackageOptions(fqNameSafe).includeNonPublic
            || this !is MemberDescriptor
            || this.visibility.isPublicAPI)
            && !isDocumentationSuppressed(passConfiguration)
            && (!passConfiguration.effectivePackageOptions(fqNameSafe).skipDeprecated || !isDeprecated())
}

private fun DeclarationDescriptor.isGenerated() = this is CallableMemberDescriptor && kind != CallableMemberDescriptor.Kind.DECLARATION

class KotlinPackageDocumentationBuilder : PackageDocumentationBuilder {
    override fun buildPackageDocumentation(documentationBuilder: DocumentationBuilder,
                                           packageName: FqName,
                                           packageNode: DocumentationNode,
                                           declarations: List<DeclarationDescriptor>,
                                           allFqNames: Collection<FqName>) {
        val externalClassNodes = hashMapOf<FqName, DocumentationNode>()
        declarations.forEach { descriptor ->
            with(documentationBuilder) {
                if (descriptor.isDocumented(passConfiguration)) {
                    val parent = packageNode.getParentForPackageMember(
                        descriptor,
                        externalClassNodes,
                        allFqNames,
                        packageName
                    )
                    parent.appendOrUpdateMember(descriptor)
                }
            }
        }
    }
}

class KotlinJavaDocumentationBuilder
@Inject constructor(val resolutionFacade: DokkaResolutionFacade,
                    val documentationBuilder: DocumentationBuilder,
                    val passConfiguration: DokkaConfiguration.PassConfiguration,
                    val logger: DokkaLogger) : JavaDocumentationBuilder {
    override fun appendFile(file: PsiJavaFile, module: DocumentationModule, packageContent: Map<String, Content>) {
        val classDescriptors = file.classes.map {
            it.getJavaClassDescriptor(resolutionFacade)
        }

        if (classDescriptors.any { it != null && it.isDocumented(passConfiguration) }) {
            val packageNode = findOrCreatePackageNode(module, file.packageName, packageContent, documentationBuilder.refGraph)

            for (descriptor in classDescriptors.filterNotNull()) {
                with(documentationBuilder) {
                    packageNode.appendChild(descriptor, RefKind.Member)
                }
            }
        }
    }
}

private val hiddenAnnotations = setOf(
        KotlinBuiltIns.FQ_NAMES.parameterName.asString()
)

private fun AnnotationDescriptor.isHiddenInDocumentation() =
        type.constructor.declarationDescriptor?.fqNameSafe?.asString() in hiddenAnnotations

private fun AnnotationDescriptor.isDocumented(): Boolean {
    if (source.getPsi() != null && mustBeDocumented()) return true
    val annotationClassName = type.constructor.declarationDescriptor?.fqNameSafe?.asString()
    return annotationClassName == KotlinBuiltIns.FQ_NAMES.extensionFunctionType.asString()
}

fun AnnotationDescriptor.mustBeDocumented(): Boolean {
    val annotationClass = type.constructor.declarationDescriptor as? Annotated ?: return false
    return annotationClass.isDocumentedAnnotation()
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

fun CallableMemberDescriptor.getExtensionClassDescriptor(): ClassifierDescriptor? {
    val extensionReceiver = extensionReceiverParameter
    if (extensionReceiver != null) {
        val type = extensionReceiver.type
        val receiverClass = type.constructor.declarationDescriptor as? ClassDescriptor
        if (receiverClass?.isCompanionObject ?: false) {
            return receiverClass?.containingDeclaration as? ClassifierDescriptor
        }
        return receiverClass
    }
    return null
}

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

fun DocumentationModule.prepareForGeneration(configuration: DokkaConfiguration) {
    if (configuration.generateIndexPages) {
        generateAllTypesNode()
    }
    nodeRefGraph.resolveReferences()
}

fun DocumentationNode.generateAllTypesNode() {
    val allTypes = members(NodeKind.Package)
            .flatMap { it.members.filter {
                it.kind in NodeKind.classLike || it.kind == NodeKind.ExternalClass
                        || (it.kind == NodeKind.GroupNode && it.origins.all { it.kind in NodeKind.classLike }) } }
            .sortedBy { if (it.kind == NodeKind.ExternalClass) it.name.substringAfterLast('.').toLowerCase() else it.name.toLowerCase() }

    val allTypesNode = DocumentationNode("alltypes", Content.Empty, NodeKind.AllTypes)
    for (typeNode in allTypes) {
        allTypesNode.addReferenceTo(typeNode, RefKind.Member)
    }

    append(allTypesNode, RefKind.Member)
}

fun ClassDescriptor.supertypesWithAnyPrecise(): Collection<KotlinType> {
    if (KotlinBuiltIns.isAny(this)) {
        return emptyList()
    }
    return typeConstructor.supertypesWithAny()
}

fun PassConfiguration.effectivePackageOptions(pack: String): DokkaConfiguration.PackageOptions {
    val rootPackageOptions = PackageOptionsImpl("", includeNonPublic, reportUndocumented, skipDeprecated, false)
    return perPackageOptions.firstOrNull { pack == it.prefix }
            ?: perPackageOptions.firstOrNull { pack.startsWith(it.prefix + ".") }
            ?: rootPackageOptions
}

fun PassConfiguration.effectivePackageOptions(pack: FqName): DokkaConfiguration.PackageOptions = effectivePackageOptions(pack.asString())

