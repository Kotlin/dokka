## Purpose
This is a helper script for Dokka development and testing.
If it does not work for you, you can always do the same steps manually
as described in [CONTRIBUTING.md](../CONTRIBUTING.md#usetest-locally-built-dokka).

`.testDokka.sh` makes Dokka development and testing a bit faster

It compiles current Dokka version from the source code, \
publishes it to the local Maven repository, \
then runs the Dokka documentation generation against the specified project \
and finally runs a webserver to open the generated documentation in the browser.

## Usage

`cd scripts` and then

### Without parameters 
By default it applied to the `./examples/gradle/dokka-gradle-example` project

```bash
./testDokka.sh
```

### Specify test project path

```bash
./testDokka.sh -d ./examples/gradle/dokka-gradle-example
```

### Specify Dokka version

```bash
./testDokka.sh -v 1.9.20-my-fix-SNAPSHOT
```

### Specify port

```bash
./testDokka.sh  -p 8001
```

### All together

```bash
./testDokka.sh -d ./examples/gradle/dokka-gradle-example -v 1.9.20-my-fix-SNAPSHOT -p 8001
```

### Apply to a multi-module project

```bash
./testDokka.sh -m
```


## Requirements
To run the server you need to have Python 3 installed.

## Troubleshooting

* Make sure thar the path to the test project specified relative to the root of the Dokka project

* If occurs `Could not resolve all files for configuration ':dokkaHtmlPlugin'` error,
    * then make sure that `mavenLocal()` is added to the `repositories` section of the `build.gradle` file of the project you are testing against.
    It is not automated and should be done manually.

* if occurs `Failed to write org.jetbrains.dokka.base.renderers.FileWriter@628cef4a. Parent job is Cancelling`
  * then try to change the dokka version specified by `-v` parameter (e.g. `-v 1.9.20-my-fix1-SNAPSHOT`)
