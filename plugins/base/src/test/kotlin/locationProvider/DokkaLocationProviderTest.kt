/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package locationProvider

import org.jetbrains.dokka.base.resolvers.local.DokkaLocationProvider
import org.jetbrains.dokka.base.testApi.testRunner.BaseAbstractTest
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import kotlin.test.Test
import kotlin.test.assertEquals

class DokkaLocationProviderTest : BaseAbstractTest() {

    private val configuration = dokkaConfiguration {
        sourceSets {
            sourceSet {
                sourceRoots = listOf("src/")
                classpath += jvmStdlibPath!!
            }
        }
    }

    private fun getTestLocationProvider(root: RootPageNode, context: DokkaContext? = null): DokkaLocationProvider {
        val dokkaContext = context ?: DokkaContext.create(configuration, logger, emptyList())
        return DokkaLocationProvider(root, dokkaContext, ".html")
    }

    @DslMarker
    annotation class TestNavigationDSL

    @TestNavigationDSL
    class NavigationDSL {
        companion object {
            private val stubDCI = DCI(
                setOf(
                    DRI("kotlin", "Any")
                ),
                ContentKind.Comment
            )
            val stubContentNode = ContentText("", stubDCI, emptySet())
        }

        operator fun invoke(name: String, fn: ModulesDsl.() -> Unit): RendererSpecificRootPage {
            val modules = ModulesDsl().also { it.fn() }
            return RendererSpecificRootPage(name = name, children = modules.pages, RenderingStrategy.DoNothing)
        }

        @TestNavigationDSL
        class ModulesDsl(val pages: MutableList<ModulePageNode> = mutableListOf()) {
            fun modulePage(name: String, fn: PackageDsl.() -> Unit) {
                val packages = PackageDsl().also { it.fn() }
                pages.add(
                    ModulePageNode(
                        name = name,
                        children = packages.pages,
                        content = stubContentNode
                    )
                )
            }
        }

        @TestNavigationDSL
        class PackageDsl(val pages: MutableList<PackagePageNode> = mutableListOf()) {
            fun packagePage(name: String, fn: ClassDsl.() -> Unit) {
                val packages = ClassDsl().also { it.fn() }
                pages.add(
                    PackagePageNode(
                        name = name,
                        children = packages.pages,
                        content = stubContentNode,
                        dri = emptySet()
                    )
                )
            }
        }

        @TestNavigationDSL
        class ClassDsl(val pages: MutableList<ClasslikePageNode> = mutableListOf()) {
            fun classPage(name: String) {
                pages.add(
                    ClasslikePageNode(
                        name = name,
                        children = emptyList(),
                        content = stubContentNode,
                        dri = emptySet()
                    )
                )
            }
        }
    }

    @Test
    fun `links to a package with or without a class`() {
        val root = NavigationDSL()("Root") {
            modulePage("Module") {
                packagePage("Package") {}
            }
        }
        val packagePage = root.children.first().children.first() as PackagePageNode
        val locationProvider = getTestLocationProvider(root)
        val resolvedLink = locationProvider.resolve(packagePage)
        val localToRoot = locationProvider.pathToRoot(packagePage)

        val rootWithClass = NavigationDSL()("Root") {
            modulePage("Module") {
                packagePage("Package") {
                    classPage("ClassA")
                }
            }
        }
        val packagePageWithClass = rootWithClass.children.first().children.first() as PackagePageNode

        val locationProviderWithClass = getTestLocationProvider(rootWithClass)
        val localToRootWithClass = locationProviderWithClass.pathToRoot(packagePageWithClass)
        val resolvedLinkWithClass = locationProviderWithClass.resolve(packagePageWithClass)

        assertEquals("-module/Package.html", resolvedLink)
        assertEquals("../", localToRoot)

        assertEquals("-module/Package/index.html", resolvedLinkWithClass)
        assertEquals("../../", localToRootWithClass)
    }
}
