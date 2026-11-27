/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package linking

import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClass
import org.jetbrains.dokka.model.DFunction
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.DProperty
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.InheritedMember
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.ClasslikePageNode
import org.jetbrains.dokka.pages.ContentDRILink
import org.jetbrains.dokka.pages.ContentDivergentGroup
import org.jetbrains.dokka.pages.ContentKind
import org.jetbrains.dokka.pages.ContentStyle
import org.jetbrains.dokka.pages.ContentTable
import org.jetbrains.dokka.pages.ContentText
import org.jetbrains.dokka.pages.RootPageNode
import org.jsoup.nodes.Element
import signatures.renderedContent
import testApi.testRunner.DokkaSourceSetBuilder
import utils.TestOutputWriterPlugin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InheritedMemberLinkingTest : BaseAbstractTest() {

    @Test
    fun `row title prefers subclass page when member is declared on common and jvm but inherited on js`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            mixedInheritanceSources(
                baseName = "AaaBase",
                subName = "ZzzSub",
                functionName = "member",
            ),
            multiplatformConfiguration(),
            pluginOverrides = listOf(writerPlugin)
        ) {
            documentablesTransformationStage = { module ->
                module.assertMixedFunctionInheritance(
                    baseName = "AaaBase",
                    subName = "ZzzSub",
                    functionName = "member",
                )
            }
            pagesGenerationStage = { root ->
                val rowLink = root.rowTitleLink("ZzzSub", ContentKind.Functions, "member")
                assertEquals("ZzzSub", rowLink.address.classNames)
                assertEquals("member", rowLink.address.callable?.name)

                val signatureOwners = root.signatureOwners("ZzzSub", ContentKind.Functions, "member")
                assertEquals("ZzzSub", signatureOwners.getValue("common").classNames)
                assertEquals("ZzzSub", signatureOwners.getValue("jvm").classNames)
                assertEquals("AaaBase", signatureOwners.getValue("js").classNames)
            }
            renderingStage = { _, _ ->
                val page = writerPlugin.writer.renderedContent("root/test/-zzz-sub/index.html")
                assertEquals(
                    "member.html",
                    page.functionRowTitleHref("member")
                )
                assertTrue(
                    "root/test/-zzz-sub/member.html" in writerPlugin.writer.contents,
                    "Declared subclass member page should be generated"
                )

                val signatureHrefs = page.functionSignatureHrefs("member")
                assertEquals("member.html", signatureHrefs.getValue("common"))
                assertEquals("member.html", signatureHrefs.getValue("jvm"))
                assertEquals("../-aaa-base/member.html", signatureHrefs.getValue("js"))
            }
        }
    }

    @Test
    fun `row title follows declaration ownership rather than source set order or stdlib names`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            mixedInheritanceSources(
                baseName = "AlphaBase",
                subName = "OmegaSub",
                functionName = "compute",
            ),
            multiplatformConfiguration(reverseSourceSetOrder = true),
            pluginOverrides = listOf(writerPlugin)
        ) {
            documentablesTransformationStage = { module ->
                module.assertMixedFunctionInheritance(
                    baseName = "AlphaBase",
                    subName = "OmegaSub",
                    functionName = "compute",
                )
            }
            pagesGenerationStage = { root ->
                val rowLink = root.rowTitleLink("OmegaSub", ContentKind.Functions, "compute")
                assertEquals("OmegaSub", rowLink.address.classNames)
                assertEquals("compute", rowLink.address.callable?.name)
            }
            renderingStage = { _, _ ->
                val page = writerPlugin.writer.renderedContent("root/test/-omega-sub/index.html")
                assertEquals("compute.html", page.functionRowTitleHref("compute"))
                assertTrue("root/test/-omega-sub/compute.html" in writerPlugin.writer.contents)
                assertEquals(
                    "../-alpha-base/compute.html",
                    page.functionSignatureHrefs("compute").getValue("js")
                )
            }
        }
    }

    @Test
    fun `property row title prefers declared property page with mixed inheritance`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            mixedInheritanceSources(
                baseName = "AaaBase",
                subName = "ZzzSub",
                propertyName = "item",
            ),
            multiplatformConfiguration(),
            pluginOverrides = listOf(writerPlugin)
        ) {
            documentablesTransformationStage = { module ->
                module.assertMixedPropertyInheritance(
                    baseName = "AaaBase",
                    subName = "ZzzSub",
                    propertyName = "item",
                )
            }
            pagesGenerationStage = { root ->
                val rowLink = root.rowTitleLink("ZzzSub", ContentKind.Properties, "item")
                assertEquals("ZzzSub", rowLink.address.classNames)
                assertEquals("item", rowLink.address.callable?.name)

                val signatureOwners = root.signatureOwners("ZzzSub", ContentKind.Properties, "item")
                assertEquals("ZzzSub", signatureOwners.getValue("common").classNames)
                assertEquals("ZzzSub", signatureOwners.getValue("jvm").classNames)
                assertEquals("AaaBase", signatureOwners.getValue("js").classNames)
            }
            renderingStage = { _, _ ->
                val page = writerPlugin.writer.renderedContent("root/test/-zzz-sub/index.html")
                assertEquals("item.html", page.propertyRowTitleHref("item"))
                assertTrue("root/test/-zzz-sub/item.html" in writerPlugin.writer.contents)
                assertEquals(
                    "../-aaa-base/item.html",
                    page.propertySignatureHrefs("item").getValue("js")
                )
            }
        }
    }

    @Test
    fun `fully inherited member keeps superclass destination and does not invent a subclass page`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            inheritedEverywhereSources(
                baseName = "AaaBase",
                subName = "ZzzSub",
                functionName = "member",
            ),
            multiplatformConfiguration(),
            pluginOverrides = listOf(writerPlugin)
        ) {
            documentablesTransformationStage = { module ->
                val functions = module.subclassFunctions("ZzzSub", "member")
                assertTrue(functions.isNotEmpty(), "expected inherited member on subclass")
                assertTrue(functions.all { it.isFullyInherited() })
                assertTrue(functions.all { it.dri.classNames == "AaaBase" })
            }
            pagesGenerationStage = { root ->
                val rowLink = root.rowTitleLink("ZzzSub", ContentKind.Functions, "member")
                assertEquals("AaaBase", rowLink.address.classNames)
            }
            renderingStage = { _, _ ->
                val page = writerPlugin.writer.renderedContent("root/test/-zzz-sub/index.html")
                assertEquals("../-aaa-base/member.html", page.functionRowTitleHref("member"))
                assertFalse(
                    "root/test/-zzz-sub/member.html" in writerPlugin.writer.contents,
                    "Fully inherited members should not get a subclass member page"
                )
                assertTrue("root/test/-aaa-base/member.html" in writerPlugin.writer.contents)
            }
        }
    }

    @Test
    fun `declared everywhere and single source set inherited members do not regress`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            declaredEverywhereSources(
                baseName = "AaaBase",
                subName = "ZzzSub",
                functionName = "member",
            ),
            multiplatformConfiguration(),
            pluginOverrides = listOf(writerPlugin)
        ) {
            documentablesTransformationStage = { module ->
                val functions = module.subclassFunctions("ZzzSub", "member")
                val declared = functions.filterNot { it.isFullyInherited() }
                assertTrue(declared.isNotEmpty())
                assertTrue(declared.all { it.dri.classNames == "ZzzSub" })
                declared.forEach { it.assertNotExcludedByAnyInheritedPredicate() }
            }
            pagesGenerationStage = { root ->
                val rowLink = root.rowTitleLink("ZzzSub", ContentKind.Functions, "member")
                assertEquals("ZzzSub", rowLink.address.classNames)
            }
            renderingStage = { _, _ ->
                val page = writerPlugin.writer.renderedContent("root/test/-zzz-sub/index.html")
                assertEquals("member.html", page.functionRowTitleHref("member"))
                assertTrue("root/test/-zzz-sub/member.html" in writerPlugin.writer.contents)
            }
        }

        val inheritedWriter = TestOutputWriterPlugin()
        testInline(
            """
            |/src/main/kotlin/test/Test.kt
            |package test
            |
            |open class AaaBase {
            |    open fun member(): String = "base"
            |}
            |
            |class ZzzSub : AaaBase()
            """.trimMargin(),
            dokkaConfiguration {
                sourceSets {
                    sourceSet {
                        sourceRoots = listOf("src/main/kotlin")
                        analysisPlatform = "jvm"
                    }
                }
            },
            pluginOverrides = listOf(inheritedWriter)
        ) {
            documentablesTransformationStage = { module ->
                val functions = module.subclassFunctions("ZzzSub", "member")
                assertEquals(1, functions.size)
                assertTrue(functions.single().isFullyInherited())
                assertEquals("AaaBase", functions.single().dri.classNames)
            }
            pagesGenerationStage = { root ->
                val rowLink = root.rowTitleLink("ZzzSub", ContentKind.Functions, "member")
                assertEquals("AaaBase", rowLink.address.classNames)
            }
            renderingStage = { _, _ ->
                val page = inheritedWriter.writer.renderedContent("root/test/-zzz-sub/index.html")
                assertEquals("../-aaa-base/member.html", page.functionRowTitleHref("member"))
                assertFalse("root/test/-zzz-sub/member.html" in inheritedWriter.writer.contents)
            }
        }
    }

    @Test
    fun `mixed inherited and declared source set metadata on a merged member still prefers the declared page`() {
        val writerPlugin = TestOutputWriterPlugin()
        testInline(
            mixedInheritanceSources(
                baseName = "AaaBase",
                subName = "ZzzSub",
                functionName = "member",
            ),
            multiplatformConfiguration(),
            pluginOverrides = listOf(writerPlugin)
        ) {
            documentablesTransformationStage = { module ->
                val functions = module.subclassFunctions("ZzzSub", "member")
                val declared = functions.filterNot { it.isFullyInherited() }
                assertTrue(declared.isNotEmpty(), "expected a declared candidate")
                declared.forEach { it.assertNotExcludedByAnyInheritedPredicate() }

                val inherited = functions.filter { it.isFullyInherited() }
                assertTrue(inherited.isNotEmpty(), "expected a fully inherited candidate that would otherwise sort first")
                assertTrue(inherited.all { it.dri.classNames == "AaaBase" })
            }
            pagesGenerationStage = { root ->
                val rowLink = root.rowTitleLink("ZzzSub", ContentKind.Functions, "member")
                assertEquals("ZzzSub", rowLink.address.classNames)
            }
            renderingStage = { _, _ ->
                assertEquals(
                    "member.html",
                    writerPlugin.writer.renderedContent("root/test/-zzz-sub/index.html")
                        .functionRowTitleHref("member")
                )
            }
        }
    }

    private fun multiplatformConfiguration(reverseSourceSetOrder: Boolean = false) = dokkaConfiguration {
        sourceSets {
            val common = sourceSet {
                name = "common"
                displayName = "common"
                analysisPlatform = "common"
                sourceRoots = listOf("src/commonMain/kotlin")
            }
            val jvm: DokkaSourceSetBuilder.() -> Unit = {
                name = "jvm"
                displayName = "jvm"
                analysisPlatform = "jvm"
                sourceRoots = listOf("src/jvmMain/kotlin")
                dependentSourceSets = setOf(common.value.sourceSetID)
            }
            val js: DokkaSourceSetBuilder.() -> Unit = {
                name = "js"
                displayName = "js"
                analysisPlatform = "js"
                sourceRoots = listOf("src/jsMain/kotlin")
                dependentSourceSets = setOf(common.value.sourceSetID)
            }
            if (reverseSourceSetOrder) {
                sourceSet(js)
                sourceSet(jvm)
            } else {
                sourceSet(jvm)
                sourceSet(js)
            }
        }
    }

    private fun mixedInheritanceSources(
        baseName: String,
        subName: String,
        functionName: String? = null,
        propertyName: String? = null,
    ): String {
        val expectBase = expectMembers(functionName, propertyName, overridden = false)
        val expectSub = expectMembers(functionName, propertyName, overridden = true)
        val actualBase = actualMembers(functionName, propertyName, overridden = false, body = "base")
        val actualSub = actualMembers(functionName, propertyName, overridden = true, body = "sub")
        return """
            |/src/commonMain/kotlin/test/Test.kt
            |package test
            |
            |expect open class $baseName {
            |$expectBase
            |}
            |
            |expect open class $subName : $baseName {
            |$expectSub
            |}
            |
            |/src/jvmMain/kotlin/test/Test.kt
            |package test
            |
            |actual open class $baseName {
            |$actualBase
            |}
            |
            |actual open class $subName : $baseName() {
            |$actualSub
            |}
            |
            |/src/jsMain/kotlin/test/Test.kt
            |package test
            |
            |actual open class $baseName {
            |$actualBase
            |}
            |
            |actual open class $subName : $baseName()
        """.trimMargin()
    }

    private fun inheritedEverywhereSources(
        baseName: String,
        subName: String,
        functionName: String,
    ): String {
        val expectBase = expectMembers(functionName, propertyName = null, overridden = false)
        val actualBase = actualMembers(functionName, propertyName = null, overridden = false, body = "base")
        return """
            |/src/commonMain/kotlin/test/Test.kt
            |package test
            |
            |expect open class $baseName {
            |$expectBase
            |}
            |
            |expect class $subName : $baseName
            |
            |/src/jvmMain/kotlin/test/Test.kt
            |package test
            |
            |actual open class $baseName {
            |$actualBase
            |}
            |
            |actual class $subName : $baseName()
            |
            |/src/jsMain/kotlin/test/Test.kt
            |package test
            |
            |actual open class $baseName {
            |$actualBase
            |}
            |
            |actual class $subName : $baseName()
        """.trimMargin()
    }

    private fun declaredEverywhereSources(
        baseName: String,
        subName: String,
        functionName: String,
    ): String {
        val expectBase = expectMembers(functionName, propertyName = null, overridden = false)
        val expectSub = expectMembers(functionName, propertyName = null, overridden = true)
        val actualBase = actualMembers(functionName, propertyName = null, overridden = false, body = "base")
        val actualSub = actualMembers(functionName, propertyName = null, overridden = true, body = "sub")
        return """
            |/src/commonMain/kotlin/test/Test.kt
            |package test
            |
            |expect open class $baseName {
            |$expectBase
            |}
            |
            |expect open class $subName : $baseName {
            |$expectSub
            |}
            |
            |/src/jvmMain/kotlin/test/Test.kt
            |package test
            |
            |actual open class $baseName {
            |$actualBase
            |}
            |
            |actual open class $subName : $baseName() {
            |$actualSub
            |}
            |
            |/src/jsMain/kotlin/test/Test.kt
            |package test
            |
            |actual open class $baseName {
            |$actualBase
            |}
            |
            |actual open class $subName : $baseName() {
            |$actualSub
            |}
        """.trimMargin()
    }

    private fun expectMembers(
        functionName: String?,
        propertyName: String?,
        overridden: Boolean,
    ): String = buildString {
        val modifier = if (overridden) "override " else "open "
        if (functionName != null) appendLine("    ${modifier}fun $functionName(): String")
        if (propertyName != null) appendLine("    ${modifier}val $propertyName: String")
    }.trimEnd()

    private fun actualMembers(
        functionName: String?,
        propertyName: String?,
        overridden: Boolean,
        body: String,
    ): String = buildString {
        val modifier = if (overridden) "override " else "open "
        if (functionName != null) appendLine("    actual ${modifier}fun $functionName(): String = \"$body\"")
        if (propertyName != null) appendLine("    actual ${modifier}val $propertyName: String = \"$body\"")
    }.trimEnd()

    private fun DModule.assertMixedFunctionInheritance(
        baseName: String,
        subName: String,
        functionName: String,
    ) {
        val functions = subclassFunctions(subName, functionName)
        assertTrue(functions.size >= 2, "expected distinct declared and inherited owners, got $functions")

        val declared = functions.filterNot { it.isFullyInherited() }
        val inherited = functions.filter { it.isFullyInherited() }
        assertTrue(declared.any { it.dri.classNames == subName }, "declared DRI should belong to $subName: $declared")
        assertTrue(inherited.any { it.dri.classNames == baseName }, "inherited DRI should belong to $baseName: $inherited")

        val declaredSourceSets = declared.flatMap { it.sourceSetNames() }.toSet()
        assertTrue("common" in declaredSourceSets || declaredSourceSets.any { it.contains("common", ignoreCase = true) })
        assertTrue("jvm" in declaredSourceSets || declaredSourceSets.any { it.contains("jvm", ignoreCase = true) })
        assertTrue(inherited.flatMap { it.sourceSetNames() }.any { it.contains("js", ignoreCase = true) })

        declared.forEach { it.assertNotExcludedByAnyInheritedPredicate() }
    }

    private fun DModule.assertMixedPropertyInheritance(
        baseName: String,
        subName: String,
        propertyName: String,
    ) {
        val properties = subclassProperties(subName, propertyName)
        assertTrue(properties.size >= 2, "expected distinct declared and inherited owners, got $properties")

        val declared = properties.filterNot { it.isFullyInherited() }
        val inherited = properties.filter { it.isFullyInherited() }
        assertTrue(declared.any { it.dri.classNames == subName })
        assertTrue(inherited.any { it.dri.classNames == baseName })
        declared.forEach { it.assertNotExcludedByAnyInheritedPredicate() }
    }

    private fun DModule.subclassFunctions(className: String, functionName: String): List<DFunction> =
        subclass(className).functions.filter { it.name == functionName }

    private fun DModule.subclassProperties(className: String, propertyName: String): List<DProperty> =
        subclass(className).properties.filter { it.name == propertyName }

    private fun DModule.subclass(className: String): DClass {
        val matches = packages.filter { it.name == "test" }.flatMap { it.classlikes }.filter { it.name == className }
        assertEquals(
            1,
            matches.size,
            "expected a single merged $className, got ${
                matches.map { it.sourceSets.map { sourceSet -> sourceSet.sourceSetID.sourceSetName } }
            }"
        )
        return matches.single() as DClass
    }

    private fun Documentable.sourceSetNames(): Set<String> =
        sourceSets.map { it.sourceSetID.sourceSetName }.toSet()

    /**
     * Mirrors [org.jetbrains.dokka.base.translators.documentables.DefaultPageCreator] inherited-member
     * semantics: a callable is fully inherited only when every source set marks it inherited.
     */
    private fun Documentable.isFullyInherited(): Boolean {
        val inheritedMember = inheritedMember() ?: return false
        return sourceSets.all { sourceSet -> inheritedMember.isInherited(sourceSet) }
    }

    /**
     * A merged documentable can carry InheritedMember for some source sets and still be declared
     * in others. Using an "any inherited" predicate would wrongly drop that candidate.
     */
    private fun Documentable.assertNotExcludedByAnyInheritedPredicate() {
        val inheritedMember = inheritedMember()
        val anyInherited = inheritedMember != null && sourceSets.any { inheritedMember.isInherited(it) }
        if (anyInherited) {
            assertFalse(isFullyInherited(), "mixed inherited/declared metadata must still count as declared")
        }
    }

    private fun Documentable.inheritedMember(): InheritedMember? = when (this) {
        is DFunction -> extra[InheritedMember]
        is DProperty -> extra[InheritedMember]
        else -> null
    }

    private fun RootPageNode.rowTitleLink(
        className: String,
        kind: ContentKind,
        memberName: String,
    ): ContentDRILink {
        val row = memberRow(className, kind, memberName)
        val link = row.dfs { node ->
            node is ContentDRILink && node.children.any { child ->
                child is ContentText && child.text == memberName && ContentStyle.RowTitle in child.style
            }
        } as? ContentDRILink
        return assertNotNull(link, "row title ContentDRILink for $memberName on $className")
    }

    private fun RootPageNode.signatureOwners(
        className: String,
        kind: ContentKind,
        memberName: String,
    ): Map<String, DRI> {
        val row = memberRow(className, kind, memberName)
        val divergent = row.dfs { it is ContentDivergentGroup } as ContentDivergentGroup
        return divergent.children.flatMap { instance ->
            val dri = instance.dci.dri.single()
            instance.sourceSets.map { sourceSet -> sourceSet.name to dri }
        }.toMap()
    }

    private fun RootPageNode.memberRow(
        className: String,
        kind: ContentKind,
        memberName: String,
    ) = run {
        val classPage = dfs { it is ClasslikePageNode && it.name == className } as ClasslikePageNode
        val table = classPage.content.dfs { it is ContentTable && it.dci.kind == kind } as ContentTable
        table.children.single { group ->
            group.dfs { node ->
                node is ContentText && node.text == memberName && ContentStyle.RowTitle in node.style
            } != null
        }
    }

    private fun Element.functionRowTitleHref(memberName: String): String =
        rowTitleHref("FUNCTION", memberName)

    private fun Element.propertyRowTitleHref(memberName: String): String =
        rowTitleHref("PROPERTY", memberName)

    private fun Element.rowTitleHref(tab: String, memberName: String): String {
        val href = select("div[data-togglable=$tab] .table-row .main-subrow > div:first-child a")
            .first { it.text() == memberName }
            .attr("href")
        assertTrue(href.isNotBlank(), "expected row title href for $memberName")
        return href
    }

    private fun Element.functionSignatureHrefs(memberName: String): Map<String, String> =
        signatureHrefs("FUNCTION", memberName)

    private fun Element.propertySignatureHrefs(memberName: String): Map<String, String> =
        signatureHrefs("PROPERTY", memberName)

    private fun Element.signatureHrefs(tab: String, memberName: String): Map<String, String> {
        val row = select("div[data-togglable=$tab] .table-row").first { row ->
            row.select(".main-subrow > div:first-child a").any { it.text() == memberName }
        }
        return row.select(".sourceset-dependent-content").associate { content ->
            val sourceSet = content.attr("data-togglable").substringAfterLast('/')
            val href = content.select(".symbol a").first { it.text() == memberName }.attr("href")
            sourceSet to href
        }
    }
}
