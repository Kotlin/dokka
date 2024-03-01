import java.net.URL

plugins {
    id("uitest.dokka")

    kotlin("jvm")
}

tasks.dokkaHtmlPartial {
    dokkaSourceSets.configureEach {
        includes.setFrom("description.md")

        suppressObviousFunctions.set(false)
        suppressInheritedMembers.set(false)
        skipEmptyPackages.set(false)

        sourceLink {
            localDirectory.set(projectDir.resolve("src"))
            remoteUrl.set(URL("https://github.com/kotlin/dokka/dokka-integration-tests/ui/test-project/jvm/src"))
            remoteLineSuffix.set("#L")
        }
    }
}
