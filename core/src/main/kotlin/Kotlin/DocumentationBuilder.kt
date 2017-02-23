package org.jetbrains.dokka

import com.google.inject.Inject
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiJavaFile
import org.jetbrains.dokka.Kotlin.DescriptorDocumentationParser
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isDocumentedAnnotation
import org.jetbrains.kotlin.resolve.findTopMostOverriddenDescriptors
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.supertypes

data class DocumentationOptions(val outputDir: String,
                                val outputFormat: String,
                                val includeNonPublic: Boolean = false,
                                val reportUndocumented: Boolean = true,
                                val skipEmptyPackages: Boolean = true,
                                val skipDeprecated: Boolean = false,
                                val jdkVersion: Int = 6,
                                val generateIndexPages: Boolean = true,
                                val sourceLinks: List<SourceLinkDefinition>)

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

class DocumentationBuilder
@Inject constructor(val resolutionFacade: DokkaResolutionFacade,
                    val descriptorDocumentationParser: DescriptorDocumentationParser,
                    val options: DocumentationOptions,
                    val refGraph: NodeReferenceGraph,
                    val platformNodeRegistry: PlatformNodeRegistry,
                    val logger: DokkaLogger,
                    val linkResolver: DeclarationLinkResolver) {
    val boringBuiltinClasses = setOf(
            "kotlin.Unit", "kotlin.Byte", "kotlin.Short", "kotlin.Int", "kotlin.Long", "kotlin.Char", "kotlin.Boolean",
            "kotlin.Float", "kotlin.Double", "kotlin.String", "kotlin.Array", "kotlin.Any")
    val knownModifiers = setOf(
            KtTokens.PUBLIC_KEYWORD, KtTokens.PROTECTED_KEYWORD, KtTokens.INTERNAL_KEYWORD, KtTokens.PRIVATE_KEYWORD,
            KtTokens.OPEN_KEYWORD, KtTokens.FINAL_KEYWORD, KtTokens.ABSTRACT_KEYWORD, KtTokens.SEALED_KEYWORD,
            KtTokens.OVERRIDE_KEYWORD)

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

    fun <T> nodeForDescriptor(descriptor: T, kind: NodeKind): DocumentationNode where T : DeclarationDescriptor, T : Named {
        val (doc, callback) = descriptorDocumentationParser.parseDocumentationAndDetails(descriptor, kind == NodeKind.Parameter)
        val node = DocumentationNode(descriptor.name.asString(), doc, kind).withModifiers(descriptor)
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

    fun DocumentationNode.appendVisibility(descriptor: DeclarationDescriptorWithVisibility) {
        val modifier = descriptor.visibility.normalize().displayName
        appendTextNode(modifier, NodeKind.Modifier)
    }

    fun DocumentationNode.appendSupertype(descriptor: ClassDescriptor, superType: KotlinType) {
        val unwrappedType = superType.unwrap()
        if (unwrappedType is AbbreviatedType) {
            appendSupertype(descriptor, unwrappedType.abbreviation)
        } else if (!ignoreSupertype(unwrappedType)) {
            appendType(unwrappedType, NodeKind.Supertype)
            val superclass = unwrappedType.constructor.declarationDescriptor
            link(superclass, descriptor, RefKind.Inheritor)
            link(descriptor, superclass, RefKind.Superclass)
        }
    }

    private fun ignoreSupertype(superType: KotlinType): Boolean {
        val superClass = superType.constructor.declarationDescriptor as? ClassDescriptor
        if (superClass != null) {
            val fqName = DescriptorUtils.getFqNameSafe(superClass).asString()
            return fqName == "kotlin.Annotation" || fqName == "kotlin.Enum" || fqName == "kotlin.Any"
        }
        return false
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
        if (kotlinType.isMarkedNullable) {
            node.appendTextNode("?", NodeKind.NullabilityModifier)
        }
        if (classifierDescriptor != null) {
            val jdkLink = linkResolver.buildJdkLink(classifierDescriptor)
            if (jdkLink != null) {
                node.append(DocumentationNode(jdkLink, Content.Empty, NodeKind.ExternalLink), RefKind.Link)
            } else {
                link(node, classifierDescriptor,
                        if (classifierDescriptor.isBoringBuiltinClass()) RefKind.HiddenLink else RefKind.Link)
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

    fun DocumentationNode.appendSinceKotlin(annotation: DocumentationNode) {
        var kotlinVersion = annotation.detail(NodeKind.Parameter).detail(NodeKind.Value).name
        if (kotlinVersion.startsWith('\"') && kotlinVersion.endsWith('\"')) {
            kotlinVersion = kotlinVersion.substring(1..kotlinVersion.length-2)
        }
        append(platformNodeRegistry["Kotlin " + kotlinVersion], RefKind.Platform)
    }

    fun DocumentationNode.appendModifiers(descriptor: DeclarationDescriptor) {
        val psi = (descriptor as DeclarationDescriptorWithSource).source.getPsi() as? KtModifierListOwner ?: return
        KtTokens.MODIFIER_KEYWORDS_ARRAY.filter { it !in knownModifiers }.forEach {
            if (psi.hasModifier(it)) {
                appendTextNode(it.value, NodeKind.Modifier)
            }
        }
    }

    fun DocumentationNode.isDeprecation() = name == "Deprecated" || name == "deprecated"

    fun DocumentationNode.isSinceKotlin() = name == "SinceKotlin" && kind == NodeKind.Annotation

    fun DocumentationNode.appendSourceLink(sourceElement: SourceElement) {
        appendSourceLink(sourceElement.getPsi(), options.sourceLinks)
    }

    fun DocumentationNode.appendSignature(descriptor: DeclarationDescriptor) {
        appendTextNode(descriptor.signature(), NodeKind.Signature, RefKind.Detail)
    }

    fun DocumentationNode.appendChild(descriptor: DeclarationDescriptor, kind: RefKind): DocumentationNode? {
        // do not include generated code
        if (descriptor is CallableMemberDescriptor && descriptor.kind != CallableMemberDescriptor.Kind.DECLARATION)
            return null

        if (descriptor.isDocumented(options)) {
            val node = descriptor.build()
            append(node, kind)
            return node
        }
        return null
    }

    fun DocumentationNode.appendMembers(descriptors: Iterable<DeclarationDescriptor>,
                                        inheritedLinkKind: RefKind = RefKind.InheritedMember): List<DocumentationNode> {
        val nodes = descriptors.map { descriptor ->
            if (descriptor is CallableMemberDescriptor && descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                val baseDescriptor = descriptor.overriddenDescriptors.firstOrNull()
                if (baseDescriptor != null) {
                    link(this, baseDescriptor, inheritedLinkKind)
                }
                null
            } else {
                val descriptorToUse = if (descriptor is ConstructorDescriptor) descriptor else descriptor.original
                appendChild(descriptorToUse, RefKind.Member)
            }
        }
        return nodes.filterNotNull()
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
        val allFqNames = fragments.map { it.fqName }.distinct()

        for (packageName in allFqNames) {
            val declarations = fragments.filter { it.fqName == packageName }.flatMap { it.getMemberScope().getContributedDescriptors() }

            if (options.skipEmptyPackages && declarations.none { it.isDocumented(options) }) continue
            logger.info("  package $packageName: ${declarations.count()} declarations")
            val packageNode = findOrCreatePackageNode(packageName.asString(), packageContent, this@DocumentationBuilder.refGraph)
            packageDocumentationBuilder.buildPackageDocumentation(this@DocumentationBuilder, packageName, packageNode,
                    declarations, allFqNames)
        }

        propagateExtensionFunctionsToSubclasses(fragments)
        if (options.generateIndexPages) {
            generateAllTypesNode()
        }
    }

    private fun propagateExtensionFunctionsToSubclasses(fragments: Collection<PackageFragmentDescriptor>) {
        val allDescriptors = fragments.flatMap { it.getMemberScope().getContributedDescriptors() }
        val allClasses = allDescriptors.filterIsInstance<ClassDescriptor>()
        val classHierarchy = buildClassHierarchy(allClasses)

        val allExtensionFunctions = allDescriptors
                .filterIsInstance<CallableMemberDescriptor>()
                .filter { it.extensionReceiverParameter != null }
        val extensionFunctionsByName = allExtensionFunctions.groupBy { it.name }

        allExtensionFunctions.forEach { extensionFunction ->
            val possiblyShadowingFunctions = extensionFunctionsByName[extensionFunction.name]
                    ?.filter { fn -> fn.canShadow(extensionFunction) }
                    ?: emptyList()

            val classDescriptor = extensionFunction.getExtensionClassDescriptor() ?: return@forEach
            val subclasses = classHierarchy[classDescriptor] ?: return@forEach
            subclasses.forEach { subclass ->
                if (subclass.isExtensionApplicable(extensionFunction) &&
                        possiblyShadowingFunctions.none { subclass.isExtensionApplicable(it) }) {
                    refGraph.link(subclass.signature(), extensionFunction.signature(), RefKind.Extension)
                }
            }
        }
    }

    private fun ClassDescriptor.isExtensionApplicable(extensionFunction: CallableMemberDescriptor): Boolean {
        val receiverType = extensionFunction.extensionReceiverParameter!!.type
        if (receiverType.arguments.any { it.type.constructor.declarationDescriptor is TypeParameterDescriptor }) {
            val receiverClass = receiverType.constructor.declarationDescriptor
            return receiverClass is ClassDescriptor && DescriptorUtils.isSubclass(this, receiverClass)
        }
        return defaultType.isSubtypeOf(receiverType)
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

    private fun DocumentationNode.generateAllTypesNode() {
        val allTypes = members(NodeKind.Package)
                .flatMap { it.members.filter { it.kind in NodeKind.classLike || it.kind == NodeKind.ExternalClass } }
                .sortedBy { if (it.kind == NodeKind.ExternalClass) it.name.substringAfterLast('.') else it.name }

        val allTypesNode = DocumentationNode("alltypes", Content.Empty, NodeKind.AllTypes)
        for (typeNode in allTypes) {
            allTypesNode.addReferenceTo(typeNode, RefKind.Member)
        }

        append(allTypesNode, RefKind.Member)
    }

    fun DeclarationDescriptor.build(): DocumentationNode = when (this) {
        is ClassDescriptor -> build()
        is ConstructorDescriptor -> build()
        is PropertyDescriptor -> build()
        is FunctionDescriptor -> build()
        is TypeParameterDescriptor -> build()
        is ValueParameterDescriptor -> build()
        is ReceiverParameterDescriptor -> build()
        is TypeAliasDescriptor -> build()
        else -> throw IllegalStateException("Descriptor $this is not known")
    }

    fun TypeAliasDescriptor.build(): DocumentationNode {
        val node = nodeForDescriptor(this, NodeKind.TypeAlias)

        node.appendAnnotations(this)
        node.appendModifiers(this)
        node.appendInPageChildren(typeConstructor.parameters, RefKind.Detail)

        node.appendType(underlyingType, NodeKind.TypeAliasUnderlyingType)

        node.appendSourceLink(source)

        register(this, node)
        return node
    }

    fun ClassDescriptor.build(): DocumentationNode {
        val kind = when {
            kind == ClassKind.OBJECT -> NodeKind.Object
            kind == ClassKind.INTERFACE -> NodeKind.Interface
            kind == ClassKind.ENUM_CLASS -> NodeKind.Enum
            kind == ClassKind.ANNOTATION_CLASS -> NodeKind.AnnotationClass
            kind == ClassKind.ENUM_ENTRY -> NodeKind.EnumItem
            isSubclassOfThrowable() -> NodeKind.Exception
            else -> NodeKind.Class
        }
        val node = nodeForDescriptor(this, kind)
        typeConstructor.supertypes.forEach {
            node.appendSupertype(this, it)
        }
        if (getKind() != ClassKind.OBJECT && getKind() != ClassKind.ENUM_ENTRY) {
            node.appendInPageChildren(typeConstructor.parameters, RefKind.Detail)
            val constructorsToDocument = if (getKind() == ClassKind.ENUM_CLASS)
                constructors.filter { it.valueParameters.size > 0 }
            else
                constructors
            node.appendMembers(constructorsToDocument)
        }
        val members = defaultType.memberScope.getContributedDescriptors().filter { it != companionObjectDescriptor }
        node.appendMembers(members)
        node.appendMembers(staticScope.getContributedDescriptors()).forEach {
            it.appendTextNode("static", NodeKind.Modifier)
        }
        val companionObjectDescriptor = companionObjectDescriptor
        if (companionObjectDescriptor != null) {
            val descriptors = companionObjectDescriptor.defaultType.memberScope.getContributedDescriptors()
            val descriptorsToDocument = descriptors.filter { it !is CallableDescriptor || !it.isInheritedFromAny() }
            node.appendMembers(descriptorsToDocument, RefKind.InheritedCompanionObjectMember)
        }
        node.appendAnnotations(this)
        node.appendModifiers(this)
        node.appendSourceLink(source)
        register(this, node)
        return node
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

    fun FunctionDescriptor.build(): DocumentationNode {
        if (ErrorUtils.containsErrorType(this)) {
            logger.warn("Found an unresolved type in ${signatureWithSourceLocation()}")
        }

        val node = nodeForDescriptor(this, if (inCompanionObject()) NodeKind.CompanionObjectFunction else NodeKind.Function)

        node.appendInPageChildren(typeParameters, RefKind.Detail)
        extensionReceiverParameter?.let { node.appendChild(it, RefKind.Detail) }
        node.appendInPageChildren(valueParameters, RefKind.Detail)
        node.appendType(returnType)
        node.appendAnnotations(this)
        node.appendModifiers(this)
        node.appendSourceLink(source)
        node.appendSignature(this)

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

    fun PropertyDescriptor.build(): DocumentationNode {
        val node = nodeForDescriptor(this, if (inCompanionObject()) NodeKind.CompanionObjectProperty else NodeKind.Property)
        node.appendInPageChildren(typeParameters, RefKind.Detail)
        extensionReceiverParameter?.let { node.appendChild(it, RefKind.Detail) }
        node.appendType(returnType)
        node.appendAnnotations(this)
        node.appendModifiers(this)
        node.appendSourceLink(source)
        node.appendSignature(this)
        if (isVar) {
            node.appendTextNode("var", NodeKind.Modifier)
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
        node.appendAnnotations(this)
        node.appendModifiers(this)
        node.appendSignature(this)
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
        link(receiverClass,
                containingDeclaration,
                RefKind.Extension)

        val node = DocumentationNode(name.asString(), Content.Empty, NodeKind.Receiver)
        node.appendType(type)
        return node
    }

    fun AnnotationDescriptor.build(): DocumentationNode? {
        val annotationClass = type.constructor.declarationDescriptor
        if (annotationClass == null || ErrorUtils.isError(annotationClass)) {
            return null
        }
        val node = DocumentationNode(annotationClass.name.asString(), Content.Empty, NodeKind.Annotation)
        val arguments = allValueArguments.toList().sortedBy { it.first.index }
        arguments.forEach {
            val valueNode = it.second.toDocumentationNode()
            if (valueNode != null) {
                val paramNode = DocumentationNode(it.first.name.asString(), Content.Empty, NodeKind.Parameter)
                paramNode.append(valueNode, RefKind.Detail)
                node.append(paramNode, RefKind.Detail)
            }
        }
        return node
    }

    fun ConstantValue<*>.toDocumentationNode(): DocumentationNode? = value?.let { value ->
        when (value) {
            is String ->
                "\"" + StringUtil.escapeStringCharacters(value) + "\""
            is EnumEntrySyntheticClassDescriptor ->
                value.containingDeclaration.name.asString() + "." + value.name.asString()
            else -> value.toString()
        }.let { valueString ->
            DocumentationNode(valueString, Content.Empty, NodeKind.Value)
        }
    }
}

val visibleToDocumentation = setOf(Visibilities.PROTECTED, Visibilities.PUBLIC)

fun DeclarationDescriptor.isDocumented(options: DocumentationOptions): Boolean {
    return (options.includeNonPublic
            || this !is MemberDescriptor
            || this.visibility in visibleToDocumentation) &&
            !isDocumentationSuppressed() &&
            (!options.skipDeprecated || !isDeprecated())
}

class KotlinPackageDocumentationBuilder : PackageDocumentationBuilder {
    override fun buildPackageDocumentation(documentationBuilder: DocumentationBuilder,
                                           packageName: FqName,
                                           packageNode: DocumentationNode,
                                           declarations: List<DeclarationDescriptor>,
                                           allFqNames: Collection<FqName>) {
        val externalClassNodes = hashMapOf<FqName, DocumentationNode>()
        declarations.forEach { descriptor ->
            with(documentationBuilder) {
                if (descriptor.isDocumented(options)) {
                    val parent = packageNode.getParentForPackageMember(descriptor, externalClassNodes, allFqNames)
                    parent.appendChild(descriptor, RefKind.Member)
                }
            }
        }
    }
}

class KotlinJavaDocumentationBuilder
@Inject constructor(val resolutionFacade: DokkaResolutionFacade,
                    val documentationBuilder: DocumentationBuilder,
                    val options: DocumentationOptions,
                    val logger: DokkaLogger) : JavaDocumentationBuilder {
    override fun appendFile(file: PsiJavaFile, module: DocumentationModule, packageContent: Map<String, Content>) {
        val classDescriptors = file.classes.map {
            val javaDescriptorResolver = resolutionFacade.getFrontendService(JavaDescriptorResolver::class.java)

            javaDescriptorResolver.resolveClass(JavaClassImpl(it)) ?: run {
                logger.warn("Cannot find descriptor for Java class ${it.qualifiedName}")
                null
            }
        }

        if (classDescriptors.any { it != null && it.isDocumented(options) }) {
            val packageNode = module.findOrCreatePackageNode(file.packageName, packageContent, documentationBuilder.refGraph)

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

fun DeclarationDescriptor.isDocumentationSuppressed(): Boolean {
    val doc = findKDoc()
    if (doc is KDocSection && doc.findTagByName("suppress") != null) return true

    return hasSuppressDocTag(sourcePsi())
}

fun DeclarationDescriptor.sourcePsi() =
        ((original as DeclarationDescriptorWithSource).source as? PsiSourceElement)?.psi

fun DeclarationDescriptor.isDeprecated(): Boolean = annotations.any {
    DescriptorUtils.getFqName(it.type.constructor.declarationDescriptor!!).asString() == "kotlin.Deprecated"
} || (this is ConstructorDescriptor && containingDeclaration.isDeprecated())

fun DocumentationNode.getParentForPackageMember(descriptor: DeclarationDescriptor,
                                                externalClassNodes: MutableMap<FqName, DocumentationNode>,
                                                allFqNames: Collection<FqName>): DocumentationNode {
    if (descriptor is CallableMemberDescriptor) {
        val extensionClassDescriptor = descriptor.getExtensionClassDescriptor()
        if (extensionClassDescriptor != null && isExtensionForExternalClass(descriptor, extensionClassDescriptor, allFqNames) &&
                !ErrorUtils.isError(extensionClassDescriptor)) {
            val fqName = DescriptorUtils.getFqNameSafe(extensionClassDescriptor)
            return externalClassNodes.getOrPut(fqName, {
                val newNode = DocumentationNode(fqName.asString(), Content.Empty, NodeKind.ExternalClass)
                append(newNode, RefKind.Member)
                newNode
            })
        }
    }
    return this
}

fun CallableMemberDescriptor.getExtensionClassDescriptor(): ClassifierDescriptor? {
    val extensionReceiver = extensionReceiverParameter
    if (extensionReceiver != null) {
        val type = extensionReceiver.type
        val receiverClass = type.constructor.declarationDescriptor as? ClassDescriptor
        if ((receiverClass as? ClassDescriptor)?.isCompanionObject ?: false) {
            return receiverClass?.containingDeclaration as? ClassifierDescriptor
        }
        return receiverClass
    }
    return null
}

fun DeclarationDescriptor.signature(): String = when (this) {
    is ClassDescriptor,
    is PackageFragmentDescriptor,
    is PackageViewDescriptor,
    is TypeAliasDescriptor -> DescriptorUtils.getFqName(this).asString()

    is PropertyDescriptor -> containingDeclaration.signature() + "$" + name + receiverSignature()
    is FunctionDescriptor -> containingDeclaration.signature() + "$" + name + parameterSignature()
    is ValueParameterDescriptor -> containingDeclaration.signature() + "/" + name
    is TypeParameterDescriptor -> containingDeclaration.signature() + "*" + name
    else -> throw UnsupportedOperationException("Don't know how to calculate signature for $this")
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
    return "(" + params.map { it.signature() }.joinToString() + ")"
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
