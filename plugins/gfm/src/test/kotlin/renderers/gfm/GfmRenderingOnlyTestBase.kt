package renderers.gfm

import org.jetbrains.dokka.DokkaConfigurationImpl
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.RootCreator
import org.jetbrains.dokka.base.resolvers.external.DefaultExternalLocationProviderFactory
import org.jetbrains.dokka.base.resolvers.external.javadoc.JavadocExternalLocationProviderFactory
import org.jetbrains.dokka.gfm.GfmPlugin
import org.jetbrains.dokka.gfm.location.MarkdownLocationProvider
import org.jetbrains.dokka.testApi.context.MockContext
import renderers.RenderingOnlyTestBase
import utils.TestOutputWriter

abstract class GfmRenderingOnlyTestBase : RenderingOnlyTestBase<String>() {

    val files = TestOutputWriter()
    override val context = MockContext(
        DokkaBase().outputWriter to { files },
        DokkaBase().locationProviderFactory to MarkdownLocationProvider::Factory,
        DokkaBase().externalLocationProviderFactory to ::JavadocExternalLocationProviderFactory,
        DokkaBase().externalLocationProviderFactory to ::DefaultExternalLocationProviderFactory,
        GfmPlugin().gfmPreprocessors to { RootCreator },

        testConfiguration = DokkaConfigurationImpl(moduleName = "root")
    )

    override val renderedContent: String by lazy {
        files.contents.getValue("test-page.md")
    }
}
