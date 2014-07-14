package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.name.FqName

public class DocumentationModule(name: String, val module: ModuleDescriptor) : DocumentationNode(module, name, DocumentationContent.Empty, DocumentationNode.Kind.Module) {
    fun merge(other: DocumentationModule): DocumentationModule {
        val model = DocumentationModule(name, module)
        model.addAllReferencesFrom(other)
        model.addAllReferencesFrom(this)
        return model
    }
}

fun BindingContext.createDocumentationModule(name: String, module: ModuleDescriptor, packages: Set<FqName>): DocumentationModule {
    val documentationModule = DocumentationModule(name, module)
    val visitor = DocumentationNodeBuilder(this)
    for (packageName in packages) {
        val pkg = module.getPackage(packageName)
        pkg!!.accept(DocumentationBuildingVisitor(this, visitor), documentationModule)
    }

    // TODO: Uncomment for resolve verification
    // checkResolveChildren(documentationModule)
    return documentationModule
}
