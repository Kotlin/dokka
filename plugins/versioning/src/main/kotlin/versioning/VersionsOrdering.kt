package versioning

import org.apache.maven.artifact.versioning.ComparableVersion
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import org.jetbrains.dokka.versioning.VersionId
import org.jetbrains.dokka.versioning.VersioningConfiguration
import org.jetbrains.dokka.versioning.VersioningPlugin

fun interface VersionsOrdering {
    fun order(records: List<VersionId>): List<VersionId>
}

class ByConfigurationVersionOrdering(val dokkaContext: DokkaContext) : VersionsOrdering {
    override fun order(records: List<VersionId>): List<VersionId> =
        configuration<VersioningPlugin, VersioningConfiguration>(dokkaContext)?.versionsOrdering
            ?: throw IllegalStateException("Attempted to use a configuration ordering without providing configuration")
}

class SemVerVersionOrdering : VersionsOrdering {
    override fun order(records: List<VersionId>): List<VersionId> =
        records.sortedWith { lhs, rhs ->
            -1*ComparableVersion(lhs).compareTo(ComparableVersion(rhs))
        }
}