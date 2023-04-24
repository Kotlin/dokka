import com.github.jengelman.gradle.plugins.shadow.transformers.DontIncludeResourceTransformer
import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
    id("com.github.johnrengelman.shadow")
}

repositories {
    // Override the shared repositories defined in the root settings.gradle.kts
    // These repositories are very specific and are not needed in other projects
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/snapshots") {
        mavenContent { snapshotsOnly() }
    }
    maven("https://www.jetbrains.com/intellij-repository/releases")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    maven("https://www.myget.org/F/rd-snapshots/maven/")
}

val intellijCore: Configuration by configurations.creating

fun intellijCoreAnalysis() = zipTree(intellijCore.singleFile).matching {
    include("intellij-core.jar")
}

val jpsStandalone: Configuration by configurations.creating

fun jpsModel() = zipTree(jpsStandalone.singleFile).matching {
    include("jps-model.jar")
    include("aalto-xml-*.jar")
}


//val javaImplConfiguration: Configuration by configurations.creating
//fun javaImpl() = zipTree(javaImplConfiguration.singleFile)

dependencies {
    api(libs.kotlinPlugin.common)
    api(libs.kotlinPlugin.idea) {
        isTransitive = false
    }
    api(libs.kotlinPlugin.core)
    api(libs.kotlinPlugin.native)

    @Suppress("UnstableApiUsage")
    intellijCore(libs.jetbrainsIntelliJ.core)
    implementation(intellijCoreAnalysis())

    @Suppress("UnstableApiUsage")
    jpsStandalone(libs.jetbrainsIntelliJ.jpsStandalone)
    implementation(jpsModel())

    /*implementation("com.jetbrains.intellij.java:java-impl:223.8836.41") {
        isTransitive = false
    }*/
   // implementation(javaImpl())
   // implementation("org.jetbrains.kotlin:kotlin-compiler:1.8.20-release-327/")
    implementation("org.jetbrains.kotlin:high-level-api-for-ide:1.8.20-release-327") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:high-level-api-impl-base-for-ide:1.8.20-release-327") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:high-level-api-fir-for-ide:1.8.20-release-327") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:high-level-api-fe10-for-ide:1.8.20-release-327") {
        isTransitive = false // see KTIJ-19820
    }

    implementation("org.jetbrains.kotlin:low-level-api-fir-for-ide:1.8.20-release-327") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:analysis-project-structure-for-ide:1.8.20-release-327") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:analysis-api-standalone-for-ide:1.8.20-release-327") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:analysis-api-providers-for-ide:1.8.20-release-327") {
        isTransitive = false // see KTIJ-19820
    }
    implementation("org.jetbrains.kotlin:symbol-light-classes-for-ide:1.8.20-release-327") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.4")
    implementation("com.jetbrains.intellij.platform:core-impl:203.8084.24")
    //implementation("com.jetbrains.intellij.platform:java-impl:203.8084.24")
}

tasks {
    shadowJar {


        val dokka_version: String by project
        archiveFileName.set("dokka-kotlin-analysis-intellij-$dokka_version.jar")

      //      transform(DontIncludeResourceTransformer::class.java)  {
       //         resource = "/org/jetbrains/kotlin/utils/PathUtil.class"
       //     }
            transform(DontIncludeResourceTransformer::class.java)  {
                resource = "/org/jetbrains/kotlin/resolve/CompilerDeserializationConfiguration.class"
            }
        transform(DontIncludeResourceTransformer::class.java)  {
                resource = "org/jetbrains/kotlin/resolve/CompilerDeserializationConfiguration.class"
            }

      //  exclude("org/jetbrains/kotlin/*")
       // include("org/jetbrains/kotlin/analysis/**")

            exclude("org/jetbrains/kotlin/resolve/CompilerDeserializationConfiguration.class")
            exclude("/org/jetbrains/kotlin/resolve/CompilerDeserializationConfiguration.class")
            exclude("**/CompilerDeserializationConfiguration.class")



            exclude("org/jetbrains/kotlin/util/TypeRegistry.class")

            exclude("org/jetbrains/kotlin/name/StandardClassIds.class")
            exclude("/org/jetbrains/kotlin/name/StandardClassIds.class")

    //    exclude("org/jetbrains/kotlin/psi/stubs/**")
      //  exclude("/org/jetbrains/kotlin/psi/stubs/**")


            exclude("org/jetbrains/kotlin/util/**")

           exclude("org/jetbrains/kotlin/analysis/decompiler/psi/BuiltInDefinitionFile*")

            exclude("org/jetbrains/kotlin/metadata/builtins/BuiltInsBinaryVersion.*")
            exclude("org/jetbrains/kotlin/metadata/builtins/readPackageFragment*")
            exclude("org/jetbrains/kotlin/metadata/builtins/ReadPackageFragment*")
            exclude("org/jetbrains/kotlin/builtins/StandardNames*")
            exclude("org/jetbrains/kotlin/psi/KtNamedFunction*")
            exclude("org/jetbrains/kotlin/asJava/classes/KotlinClassInnerStuffCache*")
            exclude("org/jetbrains/kotlin/serialization/deserialization/builtins/BuiltInsPackageFragmentImpl*")
            exclude("org/jetbrains/kotlin/config/LanguageVersion*")

          //  exclude("/org/jetbrains/kotlin/metadata/builtins/**")


            exclude("org/jetbrains/kotlin/utils/PathUtil.class")
            exclude("/org/jetbrains/kotlin/utils/PathUtil.class")
            exclude("**/PathUtil.class")
            exclude("**/PathUtil.*")
            exclude("colorScheme/**")
            exclude("fileTemplates/**")
            exclude("inspectionDescriptions/**")
            exclude("intentionDescriptions/**")
            exclude("tips/**")
            exclude("messages/**")
            exclude("src/**")
            exclude("**/*.kotlin_metadata")
            exclude("**/*.kotlin_builtins")



        archiveClassifier.set("all")
    }
}

registerDokkaArtifactPublication("kotlinAnalysisIntelliJ") {
artifactId = "kotlin-analysis-intellij"
component = Shadow
}
