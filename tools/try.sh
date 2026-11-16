#!/usr/bin/env bash
# One command to try Sable Watch: builds the debug apps and installs them on whatever is
# connected. A Wear OS device gets the watch app and the face (and the face is set active);
# a phone gets the phone app. Emulators and real devices both work.
#
#   bash tools/try.sh            # phone gets the "generic" build (any Android phone)
#   bash tools/try.sh sable      # phone gets the SableOS build
#
# Works with the bash that ships on macOS (3.2) and on Linux. Needs nothing beyond Android
# Studio: it finds adb in the Android SDK and Java in Android Studio if they aren't on PATH.
set -euo pipefail

cd "$(dirname "$0")/.."
FLAVOR="${1:-generic}"
case "$FLAVOR" in
  generic) VARIANT=GenericDebug ;;
  sable) VARIANT=SableDebug ;;
  *) echo "Usage: bash tools/try.sh [generic|sable]" >&2; exit 2 ;;
esac
FACE_PKG=io.github.aimindseye.sablewatch.face

say() { printf '\n==> %s\n' "$*"; }

# --- Java: Gradle needs a JDK 17-24. Android Studio's own one is fine. ---
if [[ -z "${JAVA_HOME:-}" ]]; then
  for jbr in \
    "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
    "$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
    "/opt/android-studio/jbr" \
    "$HOME/android-studio/jbr"; do
    if [[ -x "$jbr/bin/java" ]]; then export JAVA_HOME="$jbr"; break; fi
  done
fi

# --- adb: on PATH, or in the SDK that Android Studio installed. ---
ADB="$(command -v adb || true)"
if [[ -z "$ADB" ]]; then
  for sdk in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
    if [[ -n "$sdk" ]] && [[ -x "$sdk/platform-tools/adb" ]]; then ADB="$sdk/platform-tools/adb"; break; fi
  done
fi
if [[ -z "$ADB" ]]; then
  echo "Can't find adb. Open Android Studio once (it installs the SDK), then try again." >&2
  exit 1
fi

# --- Which devices are connected, and which of them are watches. ---
WATCHES=""
PHONES=""
SEEN=" "
for serial in $("$ADB" devices | awk 'NR > 1 && $2 == "device" { print $1 }'); do
  # A device on wireless debugging can be listed twice (by IP and by its mDNS name);
  # its hardware serial number tells the two apart from different devices.
  hw="$("$ADB" -s "$serial" shell getprop ro.serialno 2>/dev/null | tr -d '\r')"
  if [[ -n "$hw" ]]; then
    case "$SEEN" in *" $hw "*) continue ;; esac
    SEEN="$SEEN$hw "
  fi
  traits="$("$ADB" -s "$serial" shell getprop ro.build.characteristics 2>/dev/null | tr -d '\r')"
  case "$traits" in
    *watch*) WATCHES="$WATCHES $serial" ;;
    *) PHONES="$PHONES $serial" ;;
  esac
done
if [[ -z "$WATCHES$PHONES" ]]; then
  echo "No device is connected. Start the Wear OS emulator (and a phone emulator, if you" >&2
  echo "want the phone side too) from Android Studio's Device Manager, then run this again." >&2
  exit 1
fi
echo "Watches:${WATCHES:- none}"
echo "Phones: ${PHONES:- none}"

# --- Build only what's needed. ---
TASKS=""
[[ -n "$WATCHES" ]] && TASKS="$TASKS :watch:assembleDebug :watchface:assembleDebug"
[[ -n "$PHONES" ]] && TASKS="$TASKS :phone:assemble$VARIANT"
say "Building$TASKS"
# shellcheck disable=SC2086 # TASKS is a list of words on purpose.
./gradlew --quiet $TASKS

# Only the APKs that were just built exist, so look them up only when a device needs them.
first_apk() { local dir="$1"; find "$dir" -name '*.apk' -print 2>/dev/null | head -n 1; }
WATCH_APK=""; FACE_APK=""; PHONE_APK=""
if [[ -n "$WATCHES" ]]; then
  WATCH_APK="$(first_apk watch/build/outputs/apk/debug)"
  FACE_APK="$(first_apk watchface/build/outputs/apk/debug)"
fi
[[ -n "$PHONES" ]] && PHONE_APK="$(first_apk "phone/build/outputs/apk/$FLAVOR/debug")"

for serial in $WATCHES; do
  say "Watch $serial: installing the watch app and the face"
  "$ADB" -s "$serial" install -r -t "$WATCH_APK" >/dev/null
  "$ADB" -s "$serial" install -r -t "$FACE_APK" >/dev/null
  # The same broadcast Android Studio's watch-face run uses to make a face active.
  if "$ADB" -s "$serial" shell am broadcast \
      -a com.google.android.wearable.app.DEBUG_SURFACE \
      --es operation set-watchface --es watchFaceId "$FACE_PKG" >/dev/null 2>&1; then
    echo "Face set. If the watch still shows another face, long-press it and pick Sable."
  else
    echo "Installed. Long-press the watch face, swipe to the end, tap + and pick Sable."
  fi
done

for serial in $PHONES; do
  say "Phone $serial: installing the phone app ($FLAVOR)"
  "$ADB" -s "$serial" install -r -t "$PHONE_APK" >/dev/null
  "$ADB" -s "$serial" shell monkey -p io.github.aimindseye.sablewatch \
    -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
  echo "Opened Sable Watch on the phone."
done

say "Done"
if [[ -n "$WATCHES" ]] && [[ -z "$PHONES" ]]; then
  echo "Only a watch is connected, so Hub counts, Agenda Peek, World Clock and weather stay"
  echo "empty. Pair a phone emulator (Device Manager > watch > Pair Wearable) to see them."
fi
