## Purpose
`.testDokka.sh` makes Dokka development and testing a bit faster

It compiles current Dokka version from the source code, \
publishes it to the local Maven repository, \
then runs the Dokka documentation generation against the specified project \
and finally runs a webserver to open the generated documentation in the browser.

## Usage

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


## Requirements
To run the server you need to have Python 3 installed.

## Troubleshooting

* If occurs `Could not resolve all files for configuration ':dokkaHtmlPlugin'` error,
    * then make sure that `mavenLocal()` is added to the `repositories` section of the `build.gradle` file of the project you are testing against.
    It is not automated and should be done manually.
