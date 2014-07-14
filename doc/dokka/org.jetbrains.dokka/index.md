[dokka](../index.md) / [org.jetbrains.dokka](index.md)

# org.jetbrains.dokka

```
package org.jetbrains.dokka
```
## Members
| Name | Summary |
|------|---------|
|[AnalysisEnvironment](AnalysisEnvironment/index.md)|Kotlin as a service entry point<br>`public class AnalysisEnvironment`<br>|
|[ConsoleGenerator](ConsoleGenerator/index.md)|`public class ConsoleGenerator`<br>|
|[DocumentationBuildingVisitor](DocumentationBuildingVisitor/index.md)|`class DocumentationBuildingVisitor`<br>|
|[DocumentationContent](DocumentationContent/index.md)|`class DocumentationContent`<br>|
|[DocumentationContentSection](DocumentationContentSection/index.md)|`class DocumentationContentSection`<br>|
|[DocumentationModule](DocumentationModule/index.md)|`public class DocumentationModule`<br>|
|[DocumentationNode](DocumentationNode/index.md)|`open public class DocumentationNode`<br>|
|[DocumentationNodeBuilder](DocumentationNodeBuilder/index.md)|`class DocumentationNodeBuilder`<br>|
|[DocumentationReference](DocumentationReference/index.md)|`public class DocumentationReference`<br>|
|[DokkaArguments](DokkaArguments/index.md)|`class DokkaArguments`<br>|
|[FileGenerator](FileGenerator/index.md)|`public class FileGenerator`<br>|
|[FoldersLocationService](FoldersLocationService.md)|`public fun FoldersLocationService(root: String): FoldersLocationService`<br>|
|[FoldersLocationService](FoldersLocationService/index.md)|`public class FoldersLocationService`<br>|
|[FormatService](FormatService/index.md)|`abstract public trait FormatService`<br>|
|[HtmlFormatService](HtmlFormatService/index.md)|`public class HtmlFormatService`<br>|
|[JavaSignatureGenerator](JavaSignatureGenerator/index.md)|`class JavaSignatureGenerator`<br>|
|[KotlinSignatureGenerator](KotlinSignatureGenerator/index.md)|`class KotlinSignatureGenerator`<br>|
|[Location](Location/index.md)|`class Location`<br>|
|[LocationService](LocationService/index.md)|`abstract public trait LocationService`<br>|
|[MarkdownFormatService](MarkdownFormatService/index.md)|`public class MarkdownFormatService`<br>|
|[SignatureGenerator](SignatureGenerator/index.md)|`abstract trait SignatureGenerator`<br>|
|[SingleFolderLocationService](SingleFolderLocationService/index.md)|`public class SingleFolderLocationService`<br>|
|[SingleFolderLocationService](SingleFolderLocationService.md)|`public fun SingleFolderLocationService(root: String): SingleFolderLocationService`<br>|
|[TextFormatService](TextFormatService/index.md)|`public class TextFormatService`<br>|
|[analyze](analyze.md)|`fun JetCoreEnvironment.analyze(messageCollector: MessageCollector): AnalyzeExhaust`<br>|
|[analyzeAndReport](analyzeAndReport.md)|`fun AnalyzerWithCompilerReport.analyzeAndReport(files: List<JetFile>, analyser: ()->AnalyzeExhaust): Unit`<br>|
|[appendExtension](appendExtension.md)|`fun File.appendExtension(extension: String): File`<br>|
|[checkResolveChildren](checkResolveChildren.md)|`fun BindingContext.checkResolveChildren(node: DocumentationNode): Unit`<br>|
|[createDocumentationModule](createDocumentationModule.md)|`fun BindingContext.createDocumentationModule(name: String, module: ModuleDescriptor, packages: Set<FqName>): DocumentationModule`<br>|
|[escapeUri](escapeUri.md)|`public fun escapeUri(path: String): String`<br>|
|[extractText](extractText.md)|`fun KDoc.extractText(): String`<br>|
|[format](format.md)|`fun FormatService.format(node: Iterable<DocumentationNode>): String`<br>|
|[getAnnotationsPath](getAnnotationsPath.md)|`private fun getAnnotationsPath(paths: KotlinPaths, arguments: K2JVMCompilerArguments): MutableList<File>`<br>|
|[getClassInnerScope](getClassInnerScope.md)|`public fun getClassInnerScope(outerScope: JetScope, descriptor: ClassDescriptor): JetScope`<br>|
|[getDocumentation](getDocumentation.md)|`fun BindingContext.getDocumentation(descriptor: DeclarationDescriptor): DocumentationContent`<br>|
|[getDocumentationElements](getDocumentationElements.md)|`fun BindingContext.getDocumentationElements(descriptor: DeclarationDescriptor): List<KDoc>`<br>|
|[getFunctionInnerScope](getFunctionInnerScope.md)|`public fun getFunctionInnerScope(outerScope: JetScope, descriptor: FunctionDescriptor): JetScope`<br>|
|[getPackageFragment](getPackageFragment.md)|`fun BindingContext.getPackageFragment(file: JetFile): PackageFragmentDescriptor`<br>|
|[getPackageInnerScope](getPackageInnerScope.md)|`public fun getPackageInnerScope(descriptor: PackageFragmentDescriptor): JetScope`<br>|
|[getPropertyInnerScope](getPropertyInnerScope.md)|`public fun getPropertyInnerScope(outerScope: JetScope, descriptor: PropertyDescriptor): JetScope`<br>|
|[getRelativePath](getRelativePath.md)|`fun File.getRelativePath(name: File): File`<br>|
|[getResolutionScope](getResolutionScope.md)|`fun BindingContext.getResolutionScope(descriptor: DeclarationDescriptor): JetScope`<br>|
|[htmlEscape](htmlEscape.md)|`fun String.htmlEscape(): String`<br>|
|[isUserCode](isUserCode.md)|`fun DeclarationDescriptor.isUserCode(): Boolean`<br>|
|[main](main.md)|`public fun main(args: Array<String>): Unit`<br>|
|[parseLabel](parseLabel.md)|`fun String.parseLabel(index: Int): Pair<String, Int>`<br>|
|[parseSections](parseSections.md)|`fun String.parseSections(): List<DocumentationContentSection>`<br>|
|[path](path/index.md)|`val DocumentationNode.path: List<DocumentationNode>`<br>|
|[path](path.md)|`fun path(node: DocumentationNode): String`<br>|
|[previousSiblings](previousSiblings.md)|`fun PsiElement.previousSiblings(): Stream<PsiElement>`<br>|
|[relativeLocation](relativeLocation.md)|`fun LocationService.relativeLocation(node: DocumentationNode, link: DocumentationNode, extension: String): File`<br>|
