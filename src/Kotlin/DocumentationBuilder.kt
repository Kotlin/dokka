package org.jetbrains.dokka

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.DocumentationNode.Kind
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.idea.kdoc.KDocFinder
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.lexer.JetSingleValueToken
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetModifierListOwner
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
import org.jetbrains.kotlin.resolve.descriptorUtil.isDocumentedAnnotation
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.expressions.OperatorConventions

public data class DocumentationOptions(val includeNonPublic: Boolean = false,
                                       val reportUndocumented: Boolean = true,
                                       val skipEmptyPackages: Boolean = true,
                                       val skipDeprecated: Boolean = false,
                                       val sourceLinks: List<SourceLinkDefinition>)

private fun isSamePackage(descriptor1: DeclarationDescriptor, descriptor2: DeclarationDescriptor): Boolean {
    val package1 = DescriptorUtils.getParentOfType(descriptor1, PackageFragmentDescriptor::class.java)
    val package2 = DescriptorUtils.getParentOfType(descriptor2, PackageFragmentDescriptor::class.java)
    return package1 != null && package2 != null && package1.fqName == package2.fqName
}

class DocumentationBuilder(val resolutionFacade: ResolutionFacade,
                           val session: ResolveSession,
                           val options: DocumentationOptions,
                           val refGraph: NodeReferenceGraph,
                           val logger: DokkaLogger) {
    val visibleToDocumentation = setOf(Visibilities.PROTECTED, Visibilities.PUBLIC)
    val boringBuiltinClasses = setOf(
            "kotlin.Unit", "kotlin.Byte", "kotlin.Short", "kotlin.Int", "kotlin.Long", "kotlin.Char", "kotlin.Boolean",
            "kotlin.Float", "kotlin.Double", "kotlin.String", "kotlin.Array", "kotlin.Any")
    val knownModifiers = setOf(
            JetTokens.PUBLIC_KEYWORD, JetTokens.PROTECTED_KEYWORD, JetTokens.INTERNAL_KEYWORD, JetTokens.PRIVATE_KEYWORD,
            JetTokens.OPEN_KEYWORD, JetTokens.FINAL_KEYWORD, JetTokens.ABSTRACT_KEYWORD, JetTokens.SEALED_KEYWORD,
            JetTokens.OVERRIDE_KEYWORD)

    fun parseDocumentation(descriptor: DeclarationDescriptor): Content {
        val kdoc = KDocFinder.findKDoc(descriptor) ?: findStdlibKDoc(descriptor)
        if (kdoc == null) {
            if (options.reportUndocumented && !descriptor.isDeprecated() &&
                    descriptor !is ValueParameterDescriptor && descriptor !is TypeParameterDescriptor &&
                    descriptor !is PropertyAccessorDescriptor) {
                logger.warn("No documentation for ${descriptor.signatureWithSourceLocation()}")
            }
            return Content.Empty
        }
        var kdocText = kdoc.getContent()
        // workaround for code fence parsing problem in IJ markdown parser
        if (kdocText.endsWith("```") || kdocText.endsWith("~~~")) {
            kdocText += "\n"
        }
        val tree = parseMarkdown(kdocText)
        //println(tree.toTestString())
        val content = buildContent(tree, { href -> resolveContentLink(descriptor, href) })
        if (kdoc is KDocSection) {
            val tags = kdoc.getTags()
            tags.forEach {
                when (it.name) {
                    "sample" ->
                        content.append(functionBody(descriptor, it.getSubjectName()))
                    "see" ->
                        content.addTagToSeeAlso(descriptor, it)
                    else -> {
                        val section = content.addSection(javadocSectionDisplayName(it.name), it.getSubjectName())
                        val sectionContent = it.getContent()
                        val markdownNode = parseMarkdown(sectionContent)
                        buildInlineContentTo(markdownNode, section, { href -> resolveContentLink(descriptor, href) })
                    }
                }
            }
        }
        return content
    }

    /**
     * Special case for generating stdlib documentation (the Any class to which the override chain will resolve
     * is not the same one as the Any class included in the source scope).
     */
    fun findStdlibKDoc(descriptor: DeclarationDescriptor): KDocTag? {
        if (descriptor !is CallableMemberDescriptor) {
            return null
        }
        val name = descriptor.name.asString()
        if (name == "equals" || name == "hashCode" || name == "toString") {
            var deepestDescriptor: CallableMemberDescriptor = descriptor
            while (!deepestDescriptor.overriddenDescriptors.isEmpty()) {
                deepestDescriptor = deepestDescriptor.overriddenDescriptors.first()
            }
            if (DescriptorUtils.getFqName(deepestDescriptor.containingDeclaration).asString() == "kotlin.Any") {
                val anyClassDescriptors = session.getTopLevelClassDescriptors(FqName.fromSegments(listOf("kotlin", "Any")),
                        NoLookupLocation.UNSORTED)
                anyClassDescriptors.forEach {
                    val anyMethod = it.getMemberScope(listOf()).getFunctions(descriptor.name, NoLookupLocation.UNSORTED).single()
                    val kdoc = KDocFinder.findKDoc(anyMethod)
                    if (kdoc != null) {
                        return kdoc
                    }
                }
            }
        }
        return null
    }

    fun DeclarationDescriptor.isDeprecated(): Boolean = annotations.any {
        DescriptorUtils.getFqName(it.type.constructor.declarationDescriptor!!).asString() == "kotlin.Deprecated"
    } || (this is ConstructorDescriptor && containingDeclaration.isDeprecated())

    fun DeclarationDescriptor.signature(): String = when(this) {
        is ClassDescriptor, is PackageFragmentDescriptor -> DescriptorUtils.getFqName(this).asString()
        is PropertyDescriptor -> containingDeclaration.signature() + "#" + name + receiverSignature()
        is FunctionDescriptor -> containingDeclaration.signature() + "#" + name + parameterSignature()
        is ValueParameterDescriptor -> containingDeclaration.signature() + ":" + name
        is TypeParameterDescriptor -> containingDeclaration.signature() + "<" + name

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
        val params = valueParameters.map { it.type }.toArrayList()
        val extensionReceiver = extensionReceiverParameter
        if (extensionReceiver != null) {
            params.add(0, extensionReceiver.type)
        }
        return "(" + params.map { it.signature() }.join() + ")"
    }

    fun JetType.signature(): String {
        val declarationDescriptor = constructor.declarationDescriptor ?: return "<null>"
        val typeName = DescriptorUtils.getFqName(declarationDescriptor).asString()
        if (typeName == "Array" && arguments.size() == 1) {
            return "Array<" + arguments.first().type.signature() + ">"
        }
        return typeName
    }

    fun DeclarationDescriptor.sourceLocation(): String? {
        if (this is DeclarationDescriptorWithSource) {
            val psi = (this.source as? PsiSourceElement)?.getPsi()
            if (psi != null) {
                val fileName = psi.containingFile.name
                val lineNumber = psi.lineNumber()
                return if (lineNumber != null) "$fileName:$lineNumber" else fileName
            }
        }
        return null
    }

    fun DeclarationDescriptor.signatureWithSourceLocation(): String {
        val signature = signature()
        val sourceLocation = sourceLocation()
        return if (sourceLocation != null) "$signature ($sourceLocation)" else signature
    }

    fun resolveContentLink(descriptor: DeclarationDescriptor, href: String): ContentBlock {
        val symbol = try {
            val symbols = resolveKDocLink(resolutionFacade, descriptor, null, href.split('.').toList())
            findTargetSymbol(symbols)
        } catch(e: Exception) {
            null
        }

        // don't include unresolved links in generated doc
        // assume that if an href doesn't contain '/', it's not an attempt to reference an external file
        if (symbol != null) {
            return ContentNodeLazyLink(href, { -> refGraph.lookup(symbol.signature()) })
        }
        if ("/" in href) {
            return ContentExternalLink(href)
        }
        logger.warn("Unresolved link to $href in doc comment of ${descriptor.signatureWithSourceLocation()}")
        return ContentExternalLink("#")
    }

    fun findTargetSymbol(symbols: Collection<DeclarationDescriptor>): DeclarationDescriptor? {
        if (symbols.isEmpty()) {
            return null
        }
        val symbol = symbols.first()
        if (symbol is CallableMemberDescriptor && symbol.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return symbol.overriddenDescriptors.firstOrNull()
        }
        return symbol
    }

    fun KDocSection.getTags(): Array<KDocTag> = PsiTreeUtil.getChildrenOfType(this, KDocTag::class.java) ?: arrayOf()

    private fun MutableContent.addTagToSeeAlso(descriptor: DeclarationDescriptor, seeTag: KDocTag) {
        val subjectName = seeTag.getSubjectName()
        if (subjectName != null) {
            val seeSection = findSectionByTag("See Also") ?: addSection("See Also", null)
            val link = resolveContentLink(descriptor, subjectName)
            link.append(ContentText(subjectName))
            val para = ContentParagraph()
            para.append(link)
            seeSection.append(para)
        }
    }

    fun link(node: DocumentationNode, descriptor: DeclarationDescriptor) {
        refGraph.link(node, descriptor.signature(), DocumentationReference.Kind.Link)
    }

    fun link(fromDescriptor: DeclarationDescriptor?, toDescriptor: DeclarationDescriptor?, kind: DocumentationReference.Kind) {
        if (fromDescriptor != null && toDescriptor != null) {
            refGraph.link(fromDescriptor.signature(), toDescriptor.signature(), kind)
        }
    }

    fun register(descriptor: DeclarationDescriptor, node: DocumentationNode) {
        refGraph.register(descriptor.signature(), node)
    }

    fun DocumentationNode<T>(descriptor: T, kind: Kind): DocumentationNode where T : DeclarationDescriptor, T : Named {
        val doc = parseDocumentation(descriptor)
        val node = DocumentationNode(descriptor.name.asString(), doc, kind).withModifiers(descriptor)
        return node
    }

    private fun DocumentationNode.withModifiers(descriptor: DeclarationDescriptor) : DocumentationNode{
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
        val modifier = modality.name().toLowerCase()
        appendTextNode(modifier, DocumentationNode.Kind.Modifier)
    }

    fun DocumentationNode.appendVisibility(descriptor: DeclarationDescriptorWithVisibility) {
        val modifier = descriptor.visibility.toString()
        appendTextNode(modifier, DocumentationNode.Kind.Modifier)
    }

    fun DocumentationNode.appendSupertypes(descriptor: ClassDescriptor) {
        val superTypes = descriptor.typeConstructor.supertypes
        for (superType in superTypes) {
            if (!ignoreSupertype(superType)) {
                appendType(superType, DocumentationNode.Kind.Supertype)
                link(superType?.constructor?.declarationDescriptor, descriptor, DocumentationReference.Kind.Inheritor)
            }
        }
    }

    private fun ignoreSupertype(superType: JetType): Boolean {
        val superClass = superType.constructor.declarationDescriptor as? ClassDescriptor
        if (superClass != null) {
            val fqName = DescriptorUtils.getFqNameSafe(superClass).asString()
            return fqName == "kotlin.Annotation" || fqName == "kotlin.Enum" || fqName == "kotlin.Any"
        }
        return false
    }

    fun DocumentationNode.appendProjection(projection: TypeProjection, kind: DocumentationNode.Kind = DocumentationNode.Kind.Type) {
        appendType(projection.type, kind, projection.projectionKind.label)
    }

    fun DocumentationNode.appendType(jetType: JetType?, kind: DocumentationNode.Kind = DocumentationNode.Kind.Type, prefix: String = "") {
        if (jetType == null)
            return
        val classifierDescriptor = jetType.constructor.declarationDescriptor
        val name = when (classifierDescriptor) {
            is ClassDescriptor -> {
                if (classifierDescriptor.isCompanionObject) {
                    classifierDescriptor.containingDeclaration.name.asString() +
                            "." + classifierDescriptor.name.asString()
                }
                else {
                    classifierDescriptor.name.asString()
                }
            }
            is Named -> classifierDescriptor.name.asString()
            else -> "<anonymous>"
        }
        val node = DocumentationNode(name, Content.Empty, kind)
        if (prefix != "") {
            node.appendTextNode(prefix, Kind.Modifier)
        }
        if (jetType.isMarkedNullable) {
            node.appendTextNode("?", Kind.NullabilityModifier)
        }
        if (classifierDescriptor != null && !classifierDescriptor.isBoringBuiltinClass()) {
            link(node, classifierDescriptor)
        }

        append(node, DocumentationReference.Kind.Detail)
        for (typeArgument in jetType.arguments)
            node.appendProjection(typeArgument)
    }

    fun ClassifierDescriptor.isBoringBuiltinClass(): Boolean =
        DescriptorUtils.getFqName(this).asString() in boringBuiltinClasses

    fun DocumentationNode.appendAnnotations(annotated: Annotated) {
        annotated.annotations.filter { it.source.getPsi() != null && it.mustBeDocumented() }.forEach {
            val annotationNode = it.build()
            if (annotationNode != null) {
                append(annotationNode,
                        if (annotationNode.isDeprecation()) DocumentationReference.Kind.Deprecation else DocumentationReference.Kind.Annotation)
            }
        }
    }

    fun AnnotationDescriptor.mustBeDocumented(): Boolean {
        val annotationClass = type.constructor.declarationDescriptor as? Annotated ?: return false
        return annotationClass.isDocumentedAnnotation()
    }

    fun DocumentationNode.appendModifiers(descriptor: DeclarationDescriptor) {
        val psi = (descriptor as DeclarationDescriptorWithSource).source.getPsi() as? JetModifierListOwner ?: return
        JetTokens.MODIFIER_KEYWORDS_ARRAY.filter { it !in knownModifiers }.forEach {
            if (psi.hasModifier(it)) {
                appendTextNode(it.value, Kind.Modifier)
            }
        }
    }

    fun DocumentationNode.isDeprecation() = name == "Deprecated" || name == "deprecated"

    fun DocumentationNode.appendSourceLink(sourceElement: SourceElement) {
        appendSourceLink(sourceElement.getPsi(), options.sourceLinks)
    }

    fun DocumentationNode.appendChild(descriptor: DeclarationDescriptor, kind: DocumentationReference.Kind): DocumentationNode? {
        // do not include generated code
        if (descriptor is CallableMemberDescriptor && descriptor.kind != CallableMemberDescriptor.Kind.DECLARATION)
            return null

        if (descriptor.isDocumented()) {
            val node = descriptor.build()
            append(node, kind)
            return node
        }
        return null
    }

    private fun DeclarationDescriptor.isDocumented(): Boolean {
        return (options.includeNonPublic
                || this !is MemberDescriptor
                || this.visibility in visibleToDocumentation) &&
                !isDocumentationSuppressed() &&
                (!options.skipDeprecated || !isDeprecated())
    }

    fun DeclarationDescriptor.isDocumentationSuppressed(): Boolean {
        val doc = KDocFinder.findKDoc(this)
        return doc is KDocSection && doc.findTagByName("suppress") != null
    }


    fun DocumentationNode.appendChildren(descriptors: Iterable<DeclarationDescriptor>, kind: DocumentationReference.Kind) {
        descriptors.forEach { descriptor -> appendChild(descriptor, kind) }
    }

    fun DocumentationNode.appendInPageChildren(descriptors: Iterable<DeclarationDescriptor>, kind: DocumentationReference.Kind) {
        descriptors.forEach { descriptor ->
            val node = appendChild(descriptor, kind)
            node?.addReferenceTo(this, DocumentationReference.Kind.TopLevelPage)
        }
    }

    fun DocumentationNode.getParentForPackageMember(descriptor: DeclarationDescriptor,
                                                    externalClassNodes: MutableMap<FqName, DocumentationNode>): DocumentationNode {
        if (descriptor is CallableMemberDescriptor) {
            val extensionClassDescriptor = descriptor.getExtensionClassDescriptor()
            if (extensionClassDescriptor != null && !isSamePackage(descriptor, extensionClassDescriptor) &&
                !ErrorUtils.isError(extensionClassDescriptor)) {
                val fqName = DescriptorUtils.getFqNameFromTopLevelClass(extensionClassDescriptor)
                return externalClassNodes.getOrPut(fqName, {
                    val newNode = DocumentationNode(fqName.asString(), Content.Empty, Kind.ExternalClass)
                    append(newNode, DocumentationReference.Kind.Member)
                    newNode
                })
            }
        }
        return this
    }

    fun DocumentationModule.appendFragments(fragments: Collection<PackageFragmentDescriptor>,
                                            packageContent: Map<String, Content>) {
        val descriptors = hashMapOf<String, List<DeclarationDescriptor>>()
        for ((name, parts) in fragments.groupBy { it.fqName }) {
            descriptors.put(name.asString(), parts.flatMap { it.getMemberScope().getAllDescriptors() })
        }
        for ((packageName, declarations) in descriptors) {
            if (options.skipEmptyPackages && declarations.none { it.isDocumented()}) continue
            logger.info("  package $packageName: ${declarations.count()} declarations")
            val packageNode = findOrCreatePackageNode(packageName, packageContent)
            val externalClassNodes = hashMapOf<FqName, DocumentationNode>()
            declarations.forEach { descriptor ->
                if (descriptor.isDocumented()) {
                    val parent = packageNode.getParentForPackageMember(descriptor, externalClassNodes)
                    parent.appendChild(descriptor, DocumentationReference.Kind.Member)
               }
            }
        }
    }

    fun DeclarationDescriptor.build(): DocumentationNode = when (this) {
        is ClassDescriptor -> build()
        is ConstructorDescriptor -> build()
        is ScriptDescriptor -> build()
        is PropertyDescriptor -> build()
        is FunctionDescriptor -> build()
        is TypeParameterDescriptor -> build()
        is ValueParameterDescriptor -> build()
        is ReceiverParameterDescriptor -> build()
        else -> throw IllegalStateException("Descriptor $this is not known")
    }

    fun ScriptDescriptor.build(): DocumentationNode = classDescriptor.build()

    fun ClassDescriptor.build(): DocumentationNode {
        val kind = when (kind) {
            ClassKind.OBJECT -> Kind.Object
            ClassKind.INTERFACE -> Kind.Interface
            ClassKind.ENUM_CLASS -> Kind.Enum
            ClassKind.ANNOTATION_CLASS -> Kind.AnnotationClass
            ClassKind.ENUM_ENTRY -> Kind.EnumItem
            else -> Kind.Class
        }
        val node = DocumentationNode(this, kind)
        node.appendSupertypes(this)
        if (getKind() != ClassKind.OBJECT && getKind() != ClassKind.ENUM_ENTRY) {
            node.appendInPageChildren(typeConstructor.parameters, DocumentationReference.Kind.Detail)
            val constructorsToDocument = if (getKind() == ClassKind.ENUM_CLASS)
                constructors.filter { it.valueParameters.size() > 0 }
            else
                constructors
            node.appendChildren(constructorsToDocument, DocumentationReference.Kind.Member)
        }
        val members = defaultType.memberScope.getAllDescriptors().filter { it != companionObjectDescriptor }
        node.appendChildren(members, DocumentationReference.Kind.Member)
        val companionObjectDescriptor = companionObjectDescriptor
        if (companionObjectDescriptor != null) {
            node.appendChildren(companionObjectDescriptor.defaultType.memberScope.getAllDescriptors(),
                    DocumentationReference.Kind.Member)
        }
        node.appendAnnotations(this)
        node.appendModifiers(this)
        node.appendSourceLink(source)
        register(this, node)
        return node
    }

    fun ConstructorDescriptor.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.Constructor)
        node.appendInPageChildren(valueParameters, DocumentationReference.Kind.Detail)
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

    fun CallableMemberDescriptor.getExtensionClassDescriptor(): ClassifierDescriptor? {
        val extensionReceiver = extensionReceiverParameter
        if (extensionReceiver != null) {
            val type = extensionReceiver.type
            return type.constructor.declarationDescriptor as? ClassDescriptor
        }
        return null
    }

    fun FunctionDescriptor.build(): DocumentationNode {
        if (ErrorUtils.containsErrorType(this)) {
            logger.warn("Found an unresolved type in ${signatureWithSourceLocation()}")
        }

        val node = DocumentationNode(this, if (inCompanionObject()) Kind.CompanionObjectFunction else Kind.Function)

        node.appendInPageChildren(typeParameters, DocumentationReference.Kind.Detail)
        extensionReceiverParameter?.let { node.appendChild(it, DocumentationReference.Kind.Detail) }
        node.appendInPageChildren(valueParameters, DocumentationReference.Kind.Detail)
        node.appendType(returnType)
        node.appendAnnotations(this)
        node.appendModifiers(this)
        node.appendSourceLink(source)
        node.appendOperatorOverloadNote(this)

        overriddenDescriptors.forEach {
            addOverrideLink(it, this)
        }

        register(this, node)
        return node
    }

    fun addOverrideLink(baseClassFunction: CallableMemberDescriptor, overridingFunction: CallableMemberDescriptor) {
        val source = baseClassFunction.original.source.getPsi()
        if (source != null) {
            link(overridingFunction, baseClassFunction, DocumentationReference.Kind.Override)
        } else {
            baseClassFunction.overriddenDescriptors.forEach {
                addOverrideLink(it, overridingFunction)
            }
        }
    }

    fun DocumentationNode.appendOperatorOverloadNote(descriptor: FunctionDescriptor) {
        val operatorName = descriptor.getImplementedOperator()
        if (operatorName != null) {
            val content = Content()
            content.append(ContentText("Implements "))
            content.strong {
                text("operator ")
                code {
                    text(operatorName)
                }
            }
            val noteNode = DocumentationNode("", content, DocumentationNode.Kind.OverloadGroupNote)
            append(noteNode, DocumentationReference.Kind.Detail)
        }
    }

    fun FunctionDescriptor.getImplementedOperator(): String? {
        var arity = valueParameters.size()
        if (containingDeclaration is ClassDescriptor) {
            arity++
        }
        if (extensionReceiverParameter != null) {
            arity++
        }

        val token = if (arity == 2) {
            OperatorConventions.BINARY_OPERATION_NAMES.inverse()[name] ?:
            OperatorConventions.ASSIGNMENT_OPERATIONS.inverse()[name] ?:
            OperatorConventions.BOOLEAN_OPERATIONS.inverse()[name]
        } else if (arity == 1) {
            OperatorConventions.UNARY_OPERATION_NAMES.inverse()[name]
        }
        else null

        if (token is JetSingleValueToken) {
            return token.value
        }

        val name = name.asString()
        if (arity == 2 && name == "contains") {
            return "in"
        }
        if (arity >= 2 && (name == "get" || name == "set")) {
            return "[]"
        }
        if (arity == 2 && name == "equals" && valueParameters.size() == 1 &&
            KotlinBuiltIns.isNullableAny(valueParameters.first().type)) {
            return "=="
        }
        return null
    }

    fun PropertyDescriptor.build(): DocumentationNode {
        val node = DocumentationNode(this, if (inCompanionObject()) Kind.CompanionObjectProperty else Kind.Property)
        node.appendInPageChildren(typeParameters, DocumentationReference.Kind.Detail)
        extensionReceiverParameter?.let { node.appendChild(it, DocumentationReference.Kind.Detail) }
        node.appendType(returnType)
        node.appendAnnotations(this)
        node.appendModifiers(this)
        node.appendSourceLink(source)
        if (isVar) {
            node.appendTextNode("var", DocumentationNode.Kind.Modifier)
        }
        getter?.let {
            if (!it.isDefault) {
                node.addAccessorDocumentation(parseDocumentation(it), "Getter")
            }
        }
        setter?.let {
            if (!it.isDefault) {
                node.addAccessorDocumentation(parseDocumentation(it), "Setter")
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
        val node = DocumentationNode(this, Kind.Parameter)
        node.appendType(varargElementType ?: type)
        if (declaresDefaultValue()) {
            val psi = source.getPsi() as? JetParameter
            if (psi != null) {
                val defaultValueText = psi.defaultValue?.text
                if (defaultValueText != null) {
                    node.appendTextNode(defaultValueText, Kind.Value)
                }
            }
        }
        node.appendAnnotations(this)
        node.appendModifiers(this)
        register(this, node)
        return node
    }

    fun TypeParameterDescriptor.build(): DocumentationNode {
        val doc = parseDocumentation(this)
        val name = name.asString()
        val prefix = variance.label

        val node = DocumentationNode(name, doc, DocumentationNode.Kind.TypeParameter)
        if (prefix != "") {
            node.appendTextNode(prefix, Kind.Modifier)
        }

        val builtIns = KotlinBuiltIns.getInstance()
        for (constraint in upperBounds) {
            if (constraint == builtIns.defaultBound)
                continue
            node.appendType(constraint, Kind.UpperBound)
        }

        for (constraint in lowerBounds) {
            if (KotlinBuiltIns.isNothing(constraint))
                continue
            node.appendType(constraint, Kind.LowerBound)
        }
        return node
    }

    fun ReceiverParameterDescriptor.build(): DocumentationNode {
        var receiverClass: DeclarationDescriptor = type.constructor.declarationDescriptor!!
        if ((receiverClass as? ClassDescriptor)?.isCompanionObject ?: false) {
            receiverClass = receiverClass.containingDeclaration!!
        }
        link(receiverClass,
                containingDeclaration,
                DocumentationReference.Kind.Extension)

        val node = DocumentationNode(name.asString(), Content.Empty, Kind.Receiver)
        node.appendType(type)
        return node
    }

    fun AnnotationDescriptor.build(): DocumentationNode? {
        val annotationClass = type.constructor.declarationDescriptor
        if (annotationClass == null || ErrorUtils.isError(annotationClass)) {
            return null
        }
        val node = DocumentationNode(annotationClass.name.asString(), Content.Empty, DocumentationNode.Kind.Annotation)
        val arguments = allValueArguments.toList().sortedBy { it.first.index }
        arguments.forEach {
            val valueNode = it.second.toDocumentationNode()
            if (valueNode != null) {
                val paramNode = DocumentationNode(it.first.name.asString(), Content.Empty, DocumentationNode.Kind.Parameter)
                paramNode.append(valueNode, DocumentationReference.Kind.Detail)
                node.append(paramNode, DocumentationReference.Kind.Detail)
            }
        }
        return node
    }

    fun CompileTimeConstant<Any?>.build(): DocumentationNode? = when (this) {
        is TypedCompileTimeConstant -> constantValue.toDocumentationNode()
        else -> null
    }

    fun ConstantValue<*>.toDocumentationNode(): DocumentationNode? = value?.let { value ->
        when (value) {
            is String ->
                "\"" + StringUtil.escapeStringCharacters(value) + "\""
            is EnumEntrySyntheticClassDescriptor ->
                value.containingDeclaration.name.asString() + "." + value.name.asString()
            else -> value.toString()
        }.let { valueString ->
            DocumentationNode(valueString, Content.Empty, DocumentationNode.Kind.Value)
        }
    }
}
