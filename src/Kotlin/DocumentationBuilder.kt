package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.dokka.DocumentationNode.Kind
import org.jetbrains.jet.lang.types.TypeProjection
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.name.FqName

public data class DocumentationOptions(val includeNonPublic: Boolean = false)

class DocumentationBuilder(val context: BindingContext, val options: DocumentationOptions) {
    val visibleToDocumentation = setOf(Visibilities.INTERNAL, Visibilities.PROTECTED, Visibilities.PUBLIC)
    val descriptorToNode = hashMapOf<DeclarationDescriptor, DocumentationNode>()
    val nodeToDescriptor = hashMapOf<DocumentationNode, DeclarationDescriptor>()
    val links = hashMapOf<DocumentationNode, DeclarationDescriptor>()
    val packages = hashMapOf<FqName, DocumentationNode>()

    fun parseDocumentation(descriptor: DeclarationDescriptor): Content {
        val docText = context.getDocumentationElements(descriptor).map { it.extractText() }.join("\n")
        val tree = MarkdownProcessor.parse(docText)
        //println(tree.toTestString())
        val content = tree.toContent()
        return content
    }

    fun link(node: DocumentationNode, descriptor: DeclarationDescriptor) {
        links.put(node, descriptor)
    }

    fun register(descriptor: DeclarationDescriptor, node: DocumentationNode) {
        descriptorToNode.put(descriptor, node)
        nodeToDescriptor.put(node, descriptor)
    }

    fun DocumentationNode<T>(descriptor: T, kind: Kind): DocumentationNode where T : DeclarationDescriptor, T : Named {
        val doc = parseDocumentation(descriptor)
        val node = DocumentationNode(descriptor.getName().asString(), doc, kind)
        if (descriptor is MemberDescriptor) {
            if (descriptor !is ConstructorDescriptor) {
                node.appendModality(descriptor)
            }
            node.appendVisibility(descriptor)
        }
        return node
    }

    fun DocumentationNode.append(child: DocumentationNode, kind: DocumentationReference.Kind) {
        addReferenceTo(child, kind)
        when (kind) {
            DocumentationReference.Kind.Detail -> child.addReferenceTo(this, DocumentationReference.Kind.Owner)
            DocumentationReference.Kind.Member -> child.addReferenceTo(this, DocumentationReference.Kind.Owner)
            DocumentationReference.Kind.Owner -> child.addReferenceTo(this, DocumentationReference.Kind.Member)
        }
    }

    fun DocumentationNode.appendModality(descriptor: MemberDescriptor) {
        val modifier = descriptor.getModality().name().toLowerCase()
        val node = DocumentationNode(modifier, Content.Empty, DocumentationNode.Kind.Modifier)
        append(node, DocumentationReference.Kind.Detail)
    }

    fun DocumentationNode.appendVisibility(descriptor: DeclarationDescriptorWithVisibility) {
        val modifier = descriptor.getVisibility().toString()
        val node = DocumentationNode(modifier, Content.Empty, DocumentationNode.Kind.Modifier)
        append(node, DocumentationReference.Kind.Detail)
    }

    fun DocumentationNode.appendSupertypes(descriptor: ClassDescriptor) {
        val superTypes = descriptor.getTypeConstructor().getSupertypes()
        for (superType in superTypes) {
            if (superType.toString() != "Any")
                appendType(superType, DocumentationNode.Kind.Supertype)
        }
    }

    fun DocumentationNode.appendProjection(projection: TypeProjection, kind: DocumentationNode.Kind = DocumentationNode.Kind.Type) {
        val prefix = when (projection.getProjectionKind()) {
            Variance.IN_VARIANCE -> "in "
            Variance.OUT_VARIANCE -> "out "
            else -> ""
        }
        appendType(projection.getType(), kind, prefix)
    }

    fun DocumentationNode.appendType(jetType: JetType?, kind: DocumentationNode.Kind = DocumentationNode.Kind.Type, prefix: String = "") {
        if (jetType == null)
            return
        val classifierDescriptor = jetType.getConstructor().getDeclarationDescriptor()
        val name = when (classifierDescriptor) {
            is Named -> prefix + classifierDescriptor.getName().asString() + if (jetType.isNullable()) "?" else ""
            else -> "<anonymous>"
        }
        val node = DocumentationNode(name, Content.Empty, kind)
        if (classifierDescriptor != null)
            link(node, classifierDescriptor)

        append(node, DocumentationReference.Kind.Detail)
        for (typeArgument in jetType.getArguments())
            node.appendProjection(typeArgument)
    }

