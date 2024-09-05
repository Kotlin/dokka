/**
 * Common conventions for generating documentation with Dokka.
 */

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    // Important! Ensure that each project has a distinct module path.
    // See the example README for more information.
    modulePath = rootProject.name + project.path.replace(":", "/")
}
