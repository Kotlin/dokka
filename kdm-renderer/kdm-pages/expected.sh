#!/usr/bin/env bash
# Rebuilds expected.txt — the browsable URLs of the reference output.
#
# One line per content page, in the form a browser shows:
#   index.html  ->  /<dir>/   (the site root is "/")
#   other       ->  /<path without .html>
#   path segments percent-encoded, so `[jvm]shared.html` is `/…/%5Bjvm%5Dshared`
#
# Excluded: older/ (archived version snapshots, P-15), the navigation.html payloads
# (P-13) and not-found-version.html (P-14) — none of them is a content page.
#
# Usage: ./expected.sh [path to the generated site]
set -euo pipefail

root="${1:-../../dokka-integration-tests/gradle/build/ui-showcase-result}"
out="$(cd "$(dirname "$0")" && pwd)/expected.txt"

cd "$root"
python3 - "$out" <<'PY'
import os, sys, urllib.parse

skip = {"navigation.html", "not-found-version.html"}
urls = []
for dirpath, dirnames, filenames in os.walk("."):
    if "older" in os.path.relpath(dirpath, ".").split(os.sep):
        continue
    for name in filenames:
        if not name.endswith(".html") or name in skip:
            continue
        path = os.path.relpath(os.path.join(dirpath, name), ".")
        if name == "index.html":
            directory = os.path.dirname(path)
            url = f"/{directory}/" if directory else "/"
        else:
            url = "/" + path[: -len(".html")]
        urls.append("/".join(urllib.parse.quote(part) for part in url.split("/")))

with open(sys.argv[1], "w") as f:
    f.write("\n".join(sorted(urls)) + "\n")
print(f"{len(urls)} urls -> {sys.argv[1]}")
PY
