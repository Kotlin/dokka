package org.jetbrains.conventions

import org.jetbrains.configureDokkaVersion

plugins {
  base
}

description = "common Gradle configuration that should be applied to all projets"

if (project != rootProject) {
  project.group = rootProject.group
  project.version = rootProject.version
}
