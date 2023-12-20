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
./testDokka.sh -d './examples/gradle/dokka-gradle-example'
```

### Specify Dokka version

```bash
./testDokka.sh -v "1.9.20-SNAPSHOT"
```

### Specify port

```bash
./testDokka.sh  -p 8001
```

### All together

```bash
./testDokka.sh -d './examples/gradle/dokka-gradle-example' -v "1.9.20-SNAPSHOT" -p 8001
```


