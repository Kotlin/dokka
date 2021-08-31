package versioning

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.templating.InsertTemplateExtra
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.transformers.pages.PageTransformer
import org.jetbrains.dokka.versioning.ReplaceVersionsCommand
import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin

class VersionsNavigationAdder(val context: DokkaContext) : PageTransformer {
    private val configuration = configuration<VersioningPlugin, VersioningConfiguration>(context)

    override fun invoke(input: RootPageNode): RootPageNode {
        val locationProvider =
            context.plugin<DokkaBase>().querySingle { locationProviderFactory }.getLocationProvider(input)

        var isFirst = true
        var cmd = ReplaceVersionsCommand()
        return input.transformContentPagesTree {
            if (isFirst || configuration?.onEachPage != false) {
                isFirst = false
                cmd = if (context.configuration.delayTemplateSubstitution)
                    cmd
                else ReplaceVersionsCommand(location = locationProvider.resolve(it) ?: "")

                addOnPageCmd(it, cmd)
            } else {
                it
            }
        }
    }

    private fun addOnPageCmd(contentPage: ContentPage, Cmd: ReplaceVersionsCommand) =
        contentPage.modified(content = contentPage.content.let {
            when (it) {
                is ContentGroup -> it.copy(
                    listOf(
                        ContentGroup(
                            children = emptyList(),
                            dci = DCI(contentPage.dri, ContentKind.Main),
                            sourceSets = contentPage.sourceSets(),
                            style = emptySet(),
                            extra = PropertyContainer.withAll(InsertTemplateExtra(Cmd))
                        )
                    ) + it.children
                )
                else -> it
            }
        })
}