---
layout: post
title: org.jetbrains.dokka
---
[dokka](../index.md) / [org.jetbrains.dokka](index.md)

# org.jetbrains.dokka

```
package org.jetbrains.dokka
```
## Members
| Name | Summary |
|------|---------|
|[AnalysisEnvironment](AnalysisEnvironment/index.md)|Kotlin as a service entry point<br>&nbsp;&nbsp;`public class AnalysisEnvironment`<br>|
|[ConsoleGenerator](ConsoleGenerator/index.md)|&nbsp;&nbsp;`public class ConsoleGenerator`<br>|
|[DocumentationBuildingVisitor](DocumentationBuildingVisitor/index.md)|&nbsp;&nbsp;`class DocumentationBuildingVisitor`<br>|
|[DocumentationContent](DocumentationContent/index.md)|&nbsp;&nbsp;`class DocumentationContent`<br>|
|[DocumentationContentSection](DocumentationContentSection/index.md)|&nbsp;&nbsp;`class DocumentationContentSection`<br>|
|[DocumentationModule](DocumentationModule/index.md)|&nbsp;&nbsp;`public class DocumentationModule`<br>|
|[DocumentationNode](DocumentationNode/index.md)|&nbsp;&nbsp;`open public class DocumentationNode`<br>|
|[DocumentationNodeBuilder](DocumentationNodeBuilder/index.md)|&nbsp;&nbsp;`class DocumentationNodeBuilder`<br>|
|[DocumentationReference](DocumentationReference/index.md)|&nbsp;&nbsp;`public class DocumentationReference`<br>|
|[DokkaArguments](DokkaArguments/index.md)|&nbsp;&nbsp;`class DokkaArguments`<br>|
|[FileGenerator](FileGenerator/index.md)|&nbsp;&nbsp;`public class FileGenerator`<br>|
|[FoldersLocationService](FoldersLocationService/index.md)|&nbsp;&nbsp;`public class FoldersLocationService`<br>|
|[FoldersLocationService](FoldersLocationService.md)|&nbsp;&nbsp;`public fun FoldersLocationService(root: String): FoldersLocationService`<br>|
|[FormatService](FormatService/index.md)|&nbsp;&nbsp;`abstract public trait FormatService`<br>|
|[HtmlFormatService](HtmlFormatService/index.md)|&nbsp;&nbsp;`public class HtmlFormatService`<br>|
|[JavaSignatureGenerator](JavaSignatureGenerator/index.md)|&nbsp;&nbsp;`class JavaSignatureGenerator`<br>|
|[JekyllFormatService](JekyllFormatService/index.md)|&nbsp;&nbsp;`public class JekyllFormatService`<br>|
|[KotlinSignatureGenerator](KotlinSignatureGenerator/index.md)|&nbsp;&nbsp;`class KotlinSignatureGenerator`<br>|
|[Location](Location/index.md)|&nbsp;&nbsp;`class Location`<br>|
|[LocationService](LocationService/index.md)|&nbsp;&nbsp;`abstract public trait LocationService`<br>|
|[MarkdownFormatService](MarkdownFormatService/index.md)|&nbsp;&nbsp;`open public class MarkdownFormatService`<br>|
|[SignatureGenerator](SignatureGenerator/index.md)|&nbsp;&nbsp;`abstract trait SignatureGenerator`<br>|
|[SingleFolderLocationService](SingleFolderLocationService/index.md)|&nbsp;&nbsp;`public class SingleFolderLocationService`<br>|
|[SingleFolderLocationService](SingleFolderLocationService.md)|&nbsp;&nbsp;`public fun SingleFolderLocationService(root: String): SingleFolderLocationService`<br>|
|[TextFormatService](TextFormatService/index.md)|&nbsp;&nbsp;`public class TextFormatService`<br>|
|[analyze](analyze.md)|&nbsp;&nbsp;`fun JetCoreEnvironment.analyze(messageCollector: MessageCollector): AnalyzeExhaust`<br>|
|[analyzeAndReport](analyzeAndReport.md)|&nbsp;&nbsp;`fun AnalyzerWithCompilerReport.analyzeAndReport(files: List<JetFile>, analyser: ()->AnalyzeExhaust): Unit`<br>|
|[appendExtension](appendExtension.md)|&nbsp;&nbsp;`fun File.appendExtension(extension: String): File`<br>|
|[checkResolveChildren](checkResolveChildren.md)|&nbsp;&nbsp;`fun BindingContext.checkResolveChildren(node: DocumentationNode): Unit`<br>|
|[createDocumentationModule](createDocumentationModule.md)|&nbsp;&nbsp;`fun BindingContext.createDocumentationModule(name: String, module: ModuleDescriptor, packages: Set<FqName>): DocumentationModule`<br>|
|[escapeUri](escapeUri.md)|&nbsp;&nbsp;`public fun escapeUri(path: String): String`<br>|
|[extractText](extractText.md)|&nbsp;&nbsp;`fun KDoc.extractText(): String`<br>|
|[format](format.md)|&nbsp;&nbsp;`fun FormatService.format(node: Iterable<DocumentationNode>): String`<br>|
|[getAnnotationsPath](getAnnotationsPath.md)|&nbsp;&nbsp;`private fun getAnnotationsPath(paths: KotlinPaths, arguments: K2JVMCompilerArguments): MutableList<File>`<br>|
|[getClassInnerScope](getClassInnerScope.md)|&nbsp;&nbsp;`public fun getClassInnerScope(outerScope: JetScope, descriptor: ClassDescriptor): JetScope`<br>|
|[getDocumentation](getDocumentation.md)|&nbsp;&nbsp;`fun BindingContext.getDocumentation(descriptor: DeclarationDescriptor): DocumentationContent`<br>|
|[getDocumentationElements](getDocumentationElements.md)|&nbsp;&nbsp;`fun BindingContext.getDocumentationElements(descriptor: DeclarationDescriptor): List<KDoc>`<br>|
|[getFunctionInnerScope](getFunctionInnerScope.md)|&nbsp;&nbsp;`public fun getFunctionInnerScope(outerScope: JetScope, descriptor: FunctionDescriptor): JetScope`<br>|
|[getPackageFragment](getPackageFragment.md)|&nbsp;&nbsp;`fun BindingContext.getPackageFragment(file: JetFile): PackageFragmentDescriptor`<br>|
|[getPackageInnerScope](getPackageInnerScope.md)|&nbsp;&nbsp;`public fun getPackageInnerScope(descriptor: PackageFragmentDescriptor): JetScope`<br>|
|[getPropertyInnerScope](getPropertyInnerScope.md)|&nbsp;&nbsp;`public fun getPropertyInnerScope(outerScope: JetScope, descriptor: PropertyDescriptor): JetScope`<br>|
|[getRelativePath](getRelativePath.md)|&nbsp;&nbsp;`fun File.getRelativePath(name: File): File`<br>|
|[getResolutionScope](getResolutionScope.md)|&nbsp;&nbsp;`fun BindingContext.getResolutionScope(descriptor: DeclarationDescriptor): JetScope`<br>|
|[htmlEscape](htmlEscape.md)|&nbsp;&nbsp;`fun String.htmlEscape(): String`<br>|
|[isUserCode](isUserCode.md)|&nbsp;&nbsp;`fun DeclarationDescriptor.isUserCode(): Boolean`<br>|
|[main](main.md)|&nbsp;&nbsp;`public fun main(args: Array<String>): Unit`<br>|
|[parseLabel](parseLabel.md)|&nbsp;&nbsp;`fun String.parseLabel(index: Int): Pair<String, Int>`<br>|
|[parseSections](parseSections.md)|&nbsp;&nbsp;`fun String.parseSections(): List<DocumentationContentSection>`<br>|
|[path](path.md)|&nbsp;&nbsp;`fun path(node: DocumentationNode): String`<br>|
|[path](path/index.md)|&nbsp;&nbsp;`val DocumentationNode.path: List<DocumentationNode>`<br>|
|[previousSiblings](previousSiblings.md)|&nbsp;&nbsp;`fun PsiElement.previousSiblings(): Stream<PsiElement>`<br>|
|[relativeLocation](relativeLocation.md)|&nbsp;&nbsp;`fun LocationService.relativeLocation(node: DocumentationNode, link: DocumentationNode, extension: String): File`<br>|
