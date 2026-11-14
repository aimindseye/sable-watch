#!/usr/bin/env bash
# Checks watchface/src/main/res/raw/watchface.xml against Google's Watch Face Format v1 schema
# (github.com/google/watchface, pinned commit). The schema uses XSD 1.1, so it runs Xerces-J
# 2.12.2 with XSD 1.1 support from Maven Central, checked against pinned SHA-256 sums.
# This is the schema check only; Google's own wff-validator adds expression checks on top.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WFF_COMMIT="${WFF_COMMIT:-d1cdafa953c95d5554fc54df4bc3948befbefc4d}"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

m=https://repo1.maven.org/maven2
fetch() {
    local url="$1" name="$2" sha="$3"
    curl --proto '=https' --tlsv1.2 -sSfL -o "$work/$name" "$url"
    echo "$sha  $work/$name" | sha256sum -c --quiet -
}
fetch "$m/org/exist-db/thirdparty/xerces/xercesImpl/2.12.2/xercesImpl-2.12.2-xml-schema-1.1.jar" xerces.jar \
    "368776ba019267ee0b3c78a729441e5f808fee3286ddd6968303d25d18139cde"
fetch "$m/org/exist-db/thirdparty/org/eclipse/wst/xml/xpath2/1.2.0/xpath2-1.2.0.jar" xpath2.jar "fc07fe50b9e0a0bf25772dba606f7d2c22b3f2285fd2a1408bbb33356c521d52"
fetch "$m/edu/princeton/cup/java-cup/10k/java-cup-10k.jar" cup.jar "15894fad0a81611e351b5200bbc3bd21359fc6aed53af54a48998390e4b2700d"
fetch "$m/xml-apis/xml-apis/1.4.01/xml-apis-1.4.01.jar" xml-apis.jar "a840968176645684bb01aed376e067ab39614885f9eee44abe35a5f20ebe7fad"

git clone -q --filter=blob:none --sparse https://github.com/google/watchface.git "$work/wff"
git -C "$work/wff" checkout -q "$WFF_COMMIT"
git -C "$work/wff" sparse-checkout set third_party/wff/specification/documents/1

cp "$ROOT/tools/src/sablewatch/tools/WffSchemaCheck.java" "$work/"
cp="$work/xerces.jar:$work/xpath2.jar:$work/cup.jar:$work/xml-apis.jar"
javac -d "$work" -cp "$cp" "$work/WffSchemaCheck.java"
java -cp "$cp:$work" sablewatch.tools.WffSchemaCheck \
    "$work/wff/third_party/wff/specification/documents/1/watchface.xsd" \
    "$ROOT/watchface/src/main/res/raw/watchface.xml"
