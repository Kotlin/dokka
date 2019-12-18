#!/bin/bash

patch='Index: build.gradle.kts
IDEA additional info:
Subsystem: com.intellij.openapi.diff.impl.patch.CharsetEP
<+>UTF-8
===================================================================
--- build.gradle.kts	(revision 58f85a4dce464cb5eea3d9dba4d8337c90ba3d9d)
+++ build.gradle.kts	(date 1576160110756)
@@ -3,7 +3,7 @@
 plugins {
     id("java")
     id("org.jetbrains.kotlin.multiplatform") version "1.3.50"
-    id("org.jetbrains.dokka") version "0.10.1-SNAPSHOT"
+    id("org.jetbrains.dokka") version "0.11.0-SNAPSHOT"
 }

 group = "org.jetbrains.dokka"
'

gradle publishToMavenLocal

cd example || exit
git apply - <<< patch
./gradlew --no-daemon dokka