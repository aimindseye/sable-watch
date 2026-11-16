#!/usr/bin/env bash
# Build, sign and package a Sable Watch release on the owner's computer (docs/RELEASING.md).
# CI never gets the key: it builds unsigned APKs only.
#
# The keystore path and key alias come from SABLEWATCH_KEYSTORE and SABLEWATCH_KEY_ALIAS. The
# password is asked for once at the prompt and handed to apksigner on stdin, so it is never
# written to a file, an environment variable or the command line.
# Runs in Git Bash on Windows, or any bash on macOS and Linux.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

: "${SABLEWATCH_KEYSTORE:?set SABLEWATCH_KEYSTORE to the keystore path (docs/RELEASING.md)}"
: "${SABLEWATCH_KEY_ALIAS:?set SABLEWATCH_KEY_ALIAS to the key alias (docs/RELEASING.md)}"
[[ -f "$SABLEWATCH_KEYSTORE" ]] || { echo "ERROR: no keystore at $SABLEWATCH_KEYSTORE" >&2; exit 2; }
if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "ERROR: commit tracked source changes before packaging a release." >&2
    exit 2
fi

sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
bt="$(find "$sdk/build-tools" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | LC_ALL=C sort | tail -n1)"
apksigner="$bt/apksigner"; [[ -e "$apksigner" ]] || apksigner="$apksigner.bat"
[[ -e "$apksigner" ]] || { echo "ERROR: apksigner not found under $sdk/build-tools" >&2; exit 2; }

version="$(sed -n 's/^ *versionName = "\(.*\)"/\1/p' phone/build.gradle.kts | head -n1)"
[[ -n "$version" ]] || { echo "ERROR: no versionName in phone/build.gradle.kts" >&2; exit 2; }

./gradlew --no-daemon clean assembleRelease :watch:testDebugUnitTest :phone:testSableDebugUnitTest
bash tools/validate-watchface.sh

out="${SABLEWATCH_RELEASE_DIR:-$HOME/Downloads/sable-watch-release/$version}"
mkdir -p "$out"
read -rsp "Keystore password for $SABLEWATCH_KEYSTORE: " pw; echo
sign() {
    local unsigned="$1" name="$2"
    printf '%s\n' "$pw" | "$apksigner" sign --ks "$SABLEWATCH_KEYSTORE" --ks-key-alias "$SABLEWATCH_KEY_ALIAS" \
        --ks-pass stdin --out "$out/$name" "$unsigned"
}
sign watchface/build/outputs/apk/release/watchface-release-unsigned.apk "sable-watchface-$version.apk"
sign watch/build/outputs/apk/release/watch-release-unsigned.apk "sable-watch-wear-$version.apk"
sign phone/build/outputs/apk/sable/release/phone-sable-release-unsigned.apk "sable-watch-phone-sable-$version.apk"
sign phone/build/outputs/apk/generic/release/phone-generic-release-unsigned.apk "sable-watch-phone-generic-$version.apk"
unset pw
rm -f "$out"/*.idsig

bash tools/check-apks.sh --signed "$out"
git rev-parse HEAD > "$out/SOURCE_COMMIT.txt"
# sha256sum on Linux and Git Bash; shasum -a 256 (same output) on macOS.
if command -v sha256sum >/dev/null; then sum=(sha256sum); else sum=(shasum -a 256); fi
(cd "$out" && "${sum[@]}" ./*.apk > SHA256SUMS.txt)
printf 'Signed APKs and SHA256SUMS.txt: %s\nSource commit: %s\n' "$out" "$(git rev-parse HEAD)"
echo "Publish with: gh release create v$version \"$out\"/*.apk \"$out\"/SHA256SUMS.txt --title \"Sable Watch $version\" --generate-notes"
