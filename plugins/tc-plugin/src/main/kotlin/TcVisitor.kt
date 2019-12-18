package org.jetbrains.dokka.tc.plugin

import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.jetbrains.dokka.EnvironmentAndFacade
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.model.doc.Author
import org.jetbrains.dokka.model.doc.Constructor
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.DocTag
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Param
import org.jetbrains.dokka.model.doc.Property
import org.jetbrains.dokka.model.doc.Receiver
import org.jetbrains.dokka.model.doc.Return
import org.jetbrains.dokka.model.doc.Sample
import org.jetbrains.dokka.model.doc.See
import org.jetbrains.dokka.model.doc.Since
import org.jetbrains.dokka.model.doc.Suppress
import org.jetbrains.dokka.model.doc.Throws
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.parsers.MarkdownParser
import org.jetbrains.dokka.parsers.factories.DocNodesFromIElementFactory
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.intellij.markdown.parser.MarkdownParser as IntellijMarkdownParser

interface TcVisitor : TCCompile {
  val resolution: DokkaResolutionFacade
  val declarationDescriptor: DeclarationDescriptor
  val platform: Pair<PlatformData, EnvironmentAndFacade>?
  val ctx: DokkaContext
  val MD: MarkdownParser
    get() = MarkdownParser(resolution, declarationDescriptor)
  val messageCollector: MessageCollector?
    get() = env?.configuration?.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
  val env: KotlinCoreEnvironment?
    get() = platform?.second?.environment
  val ktPsi: KtPsiFactory?
    get() = env?.project?.let { KtPsiFactory(it) }

  fun default(text: String): (ASTNode) -> DocTag =
    MD.MarkdownVisitor(text)::visitNode

  fun DocNodesFromIElementFactory.onCodeSpan(text: String, node: ASTNode): DocTag =
    getInstance(
      node.type,
      children = listOf(
        getInstance(
          MarkdownTokenTypes.TEXT,
          body = text.substring(node.startOffset + 1, node.endOffset - 1).replace('\n', ' ').trimIndent()
        )
      )
    )

  fun DocNodesFromIElementFactory.onCodeFence(text: String, node: ASTNode): DocTag = getInstance(
    node.type,
    children = node.children.filter { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }.map {
      ktPsi?.createFile(text.substring(it.startOffset, it.endOffset))?.let { file ->
        env?.project?.let { project ->
          println("Project:${project.name}, and file: ${file.isCompiled}")
          // val result = analyzeWithAllCompilerChecks(file, project)
          // println("AnalysisResult: Is an Error ${result.isError()}")
          /*file.declarations.first().resolveToDescriptorIfAny()?.findPsi()?.let {psi ->
            messageCollector?.report(CompilerMessageSeverity.WARNING, "META: Compile Error: with Diagnostics", MessageUtil.psiElementToMessageLocation(psi))
          }*/
        }
      }
      default(text)(it)
    },
    params = node.children.find { it.type == MarkdownTokenTypes.FENCE_LANG }
      ?.let {
        val lang = text.substring(it.startOffset, it.endOffset)
        println("CurrentLanguage: $lang")
        mapOf("lang" to lang)
      }
      ?: emptyMap()
  )

  fun DocNodesFromIElementFactory.onCodeBlock(text: String, node: ASTNode): DocTag =
    getInstance(node.type, children = node.children.map { default(text)(it) })

  fun visit(text: String, node: ASTNode, ctx: DokkaContext): DocTag =
    DocNodesFromIElementFactory.run {
      node.debug(text)
      when (node.type) {
        MarkdownElementTypes.CODE_BLOCK -> onCodeBlock(text, node)// codeBlocksHandler(node)
        MarkdownElementTypes.CODE_FENCE -> onCodeFence(text, node)// codeFencesHandler(node)
        MarkdownElementTypes.CODE_SPAN -> onCodeSpan(text, node)// codeSpansHandler(node)
        MarkdownElementTypes.MARKDOWN_FILE -> getInstance(MarkdownElementTypes.PARAGRAPH,
          children = node.children.map { visit(text, it, ctx) })
        else -> println("Default Function on ${node.type}:\nWithText:$text").run {
          default(text)(node)
        }
      }
    }// KtLightClassForFacade

  fun ASTNode.debug(text: String): Unit {
    val section = text.substring(startOffset, endOffset)
    println("Inspecting:$type in Platform:${platform?.first?.platformType}\nTraverse section: from $startOffset to $endOffset\n$section\n--------------\n")
  }

  fun String.toMDAST(): Pair<String, ASTNode> =
    this to IntellijMarkdownParser(CommonMarkFlavourDescriptor()).buildMarkdownTreeFromString(this)

  fun parseKDoc(tag: KDocTag?): DocumentationNode =
    tag?.run {
      DocumentationNode(
        (listOf(this) + children).filterIsInstance<KDocTag>().map {
          MD.run {
            when (it.knownTag) {
              null -> it.getContent().toMDAST().run { Description(visit(first, second, ctx)) }
              KDocKnownTag.SAMPLE -> it.getContent().toMDAST().run {
                Sample(
                  visit(first, second, ctx),
                  it.getSubjectName()!!
                )
              }
              KDocKnownTag.AUTHOR -> Author(parseStringToDocNode(it.getContent()))
              KDocKnownTag.THROWS -> Throws(parseStringToDocNode(it.getContent()), it.getSubjectName()!!)
              KDocKnownTag.EXCEPTION -> Throws(
                parseStringToDocNode(it.getContent()),
                it.getSubjectName()!!
              )
              KDocKnownTag.PARAM -> Param(parseStringToDocNode(it.getContent()), it.getSubjectName()!!)
              KDocKnownTag.RECEIVER -> Receiver(parseStringToDocNode(it.getContent()))
              KDocKnownTag.RETURN -> Return(parseStringToDocNode(it.getContent()))
              KDocKnownTag.SEE -> See(parseStringToDocNode(it.getContent()), it.getSubjectName()!!)
              KDocKnownTag.SINCE -> Since(parseStringToDocNode(it.getContent()))
              KDocKnownTag.CONSTRUCTOR -> Constructor(parseStringToDocNode(it.getContent()))
              KDocKnownTag.PROPERTY -> Property(
                parseStringToDocNode(it.getContent()),
                it.getSubjectName()!!
              )
              KDocKnownTag.SUPPRESS -> Suppress(parseStringToDocNode(it.getContent()))
            }
          }
        }
      )
    } ?: DocumentationNode(emptyList())

  companion object {
    fun default(
      resolution: DokkaResolutionFacade,
      decl: DeclarationDescriptor,
      ctx: DokkaContext,
      platform: Pair<PlatformData, EnvironmentAndFacade>?
    ): TcVisitor =
      object : TcVisitor {
        override val resolution: DokkaResolutionFacade = resolution
        override val declarationDescriptor: DeclarationDescriptor = decl
        override val ctx: DokkaContext = ctx
        override val platform: Pair<PlatformData, EnvironmentAndFacade>? = platform
      }
  }
}
