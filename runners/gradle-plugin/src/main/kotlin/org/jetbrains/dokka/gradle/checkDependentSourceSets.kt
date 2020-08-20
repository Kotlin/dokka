package org.jetbrains.dokka.gradle

import org.jetbrains.dokka.DokkaSourceSetID

internal fun checkSourceSetDependencies(sourceSets: List<GradleDokkaSourceSetBuilder>) {
    checkSourceSetDependencies(sourceSets.associateBy { it.sourceSetID })
}

private fun checkSourceSetDependencies(sourceSets: Map<DokkaSourceSetID, GradleDokkaSourceSetBuilder>) {
    sourceSets.values.forEach { sourceSet ->
        sourceSet.dependentSourceSets.getSafe().forEach { dependentSourceSetID ->
            val dependentSourceSet = requireNotNull(sourceSets[dependentSourceSetID]) {
                "Dokka source set \"${sourceSet.name}\": Cannot find dependent source set \"$dependentSourceSetID\""
            }

            if (sourceSet.suppress.getSafe().not() && dependentSourceSet.suppress.getSafe()) {
                throw IllegalArgumentException(
                    "Dokka source set: \"${sourceSet.name}\": " +
                            "Unsuppressed source set cannot depend on suppressed source set \"$dependentSourceSetID\""
                )
            }
        }
    }
}
