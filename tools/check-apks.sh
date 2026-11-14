#!/usr/bin/env bash
# Checks the release APKs under ROOT (the repository root):
#   watchface          io.github.aimindseye.sablewatch.face, no permissions, no code
#   watch              io.github.aimindseye.sablewatch
#   phone (sable)      io.github.aimindseye.sablewatch
#   phone (generic)    io.github.aimindseye.sablewatch
# The watch and phone apps must share one package name: the Wear OS Data Layer only connects
# apps with the same package name and signing key.
# With --signed (tools/build-release.sh), every APK must be signed with the registered key, the
# owner's TenSeven Launcher key. SABLEWATCH_EXPECTED_CERT overrides the expected SHA-256.
set -euo pipefail

signed=no
expected_cert="${SABLEWATCH_EXPECTED_CERT:-9b2047390e5aa77537b8e8875cdfd3c2c72d0a0715e172a7f77457fc4300f292}"
if [[ "${1:-}" == "--signed" ]]; then signed=yes; shift; fi
root="${1:?usage: tools/check-apks.sh [--signed] ROOT_OR_RELEASE_DIR}"
sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
aapt2="$(find "$sdk/build-tools" -maxdepth 2 \( -name aapt2 -o -name aapt2.exe \) -type f 2>/dev/null | LC_ALL=C sort | tail -n1)"
[[ -n "$aapt2" ]] || { echo "aapt2 not found under $sdk/build-tools"; exit 1; }
apksigner="$(dirname "$aapt2")/apksigner"
[[ -e "$apksigner" ]] || apksigner="$apksigner.bat"

# name|glob|package|allowed extra permission
apps=(
    "watchface|watchface/build/outputs/apk/release/*.apk sable-watchface-*.apk|io.github.aimindseye.sablewatch.face|"
    "watch|watch/build/outputs/apk/release/*.apk sable-watch-wear-*.apk|io.github.aimindseye.sablewatch|"
    "phone-sable|phone/build/outputs/apk/sable/release/*.apk sable-watch-phone-sable-*.apk|io.github.aimindseye.sablewatch|"
    "phone-generic|phone/build/outputs/apk/generic/release/*.apk sable-watch-phone-generic-*.apk|io.github.aimindseye.sablewatch|"
)
certs=()
fail=0
cd "$root"
for app in "${apps[@]}"; do
    IFS='|' read -r name globs want _ <<< "$app"
    apks=()
    for g in $globs; do for f in $g; do [[ -f "$f" ]] && apks+=("$f"); done; done
    if [[ "${#apks[@]}" != 1 ]]; then
        echo "FAIL $name: expected one release APK, found ${#apks[@]}"; fail=1; continue
    fi
    badging="$("$aapt2" dump badging "${apks[0]}")"
    package="$(sed -n "s/^package: name='\([^']*\)'.*/\1/p" <<< "$badging")"
    [[ "$package" == "$want" ]] || { echo "FAIL $name: package $package, expected $want"; fail=1; }
    # AndroidX adds <package>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION, the app's own signature
    # permission for its private receivers; it grants nothing. Any other permission must be
    # added here on purpose, with the reason.
    others="$(grep "^uses-permission" <<< "$badging" | grep -v "name='$want.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'" || true)"
    [[ -z "$others" ]] || { echo "FAIL $name asks for permissions:"; echo "$others"; fail=1; }
    if [[ "$name" == watchface ]] && "$aapt2" dump xmltree --file AndroidManifest.xml "${apks[0]}" | grep -q 'hasCode.*=(type 0x12)0xffffffff'; then
        echo "FAIL watchface: a Watch Face Format face must have hasCode=false"; fail=1
    fi
    if [[ "$signed" == yes ]]; then
        cert="$("$apksigner" verify --print-certs "${apks[0]}" 2>/dev/null | sed -n 's/.*certificate SHA-256 digest: //p' | head -n1)"
        if [[ -z "$cert" ]]; then echo "FAIL $name is not signed"; fail=1; else certs+=("$cert"); fi
    fi
    echo "checked $name: $package"
done
if [[ "$signed" == yes && "$fail" == 0 ]]; then
    mapfile -t distinct < <(printf '%s\n' "${certs[@]}" | sort -u)
    if [[ "${#distinct[@]}" != 1 ]]; then echo "FAIL the APKs are signed with ${#distinct[@]} different keys"; fail=1
    elif [[ "${distinct[0]}" != "$expected_cert" ]]; then
        echo "FAIL the APKs are signed with ${distinct[0]}, not the registered certificate $expected_cert"; fail=1
    else echo "PASS every APK signed with the registered certificate ${distinct[0]}"; fi
fi
[[ "$fail" == 0 ]] && echo "PASS all four APKs"
exit "$fail"
