package renderers.gfm

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.gfm.GfmPlugin
import org.jetbrains.dokka.gfm.MarkdownLocationProviderFactory
import org.jetbrains.dokka.testApi.context.MockContext
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.external.DokkaExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.JavadocExternalLocationProviderFactory
import renderers.RenderingOnlyTestBase
import utils.TestOutputWriter

abstract class GfmRenderingOnlyTestBase : RenderingOnlyTestBase() {

    val files = TestOutputWriter()
    override val context = MockContext(
        DokkaBase().outputWriter to { _ -> files },
        DokkaBase().locationProviderFactory to ::MarkdownLocationProviderFactory,
        DokkaBase().externalLocationProviderFactory to { ::JavadocExternalLocationProviderFactory },
        DokkaBase().externalLocationProviderFactory to { ::DokkaExternalLocationProviderFactory },
        GfmPlugin().gfmPreprocessors to { _ -> RootCreator },

        testConfiguration = DokkaConfigurationImpl(
            "", "", null, false, emptyList(), emptyList(), emptyMap(), emptyList(), false
        )
    )

    protected val renderedContent: String by lazy {
        files.contents.getValue("test-page.md")
    }
}