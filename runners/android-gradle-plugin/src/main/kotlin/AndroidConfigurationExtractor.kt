package org.jetbrains.dokka.gradle

import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import com.android.builder.core.BuilderConstants
import org.gradle.api.Project

class AndroidConfigurationExtractor(private val project: Project): AbstractConfigurationExtractor(project) {
     override fun getMainCompilationName(): String = getVariants(project).filter { it.name == BuilderConstants.RELEASE }.map { it.name }.first()

     private fun getVariants(project: Project): Set<BaseVariant> {
          val androidExtension = project.extensions.getByName("android")
          return when (androidExtension) {
               is AppExtension -> androidExtension.applicationVariants.toSet()
               is LibraryExtension -> {
                    androidExtension.libraryVariants.toSet() +
                            if (androidExtension is FeatureExtension) {
                                 androidExtension.featureVariants.toSet()
                            } else {
                                 emptySet<BaseVariant>()
                            }
               }
               is TestExtension -> androidExtension.applicationVariants.toSet()
               else -> emptySet()
          } +
                  if (androidExtension is TestedExtension) {
                       androidExtension.testVariants.toSet() + androidExtension.unitTestVariants.toSet()
                  } else {
                       emptySet<BaseVariant>()
                  }
     }
}