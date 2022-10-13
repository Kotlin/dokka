package org.jetbrains.dokka.gradle

// Backwards compatibility for moved files

@Deprecated(
    "Moved to org.jetbrains.dokka.gradle.tasks.AbstractDokkaLeafTask",
    ReplaceWith("org.jetbrains.dokka.gradle.tasks.AbstractDokkaLeafTask")
)
abstract class AbstractDokkaLeafTask : org.jetbrains.dokka.gradle.tasks.AbstractDokkaLeafTask()

@Deprecated(
    "Moved to org.jetbrains.dokka.gradle.tasks.AbstractDokkaParentTask",
    ReplaceWith("org.jetbrains.dokka.gradle.tasks.AbstractDokkaParentTask")
)
abstract class AbstractDokkaParentTask : org.jetbrains.dokka.gradle.tasks.AbstractDokkaParentTask()

@Deprecated(
    "Moved to org.jetbrains.dokka.gradle.tasks.AbstractDokkaTask",
    ReplaceWith("org.jetbrains.dokka.gradle.tasks.AbstractDokkaTask")
)
abstract class AbstractDokkaTask : org.jetbrains.dokka.gradle.tasks.AbstractDokkaTask()

@Deprecated(
    "Moved to org.jetbrains.dokka.gradle.tasks.DokkaCollectorTask",
    ReplaceWith("org.jetbrains.dokka.gradle.tasks.DokkaCollectorTask")
)
abstract class DokkaCollectorTask : org.jetbrains.dokka.gradle.tasks.DokkaCollectorTask()

@Deprecated(
    "Moved to org.jetbrains.dokka.gradle.tasks.DokkaMultiModuleTask",
    ReplaceWith("org.jetbrains.dokka.gradle.tasks.DokkaMultiModuleTask")
)
abstract class DokkaMultiModuleTask : org.jetbrains.dokka.gradle.tasks.DokkaMultiModuleTask()

@Deprecated(
    "Moved to org.jetbrains.dokka.gradle.tasks.DokkaTask",
    ReplaceWith("org.jetbrains.dokka.gradle.tasks.DokkaTask")
)
abstract class DokkaTask : org.jetbrains.dokka.gradle.tasks.DokkaTask()

@Deprecated(
    "Moved to org.jetbrains.dokka.gradle.tasks.DokkaTaskPartial",
    ReplaceWith("org.jetbrains.dokka.gradle.tasks.DokkaTaskPartial")
)
abstract class DokkaTaskPartial : org.jetbrains.dokka.gradle.tasks.DokkaTaskPartial()