    fun DocumentationNode.appendChild(descriptor: DeclarationDescriptor, kind: DocumentationReference.Kind) {
        // do not include generated code
        if (descriptor is CallableMemberDescriptor && descriptor.getKind() != CallableMemberDescriptor.Kind.DECLARATION)
            return

        if (options.includeNonPublic
                || descriptor !is MemberDescriptor
                || descriptor.getVisibility() in visibleToDocumentation) {
            append(descriptor.build(), kind)
        }
    }

    fun DocumentationNode.appendChildren(descriptors: Iterable<DeclarationDescriptor>, kind: DocumentationReference.Kind) {
        descriptors.forEach { descriptor -> appendChild(descriptor, kind) }
    }

    fun DocumentationNode.appendFile(sourceFile : JetFile) {
        val fragment = context.getPackageFragment(sourceFile)!!
        val packageNode = packages.getOrPut(fragment.fqName) {
            val packageNode = DocumentationNode(fragment.fqName.asString(), Content.Empty, Kind.Package)
            append(packageNode, DocumentationReference.Kind.Member)
            packageNode
        }
        packageNode.appendChildren(fragment.getMemberScope().getAllDescriptors(), DocumentationReference.Kind.Member)
    }

    fun DocumentationNode.appendFiles(sourceFiles : List<JetFile>) {
        for (sourceFile in sourceFiles) {
            appendFile(sourceFile)
        }
    }

    fun DeclarationDescriptor.build(): DocumentationNode = when (this) {
        is ClassDescriptor -> build()
        is ConstructorDescriptor -> build()
        is ScriptDescriptor -> build()
        is FunctionDescriptor -> build()
        is PropertyDescriptor -> build()
        is PropertyGetterDescriptor -> build()
        is PropertySetterDescriptor -> build()
        is TypeParameterDescriptor -> build()
        is ValueParameterDescriptor -> build()
        is ReceiverParameterDescriptor -> build()
        else -> throw IllegalStateException("Descriptor $this is not known")
    }

    fun ScriptDescriptor.build(): DocumentationNode = getClassDescriptor().build()
    fun ClassDescriptor.build(): DocumentationNode {
        val kind = when (getKind()) {
            ClassKind.OBJECT -> Kind.Object
            ClassKind.CLASS_OBJECT -> Kind.Object
            ClassKind.TRAIT -> Kind.Interface
            ClassKind.ENUM_CLASS -> Kind.Enum
            ClassKind.ENUM_ENTRY -> Kind.EnumItem
            else -> Kind.Class
        }
        val node = DocumentationNode(this, kind)
        node.appendSupertypes(this)
        if (getKind() != ClassKind.OBJECT) {
            node.appendChildren(getTypeConstructor().getParameters(), DocumentationReference.Kind.Detail)
            node.appendChildren(getConstructors(), DocumentationReference.Kind.Member)
            val classObjectDescriptor = getClassObjectDescriptor()
            if (classObjectDescriptor != null)
                node.appendChild(classObjectDescriptor, DocumentationReference.Kind.Member)
        }
        node.appendChildren(getDefaultType().getMemberScope().getAllDescriptors(), DocumentationReference.Kind.Member)
        register(this, node)
        return node
    }

