# Inventory: configuration surface

Everything a user can set that affects the HTML output. Finite and enumerable — the
easiest inventory, and the one that gives the least coverage on its own.

Sources:
- `dokka-subprojects/core/src/main/kotlin/org/jetbrains/dokka/configuration.kt`
- `dokka-subprojects/plugin-base/src/main/kotlin/org/jetbrains/dokka/base/DokkaBaseConfiguration.kt`
- per-plugin `ConfigurableBlock` implementations
- the runner-facing DSLs in `dokka-runners/` (Gradle new + classic, Maven, CLI) —
  **not yet surveyed**, and they are what users actually see. TODO.

For each option the question is the same: *is it still meaningful when the renderer
reads KDM, and if so, does it act on the model or on the rendering?*

## Global (`DokkaConfiguration`)

| Option | Affects | Model or renderer? | Registry ID |
|---|---|---|---|
| `moduleName`, `moduleVersion` | Titles, version label | renderer | |
| `outputDir` | — | renderer | |
| `cacheRoot`, `offlineMode` | External link resolution | TODO | F-062 |
| `failOnWarning` | Build behaviour | TODO | F-008 |
| `sourceSets` | Fragments | model | F-080 |
| `modules` (`DokkaModuleDescription`) | Multi-module assembly | TODO | F-120 |
| `pluginsClasspath`, `pluginsConfiguration` | Extensibility | ep | Q-004 |
| `delayTemplateSubstitution` | Multi-module templating | renderer | F-121 |
| `suppressObviousFunctions` | Declaration set | model | F-005 |
| `suppressInheritedMembers` | Declaration set | model | F-006 |
| `includes` | Module docs | model | F-044 |
| `finalizeCoroutines` | Runtime detail | — | |

## Per source set (`DokkaSourceSet`)

| Option | Affects | Model or renderer? | Registry ID |
|---|---|---|---|
| `sourceSetID`, `displayName` | Fragment identity and label | model | F-080 |
| `classpath`, `sourceRoots`, `dependentSourceSets` | Analysis input | model | |
| `samples` | `@sample` resolution | model | F-043 |
| `includes` | Package docs | model | F-044 |
| `includeNonPublic` *(deprecated)* / `documentedVisibilities` | Declaration set | model | F-001 |
| `reportUndocumented` | Diagnostics | — | F-008 |
| `skipEmptyPackages` | Declaration set | renderer | F-007 |
| `skipDeprecated` | Declaration set | model | F-002 |
| `jdkVersion`, `noJdkLink` | External links to the JDK | model | F-062 |
| `sourceLinks` (`localDirectory`, `remoteUrl`, `remoteLineSuffix`) | Source links | model | F-064 |
| `perPackageOptions` (`matchingRegex`, `suppress`, visibility, `skipDeprecated`, `reportUndocumented`) | Per-package overrides | model | F-004 |
| `externalDocumentationLinks`, `noStdlibLink` | Cross-project links | model | F-062 |
| `languageVersion`, `apiVersion` | Analysis | model | |
| `suppressedFiles` | Declaration set | model | F-004 |
| `analysisPlatform` | Platform | model | |

## `DokkaBaseConfiguration` (HTML)

| Option | Default | Affects | Registry ID |
|---|---|---|---|
| `customStyleSheets` | `[]` | Styling | F-105 |
| `customAssets` | `[]` | Styling | F-105 |
| `separateInheritedMembers` | `false` | Inherited members layout | F-006, F-022 |
| `footerMessage` | `© <year> Copyright` | Chrome | F-106 |
| `mergeImplicitExpectActualDeclarations` | `false` | MPP merging | F-082 |
| `templatesDir` | `null` | FreeMarker templates | F-107 |
| `homepageLink` | `null` | Chrome | F-106 |

Note: `separateInheritedMembers` and `mergeImplicitExpectActualDeclarations` are
*not* purely presentational despite living in the HTML plugin's config — both change
what the model layer must supply.

## `VersioningConfiguration`

`olderVersionsDir`, `olderVersions`, `versionsOrdering`, `version`,
`renderVersionsNavigationOnAllPages`, `olderVersionsDirName` → F-122. In scope only
if Q-003 is answered yes.

## Other plugins

TODO: `plugin-mathjax`, `plugin-kotlin-playground-samples`,
`plugin-android-documentation`, `plugin-javadoc`, `plugin-gfm`, `plugin-jekyll`.

## Open

- The runner DSLs may expose options that do not map 1:1 to the above, and may
  document defaults that differ. Survey needed before this file is trustworthy.
- Which options should be *retired* rather than reimplemented? Each one kept is a
  permanent constraint on the new renderer.
