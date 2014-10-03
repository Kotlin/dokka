package org.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.resolve.name.FqName

/**
 * Context for documentation generation.
 *
 * Holds information about relations between [nodes](DocumentationNode) and [descriptors](DeclarationDescriptor) during documentation generation
 *
 * $bindingContext: symbol resolution context
 */
public class DocumentationContext(val bindingContext: BindingContext) {
    val descriptorToNode = hashMapOf<DeclarationDescriptor, DocumentationNode>()
    val nodeToDescriptor = hashMapOf<DocumentationNode, DeclarationDescriptor>()

    val relations = hashMapOf<DocumentationNode, DeclarationDescriptor>()

    fun attach(node: DocumentationNode, descriptor: DeclarationDescriptor) {
        relations.put(node, descriptor)
    }

    fun register(descriptor: DeclarationDescriptor, node: DocumentationNode) {
        descriptorToNode.put(descriptor, node)
        nodeToDescriptor.put(node, descriptor)
    }

    fun getResolutionScope(node: DocumentationNode): JetScope {
        val descriptor = nodeToDescriptor[node] ?: throw IllegalArgumentException("Node is not known to this context")
        return bindingContext.getResolutionScope(descriptor)
    }

    fun parseDocumentation(descriptor: DeclarationDescriptor): Content {
        val docText = bindingContext.getDocumentationElements(descriptor).map { it.extractText() }.join("\n")
        val tree = MarkdownProcessor.parse(docText)
        //println(tree.toTestString())
        val content = tree.toContent()
        return content
    }
}

fun BindingContext.createDocumentationModule(name: String,
                                             module: ModuleDescriptor,
                                             packages: Set<FqName>,
                                             options: DocumentationOptions = DocumentationOptions()): DocumentationModule {
    val documentationModule = DocumentationModule(name)
    val context = DocumentationContext(this)
    val visitor = DocumentationNodeBuilder(context)
    for (packageName in packages) {
        val pkg = module.getPackage(packageName)
        pkg!!.accept(DocumentationBuildingVisitor(this, options, visitor), documentationModule)
    }

    context.resolveReferences(documentationModule)

    // TODO: Uncomment for resolve verification
    // checkResolveChildren(documentationModule)
    return documentationModule
}