    fun ConstructorDescriptor.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.Constructor)
        node.appendChildren(getValueParameters(), DocumentationReference.Kind.Detail)
        register(this, node)
        return node
    }

    fun FunctionDescriptor.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.Function)

        node.appendChildren(getTypeParameters(), DocumentationReference.Kind.Detail)
        getExtensionReceiverParameter()?.let { node.appendChild(it, DocumentationReference.Kind.Detail) }
        node.appendChildren(getValueParameters(), DocumentationReference.Kind.Detail)
        node.appendType(getReturnType())
        register(this, node)
        return node

    }

    fun PropertyDescriptor.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.Property)
        node.appendType(getReturnType())
        node.appendChildren(getTypeParameters(), DocumentationReference.Kind.Detail)
        getGetter()?.let {
            if (!it.isDefault())
                node.appendChild(it, DocumentationReference.Kind.Member)
        }
        getSetter()?.let {
            if (!it.isDefault())
                node.appendChild(it, DocumentationReference.Kind.Member)
        }

        register(this, node)
        return node
    }

    fun ValueParameterDescriptor.build(): DocumentationNode {
        val node = DocumentationNode(this, Kind.Parameter)
        node.appendType(getType())
        return node
    }

    fun TypeParameterDescriptor.build(): DocumentationNode {
        val doc = parseDocumentation(this)
        val name = getName().asString()
        val prefix = when (getVariance()) {
            Variance.IN_VARIANCE -> "in "
            Variance.OUT_VARIANCE -> "out "
            else -> ""
        }

        val node = DocumentationNode(prefix + name, doc, DocumentationNode.Kind.TypeParameter)

        val builtIns = KotlinBuiltIns.getInstance()
        for (constraint in getUpperBounds()) {
            if (constraint == builtIns.getDefaultBound())
                continue
            val constraintNode = DocumentationNode(constraint.toString(), Content.Empty, DocumentationNode.Kind.UpperBound)
            node.append(constraintNode, DocumentationReference.Kind.Detail)
        }

        for (constraint in getLowerBounds()) {
            if (builtIns.isNothing(constraint))
                continue
            val constraintNode = DocumentationNode(constraint.toString(), Content.Empty, DocumentationNode.Kind.LowerBound)
            node.append(constraintNode, DocumentationReference.Kind.Detail)
        }
        return node
    }

    fun ReceiverParameterDescriptor.build(): DocumentationNode {
        val node = DocumentationNode(getName().asString(), Content.Empty, Kind.Receiver)
        node.appendType(getType())
        return node
    }

    /**
     * Generates cross-references for documentation such as extensions for a type, inheritors, etc
     *
     * $receiver: [DocumentationContext] for node/descriptor resolutions
     * $node: [DocumentationNode] to visit
     */
    public fun resolveReferences(node: DocumentationNode) {
        node.details(DocumentationNode.Kind.Receiver).forEach { detail ->
            val receiverType = detail.detail(DocumentationNode.Kind.Type)
            val descriptor = links[receiverType]
            if (descriptor != null) {
                val typeNode = descriptorToNode[descriptor]
                // if typeNode is null, extension is to external type like in a library
                // should we create dummy node here?
                typeNode?.addReferenceTo(node, DocumentationReference.Kind.Extension)
            }
        }
        node.details(DocumentationNode.Kind.Supertype).forEach { detail ->
            val descriptor = links[detail]
            if (descriptor != null) {
                val typeNode = descriptorToNode[descriptor]
                typeNode?.addReferenceTo(node, DocumentationReference.Kind.Inheritor)
            }
        }
        node.details.forEach { detail ->
            val descriptor = links[detail]
            if (descriptor != null) {
                val typeNode = descriptorToNode[descriptor]
                if (typeNode != null) {
                    detail.addReferenceTo(typeNode, DocumentationReference.Kind.Link)
                }
            }
        }

        resolveContentLinks(node, node.doc)

        for (child in node.members) {
            resolveReferences(child)
        }
        for (child in node.details) {
            resolveReferences(child)
        }
    }

    fun getResolutionScope(node: DocumentationNode): JetScope {
        val descriptor = nodeToDescriptor[node] ?: throw IllegalArgumentException("Node is not known to this context")
        return context.getResolutionScope(descriptor)
    }

    fun resolveContentLinks(node: DocumentationNode, content: ContentNode) {
        val snapshot = content.children.toList()
        for (child in snapshot) {
            if (child is ContentExternalLink) {
                val referenceText = child.href
                if (Name.isValidIdentifier(referenceText)) {
                    val scope = getResolutionScope(node)
                    val symbolName = Name.guess(referenceText)
                    val symbol = scope.getLocalVariable(symbolName) ?:
                            scope.getProperties(symbolName).firstOrNull() ?:
                            scope.getFunctions(symbolName).firstOrNull() ?:
                            scope.getClassifier(symbolName)

                    if (symbol != null) {
                        val targetNode = descriptorToNode[symbol]
                        val contentLink = if (targetNode != null) ContentNodeLink(targetNode) else ContentExternalLink("#")

                        val index = content.children.indexOf(child)
                        content.children.remove(index)
                        contentLink.children.addAll(child.children)
                        content.children.add(index, contentLink)
                    }
                }
            }
            resolveContentLinks(node, child)
        }
    }
}