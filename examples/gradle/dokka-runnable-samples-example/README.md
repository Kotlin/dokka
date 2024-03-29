# runnable samples example

Note: better to change the version every time there are some changes happened,
as Gradle caches something badly and so `main.js` could not change in final HTML distribution.

To reproduce (from the root of the project):

```
rm -rf dokka-subprojects/plugin-base-frontend/node_modules
./scripts/testDokka.sh -d examples/gradle/dokka-runnable-samples-example -v 2.0.0-samples-issue-SNAPSHOT
```

Everything works fine if to revert commit which changes `package-lock.json` (155efa42):

```
git revert 155efa425bad65f6171b9c39aa6c25ce3cf85a83 --no-edit --no-commit
rm -rf dokka-subprojects/plugin-base-frontend/node_modules
./scripts/testDokka.sh -d examples/gradle/dokka-runnable-samples-example -v 2.0.0-samples-issue-fixed-package-json-SNAPSHOT
```
