package org.jetbrains.dokka

import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.name.FqName

public class DocumentationModule(val module: ModuleDescriptor) : DocumentationNode(module, "module", DocumentationContent.Empty, DocumentationNode.Kind.Module) {
    fun merge(other: DocumentationModule): DocumentationModule {
        val model = DocumentationModule(module)
        model.addAllReferencesFrom(other)
        model.addAllReferencesFrom(this)
        return model
    }
}

fun BindingContext.createDocumentationModule(module: ModuleDescriptor, packages: Set<FqName>): DocumentationModule {
    val documentationModule = DocumentationModule(module)
    val visitor = DocumentationNodeBuilder(this)
    for (packageName in packages) {
        val pkg = module.getPackage(packageName)
        pkg!!.accept(DocumentationBuildingVisitor(this, visitor), documentationModule)
    }

    // TODO: Uncomment for resolve verification
    // checkResolveChildren(documentationModule)
    return documentationModule
}
