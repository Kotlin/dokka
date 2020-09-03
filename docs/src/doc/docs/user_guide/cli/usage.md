# Using command line

To run Dokka from the command line, download the [Dokka CLI runner](https://github.com/Kotlin/dokka/releases/download/v1.4.0/dokka-cli-1.4.0.jar).
To generate documentation, run the following command:
```
java -jar dokka-cli.jar <arguments>
```

## Configuration options

Dokka supports the following command line arguments:

  * `-outputDir` - the output directory where the documentation is generated
  * `-moduleName` - (required) - module name used as a part of source set ID when declaring dependent source sets
  * `-cacheRoot` - cache directory to enable package-list caching
  * `-pluginsClasspath` - artifacts with Dokka plugins, separated by `;`. At least `dokka-base` and all its dependencies must be added there 
  * `-offlineMode` - do not resolve package-lists online
  * `-failOnWarning` - throw an exception instead of a warning
  * `-globalPackageOptions` - per package options added to all source sets
  * `-globalLinks` - external documentation links added to all source sets
  * `-globalSrcLink` - source links added to all source sets
  * `-sourceSet` - (repeatable) - configuration for a single source set. Following this argument, you can pass other arguments:
    * `-sourceSetName` - source set name as a part of source set ID when declaring dependent source sets
    * `-displayName` - source set name displayed in the generated documentation
    * `-src` - list of source files or directories separated by `;`
    * `-classpath` - list of directories or .jar files to include in the classpath (used for resolving references) separated by `;`
    * `-samples` - list of directories containing sample code (documentation for those directories is not generated but declarations from them can be referenced using the `@sample` tag) separated by `;`
    * `-includes` - list of files containing the documentation for the module and individual packages separated by `;`
    * `-includeNonPublic` - include protected and private code   
    * `-skipDeprecated` - if set, deprecated elements are not included in the generated documentation
    * `-reportUndocumented` - warn about undocumented members
    * `-skipEmptyPackages` - do not create index pages for empty packages
    * `-packageOptions` - list of package options in format `prefix,-deprecated,-privateApi,+reportUndocumented;prefix, ...`, separated by `;`
    * `-links` - list of external documentation links in format `url^packageListUrl^^url2...`, separated by `;`
    * `-srcLink` - mapping between a source directory and a Web site for browsing the code in format `<path>=<url>[#lineSuffix]`
    * `-noStdlibLink` - disable linking to online kotlin-stdlib documentation
    * `-noJdkLink` - disable linking to online JDK documentation
    * `-jdkVersion` - version of JDK to use for linking to JDK JavaDoc
    * `-analysisPlatform` - platform used for analysis, see the [Platforms](#platforms) section
    * `-dependentSourceSets` - list of dependent source sets in format `moduleName/sourceSetName`, separated by `;`

You can also use a JSON file with Dokka configuration:
 ```
 java -jar <dokka_cli.jar> <path_to_config.json>
 ```

## Applying plugins
To apply a Dokka plugin you have to provide it and all its dependencies in the `pluginsClasspath` parameter
