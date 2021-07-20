# Using the Maven plugin

!!! note 
    Dokka Maven plugin does not support multi-platform projects.

The Maven plugin does not support multi-platform projects.

Minimal Maven configuration is

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    <version>${dokka.version}</version>
    <executions>
        <execution>
            <phase>pre-site</phase>
            <goals>
                <goal>dokka</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

By default files will be generated in `target/dokka`.

The following goals are provided by the plugin:

  * `dokka:dokka` - generate HTML documentation in Dokka format (showing declarations in Kotlin syntax)
  * `dokka:javadoc` - generate HTML documentation in Javadoc format (showing declarations in Java syntax)
  * `dokka:javadocJar` - generate a .jar file with Javadoc format documentation

## Configuration options

The available configuration options are shown below:

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    <version>${dokka.version}</version>
    <executions>
        <execution>
            <phase>pre-site</phase>
            <goals>
                <goal>dokka</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
    
        <!-- Set to true to skip dokka task, default: false -->
        <skip>false</skip>
    
        <!-- Default: ${project.artifactId} -->
        <moduleName>data</moduleName>

        <!-- Default: ${project.basedir}/target/dokka -->
        <outputDir>some/out/dir</outputDir>
        
        <!-- Use default or set to custom path to cache directory to enable package-list caching. -->
        <!-- When set to default, caches stored in $USER_HOME/.cache/dokka -->
        <cacheRoot>default</cacheRoot>

        <!-- Set to true to to prevent resolving package-lists online. -->
        <!-- When this option is set to true, only local files are resolved, default: false -->
        <offlineMode>false</offlineMode>

        <!-- List of '.md' files with package and module docs -->
        <!-- https://kotlinlang.org/docs/reference/kotlin-doc.html#module-and-package-documentation -->
        <includes>
            <include>packages.md</include>
            <include>extra.md</include>
        </includes>
        
        <!-- List of sample roots -->
        <samples>
            <dir>src/test/samples</dir>
        </samples>

        <!-- Suppress obvious functions like default toString or equals. Defaults to true -->
        <suppressObviousFunctions>false</suppressObviousFunctions>

        <!-- Suppress all inherited members that were not overriden in a given class. -->
        <!-- Eg. using it you can suppress toString or equals functions but you can't suppress componentN or copy on data class. To do that use with suppressObviousFunctions -->
        <!-- Defaults to false -->
        <suppressInheritedMembers>true</suppressInheritedMembers>

        <!-- Used for linking to JDK, default: 6 -->
        <jdkVersion>6</jdkVersion>

        <!-- Do not output deprecated members, applies globally, can be overridden by packageOptions -->
        <skipDeprecated>false</skipDeprecated> 
        <!-- Emit warnings about not documented members, applies globally, also can be overridden by packageOptions -->
        <reportUndocumented>true</reportUndocumented>             
        <!-- Do not create index pages for empty packages -->
        <skipEmptyPackages>true</skipEmptyPackages> 
        
        <!-- Short form list of sourceRoots, by default, set to ${project.compileSourceRoots} -->
        <sourceDirectories>
            <dir>src/main/kotlin</dir>
        </sourceDirectories>
        
        <!-- Full form list of sourceRoots -->
        <sourceRoots>
            <root>
                <path>src/main/kotlin</path>
                <!-- See platforms section of documentation -->
                <platforms>JVM</platforms>
            </root>
        </sourceRoots>
        
        <!-- Specifies the location of the project source code on the Web. If provided, Dokka generates "source" links
             for each declaration. -->
        <sourceLinks>
            <link>
                <!-- Source directory -->
                <path>${project.basedir}/src/main/kotlin</path>
                <!-- URL showing where the source code can be accessed through the web browser -->
                <url>https://github.com/cy6erGn0m/vertx3-lang-kotlin/blob/master/src/main/kotlin</url> <!-- //remove src/main/kotlin if you use "./" above -->
                <!--Suffix which is used to append the line number to the URL. Use #L for GitHub -->
                <lineSuffix>#L</lineSuffix>
            </link>
        </sourceLinks>
        
        <!-- Disable linking to online kotlin-stdlib documentation  -->
        <noStdlibLink>false</noStdlibLink>
        
        <!-- Disable linking to online JDK documentation -->
        <noJdkLink>false</noJdkLink>
        
        <!-- Allows linking to documentation of the project's dependencies (generated with Javadoc or Dokka) -->
        <externalDocumentationLinks>
            <link>
                <!-- Root URL of the generated documentation to link with. The trailing slash is required! -->
                <url>https://example.com/docs/</url>
                <!-- If package-list file located in non-standard location -->
                <!-- <packageListUrl>file:///home/user/localdocs/package-list</packageListUrl> -->
            </link>
        </externalDocumentationLinks>

        <!-- Allows to customize documentation generation options on a per-package basis -->
        <perPackageOptions>
            <packageOptions>
                <!-- Will match kotlin and all sub-packages of it -->
                <matchingRegex>kotlin($|\.).*</matchingRegex>
                
                <!-- All options are optional, default values are below: -->
                <skipDeprecated>false</skipDeprecated>
                <!-- Emit warnings about not documented members  -->
                <reportUndocumented>true</reportUndocumented>
                <includeNonPublic>false</includeNonPublic>
            </packageOptions>
        </perPackageOptions>
        
        <!-- Allows to use any dokka plugin, eg. GFM format   -->
        <dokkaPlugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>gfm-plugin</artifactId>
                <version>${dokka.version}</version>
            </plugin>
        </dokkaPlugins>
        
        <!-- Configures a plugin separately -->
        <pluginsConfiguration>
            <fullyQualifiedPluginName>
                <!-- Configuration -->
            </fullyQualifiedPluginName>
        </pluginsConfiguration>

    </configuration>
</plugin>
```

## Applying plugins
You can add plugins inside the `dokkaPlugins` block:

```xml
<plugin>
    <groupId>org.jetbrains.dokka</groupId>
    <artifactId>dokka-maven-plugin</artifactId>
    <version>${dokka.version}</version>
    <executions>
        <execution>
            <phase>pre-site</phase>
            <goals>
                <goal>dokka</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <dokkaPlugins>
            <plugin>
                <groupId>org.jetbrains.dokka</groupId>
                <artifactId>kotlin-as-java-plugin</artifactId>
                <version>${dokka.version}</version>
            </plugin>
        </dokkaPlugins>
    </configuration>
</plugin>
```

Some plugins can be configured separately using plugin's fully qualified name. For example:

```xml
<pluginsConfiguration>
    <org.jetbrains.dokka.base.DokkaBase>
        <customStyleSheets>
            <customStyleSheet><!-- path to custom stylesheet --></customStyleSheet>
        </customStyleSheets>
        <customAssets>
            <customAsset><!-- path to custom asset --></customAsset>
        </customAssets>
    </org.jetbrains.dokka.base.DokkaBase>
</pluginsConfiguration>
```

## Example project

Please see the [Dokka Maven example project](https://github.com/Kotlin/dokka/tree/master/examples/maven) for an example.
