package org.jetbrains.conventions

import org.gradle.kotlin.dsl.`maven-publish`
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.ValidatePublications
import org.jetbrains.publicationChannels

plugins {
  id("org.jetbrains.conventions.base")
  `maven-publish`
  signing
}
