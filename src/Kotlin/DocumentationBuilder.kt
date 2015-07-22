package org.jetbrains.dokka

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.dokka.DocumentationNode.Kind
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.EnumEntrySyntheticClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.kdoc.KDocFinder
import org.jetbrains.kotlin.idea.kdoc.resolveKDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.lexer.JetSingleValueToken
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
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
    val package1 = DescriptorUtils.getParentOfType(descriptor1, javaClass<PackageFragmentDescriptor>())
    val package2 = DescriptorUtils.getParentOfType(descriptor2, javaClass<PackageFragmentDescriptor>())
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
                when (it.getName()) {
                    "sample" ->
                        content.append(functionBody(descriptor, it.getSubjectName()))
                    "see" ->
                        content.addTagToSeeAlso(descriptor, it)
                    else -> {
                        val section = content.addSection(javadocSectionDisplayName(it.getName()), it.getSubjectName())
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
        val name = descriptor.getName().asString()
        if (name == "equals" || name == "hashCode" || name == "toString") {
            var deepestDescriptor: CallableMemberDescriptor = descriptor
            while (!deepestDescriptor.getOverriddenDescriptors().isEmpty()) {
                deepestDescriptor = deepestDescriptor.getOverriddenDescriptors().first()
            }
            if (DescriptorUtils.getFqName(deepestDescriptor.getContainingDeclaration()).asString() == "kotlin.Any") {
                val anyClassDescriptors = session.getTopLevelClassDescriptors(FqName.fromSegments(listOf("kotlin", "Any")))
                anyClassDescriptors.forEach {
                    val anyMethod = it.getMemberScope(listOf()).getFunctions(descriptor.getName()).single()
                    val kdoc = KDocFinder.findKDoc(anyMethod)
                    if (kdoc != null) {
                        return kdoc
                    }
                }
            }
        }
        return null
    }

    fun DeclarationDescriptor.isDeprecated(): Boolean = getAnnotations().any {
        DescriptorUtils.getFqName(it.getType().getConstructor().getDeclarationDescriptor()!!).asString() == "kotlin.deprecated"
    } || (this is ConstructorDescriptor && getContainingDeclaration().isDeprecated())

    fun DeclarationDescriptor.signature(): String = when(this) {
        is ClassDescriptor, is PackageFragmentDescriptor -> DescriptorUtils.getFqName(this).asString()
        is PropertyDescriptor -> getContainingDeclaration().signature() + "#" + getName() + receiverSignature()
        is FunctionDescriptor -> getContainingDeclaration().signature() + "#" + getName() + parameterSignature()
        is ValueParameterDescriptor -> getContainingDeclaration().signature() + ":" + getName()
        is TypeParameterDescriptor -> getContainingDeclaration().signature() + "<" + getName()

        else -> throw UnsupportedOperationException("Don't know how to calculate signature for $this")
    }

    fun PropertyDescriptor.receiverSignature(): String {
        val receiver = getExtensionReceiverParameter()
        if (receiver != null) {
            return "#" + receiver.getType().signature()
        }
        return ""
    }

    fun CallableMemberDescriptor.parameterSignature(): String {
        val params = getValueParameters().map { it.getType() }.toArrayList()
        val extensionReceiver = getExtensionReceiverParameter()
        if (extensionReceiver != null) {
            params.add(0, extensionReceiver.getType())
        }
        return "(" + params.map { it.signature() }.join() + ")"
    }

    fun JetType.signature(): String {
        val declarationDescriptor = getConstructor().getDeclarationDescriptor() ?: return "<null>"
        val typeName = DescriptorUtils.getFqName(declarationDescriptor).asString()
        if (typeName == "Array" && getArguments().size() == 1) {
            return "Array<" + getArguments().first().getType().signature() + ">"
        }
        return typeName
    }

    fun DeclarationDescriptor.sourceLocation(): String? {
        if (this is DeclarationDescriptorWithSource) {
            val psi = (this.getSource() as? PsiSourceElement)?.getPsi()
            if (psi != null) {
                val fileName = psi.getContainingFile().getName()
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
        if (symbol is CallableMemberDescriptor && symbol.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            return symbol.getOverriddenDescriptors().firstOrNull()
        }
        return symbol
    }

    fun KDocSection.getTags(): Array<KDocTag> = PsiTreeUtil.getChildrenOfType(this, javaClass<KDocTag>()) ?: arrayOf()

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
        val node = DocumentationNode(descriptor.getName().asString(), doc, kind).withModifiers(descriptor)
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
        var modality = descriptor.getModality()
        if (modality == Modality.OPEN) {
            val containingClass = descriptor.getContainingDeclaration() as? ClassDescriptor
            if (containingClass?.getModality() == Modality.FINAL) {
                modality = Modality.FINAL
            }
        }
        val modifier = modality.name().toLowerCase()
        appendTextNode(modifier, DocumentationNode.Kind.Modifier)
    }

    fun DocumentationNode.appendVisibility(descriptor: DeclarationDescriptorWithVisibility) {
        val modifier = descriptor.getVisibility().toString()
        appendTextNode(modifier, DocumentationNode.Kind.Modifier)
    }

    fun DocumentationNode.appendSupertypes(descriptor: ClassDescriptor) {
        val superTypes = descriptor.getTypeConstructor().getSupertypes()
        for (superType in superTypes) {
            if (!ignoreSupertype(superType)) {
                appendType(superType, DocumentationNode.Kind.Supertype)
                link(superType?.getConstructor()?.getDeclarationDescriptor(), descriptor, DocumentationReference.Kind.Inheritor)
            }
        }
    }

    private fun ignoreSupertype(superType: JetType): Boolean {
        val superClass = superType.getConstructor().getDeclarationDescriptor() as? ClassDescriptor
        if (superClass != null) {
            val fqName = DescriptorUtils.getFqNameSafe(superClass).asString()
            return fqName == "kotlin.Annotation" || fqName == "kotlin.Enum" || fqName == "kotlin.Any"
        }
        return false
    }

    fun DocumentationNode.appendProjection(projection: TypeProjection, kind: DocumentationNode.Kind = DocumentationNode.Kind.Type) {
        appendType(projection.getType(), kind, projection.getProjectionKind().label)
    }

    fun DocumentationNode.appendType(jetType: JetType?, kind: DocumentationNode.Kind = DocumentationNode.Kind.Type, prefix: String = "") {
        if (jetType == null)
            return
        val classifierDescriptor = jetType.getConstructor().getDeclarationDescriptor()
        val name = when (classifierDescriptor) {
            is ClassDescriptor -> {
                if (classifierDescriptor.isCompanionObject()) {
                    classifierDescriptor.getContainingDeclaration().getName().asString() +
                            "." + classifierDescriptor.getName().asString()
                }
                else {
                    classifierDescriptor.getName().asString()
                }
            }
            is Named -> classifierDescriptor.getName().asString()
            else -> "<anonymous>"
        }
        val node = DocumentationNode(name, Content.Empty, kind)
        if (prefix != "") {
            node.appendTextNode(prefix, Kind.Modifier)
        }
        if (jetType.isMarkedNullable()) {
            node.appendTextNode("?", Kind.NullabilityModifier)
        }
        if (classifierDescriptor != null && !classifierDescriptor.isBoringBuiltinClass()) {
            link(node, classifierDescriptor)
        }

        append(node, DocumentationReference.Kind.Detail)
        for (typeArgument in jetType.getArguments())
            node.appendProjection(typeArgument)
    }

    fun ClassifierDescriptor.isBoringBuiltinClass(): Boolean =
        DescriptorUtils.getFqName(this).asString() in boringBuiltinClasses

    fun DocumentationNode.appendAnnotations(annotated: Annotated) {
        annotated.getAnnotations().forEach {
            val annotationNode = it.build()
            if (annotationNode != null) {
                append(annotationNode,
                        if (annotationNode.name == "deprecated") DocumentationReference.Kind.Deprecation else DocumentationReference.Kind.Annotation)
            }
        }
    }

    fun DocumentationNode.appendSourceLink(sourceElement: SourceElement) {
        appendSourceLink(sourceElement.getPsi(), options.sourceLinks)
    }

    fun DocumentationNode.appendChild(descriptor: DeclarationDescriptor, kind: DocumentationReference.Kind): DocumentationNode? {
        // do not include generated code
        if (descriptor is CallableMemberDescriptor && descriptor.getKind() != CallableMemberDescriptor.Kind.DECLARATION)
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
                || this.getVisibility() in visibleToDocumentation) &&
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

    fun ScriptDescriptor.build(): DocumentationNode = getClassDescriptor().build()

    fun ClassDescriptor.build(): DocumentationNode {
        val kind = when (getKind()) {
            ClassKind.OBJECT -> Kind.Object
            ClassKind.INTERFACE -> Kind.Interface
            ClassKind.ENUM_CLASS -> Kind.Enum
            ClassKind.ANNOTATION_CLASS -> Kind.AnnotationClass
            ClassKind.ENUM_ENTRY -> Kind.EnumItem
            else -> Kind.Class
        }
        val node = DocumentationNode(this, kind)
        if (isInner()) {
            node.appendTextNode("inner", Kind.Modifier)
        }
        node.appendSupertypes(this)
        if (getKind() != ClassKind.OBJECT && getKind() != ClassKind.ENUM_ENTRY) {
            node.appendInPageChildren(getTypeConstructor().getParameters(), DocumentationReference.Kind.Detail)
            val constructorsToDocument = if (getKind() == ClassKind.ENUM_CLASS)
                getConstructors().filter { it.getValueParameters().size() > 0 }
            else
                getConstructors()
            node.appendChildren(constructorsToDocument, DocumentationReference.Kind.Member)
        }
        val members = getDefaultType().getMemberScope().getAllDescriptors().filter { it != getCompanionObjectDescriptor() }
        node.appendChildren(members, DocumentationReference.Kind.Member)
        val companionObjectDescriptor = getCompanionObjectDescriptor()
        if (companionObjectDescriptor != null) {
            node.appendChildren(companionObjectDescriptor.getDefaultType().getMemberScope().getAllDescriptors(),
                    DocumentationReference.Kind.Member)
        }
        node.appendAnnotations(this)
        node.appendSourceLink(getSource())
        register(this, node)
        return node
    }

    fun ConstructorDescriptor.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.Constructor)
        node.appendInPageChildren(getValueParameters(), DocumentationReference.Kind.Detail)
        register(this, node)
        return node
    }

    private fun CallableMemberDescriptor.inCompanionObject(): Boolean {
        val containingDeclaration = getContainingDeclaration()
        if ((containingDeclaration as? ClassDescriptor)?.isCompanionObject() ?: false) {
            return true
        }
        val receiver = getExtensionReceiverParameter()
        return (receiver?.getType()?.getConstructor()?.getDeclarationDescriptor() as? ClassDescriptor)?.isCompanionObject() ?: false
    }

    fun CallableMemberDescriptor.getExtensionClassDescriptor(): ClassifierDescriptor? {
        val extensionReceiver = getExtensionReceiverParameter()
        if (extensionReceiver != null) {
            val type = extensionReceiver.getType()
            return type.getConstructor().getDeclarationDescriptor() as? ClassDescriptor
        }
        return null
    }

    fun FunctionDescriptor.build(): DocumentationNode {
        if (ErrorUtils.containsErrorType(this)) {
            logger.warn("Found an unresolved type in ${signatureWithSourceLocation()}")
        }

        val node = DocumentationNode(this, if (inCompanionObject()) Kind.CompanionObjectFunction else Kind.Function)

        node.appendInPageChildren(getTypeParameters(), DocumentationReference.Kind.Detail)
        getExtensionReceiverParameter()?.let { node.appendChild(it, DocumentationReference.Kind.Detail) }
        node.appendInPageChildren(getValueParameters(), DocumentationReference.Kind.Detail)
        node.appendType(getReturnType())
        node.appendAnnotations(this)
        node.appendSourceLink(getSource())
        node.appendOperatorOverloadNote(this)

        getOverriddenDescriptors().forEach {
            addOverrideLink(it, this)
        }

        register(this, node)
        return node
    }

    fun addOverrideLink(baseClassFunction: CallableMemberDescriptor, overridingFunction: CallableMemberDescriptor) {
        val source = baseClassFunction.getOriginal().getSource().getPsi()
        if (source != null) {
            link(overridingFunction, baseClassFunction, DocumentationReference.Kind.Override)
        } else {
            baseClassFunction.getOverriddenDescriptors().forEach {
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
        var arity = getValueParameters().size()
        if (getContainingDeclaration() is ClassDescriptor) {
            arity++
        }
        if (getExtensionReceiverParameter() != null) {
            arity++
        }

        val token = if (arity == 2) {
            OperatorConventions.BINARY_OPERATION_NAMES.inverse()[getName()] ?:
            OperatorConventions.ASSIGNMENT_OPERATIONS.inverse()[getName()] ?:
            OperatorConventions.BOOLEAN_OPERATIONS.inverse()[getName()]
        } else if (arity == 1) {
            OperatorConventions.UNARY_OPERATION_NAMES.inverse()[getName()]
        }
        else null

        if (token is JetSingleValueToken) {
            return token.getValue()
        }

        val name = getName().asString()
        if (arity == 2 && name == "contains") {
            return "in"
        }
        if (arity >= 2 && (name == "get" || name == "set")) {
            return "[]"
        }
        if (arity == 2 && name == "equals" && getValueParameters().size() == 1 &&
            KotlinBuiltIns.isNullableAny(getValueParameters().first().getType())) {
            return "=="
        }
        return null
    }

    fun PropertyDescriptor.build(): DocumentationNode {
        val node = DocumentationNode(this, if (inCompanionObject()) Kind.CompanionObjectProperty else Kind.Property)
        node.appendInPageChildren(getTypeParameters(), DocumentationReference.Kind.Detail)
        getExtensionReceiverParameter()?.let { node.appendChild(it, DocumentationReference.Kind.Detail) }
        node.appendType(getReturnType())
        node.appendAnnotations(this)
        node.appendSourceLink(getSource())
        if (isVar()) {
            node.appendTextNode("var", DocumentationNode.Kind.Modifier)
        }
        getGetter()?.let {
            if (!it.isDefault()) {
                node.addAccessorDocumentation(parseDocumentation(it), "Getter")
            }
        }
        getSetter()?.let {
            if (!it.isDefault()) {
                node.addAccessorDocumentation(parseDocumentation(it), "Setter")
            }
        }

        getOverriddenDescriptors().forEach {
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
        val varargType = getVarargElementType()
        if (varargType != null) {
            node.appendTextNode("vararg", Kind.Annotation, DocumentationReference.Kind.Annotation)
            node.appendType(varargType)
        } else {
            node.appendType(getType())
        }
        if (hasDefaultValue()) {
            val psi = getSource().getPsi() as? JetParameter
            if (psi != null) {
                val defaultValueText = psi.getDefaultValue()?.getText()
                if (defaultValueText != null) {
                    node.appendTextNode(defaultValueText, Kind.Value)
                }
            }
        }
        node.appendAnnotations(this)
        register(this, node)
        return node
    }

    fun TypeParameterDescriptor.build(): DocumentationNode {
        val doc = parseDocumentation(this)
        val name = getName().asString()
        val prefix = getVariance().label

        val node = DocumentationNode(name, doc, DocumentationNode.Kind.TypeParameter)
        if (prefix != "") {
            node.appendTextNode(prefix, Kind.Modifier)
        }

        val builtIns = KotlinBuiltIns.getInstance()
        for (constraint in getUpperBounds()) {
            if (constraint == builtIns.getDefaultBound())
                continue
            node.appendType(constraint, Kind.UpperBound)
        }

        for (constraint in getLowerBounds()) {
            if (KotlinBuiltIns.isNothing(constraint))
                continue
            node.appendType(constraint, Kind.LowerBound)
        }
        return node
    }

    fun ReceiverParameterDescriptor.build(): DocumentationNode {
        var receiverClass: DeclarationDescriptor = getType().getConstructor().getDeclarationDescriptor()!!
        if ((receiverClass as? ClassDescriptor)?.isCompanionObject() ?: false) {
            receiverClass = receiverClass.getContainingDeclaration()!!
        }
        link(receiverClass,
                getContainingDeclaration(),
                DocumentationReference.Kind.Extension)

        val node = DocumentationNode(getName().asString(), Content.Empty, Kind.Receiver)
        node.appendType(getType())
        return node
    }

    fun AnnotationDescriptor.build(): DocumentationNode? {
        val annotationClass = getType().getConstructor().getDeclarationDescriptor()
        if (annotationClass == null || ErrorUtils.isError(annotationClass)) {
            return null
        }
        val node = DocumentationNode(annotationClass.getName().asString(), Content.Empty, DocumentationNode.Kind.Annotation)
        val arguments = getAllValueArguments().toList().sortBy { it.first.getIndex() }
        arguments.forEach {
            val valueNode = it.second.value.toDocumentationNode()
            if (valueNode != null) {
                val paramNode = DocumentationNode(it.first.getName().asString(), Content.Empty, DocumentationNode.Kind.Parameter)
                paramNode.append(valueNode, DocumentationReference.Kind.Detail)
                node.append(paramNode, DocumentationReference.Kind.Detail)
            }
        }
        return node
    }

    fun CompileTimeConstant<Any?>.build(): DocumentationNode? {
        val value: Any? = if (this is TypedCompileTimeConstant) getValue(type) else null
        return value.toDocumentationNode()
    }

    private fun Any?.toDocumentationNode(): DocumentationNode? = when (this) {
        is String ->
            "\"" + StringUtil.escapeStringCharacters(this) + "\""
        is EnumEntrySyntheticClassDescriptor ->
            getContainingDeclaration().getName().asString() + "." + getName()
        else -> this?.toString()
    }.let { valueString ->
        if (valueString != null) DocumentationNode(valueString, Content.Empty, DocumentationNode.Kind.Value) else null
    }
}
