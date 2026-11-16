# Releasing Sable Watch

Releases are built and signed on the owner's computer, the same way as TenSeven Launcher and Sable Games. CI never gets the key: it builds unsigned APKs only, and those are not published.

## The signing key

Sable Watch uses the same key as TenSeven Launcher and Sable Games, whose certificate (SHA-256 `9b2047390e5aa77537b8e8875cdfd3c2c72d0a0715e172a7f77457fc4300f292`) is registered in the Android Developer Console. `tools/check-apks.sh --signed` refuses APKs signed with any other key.

The watch app and the phone app **must** be signed with the same key: the Wear OS Data Layer only connects apps whose package name and signing certificate match. A new key makes the apps new apps: watches and phones can't update from the old ones.

Tell the script where the key is, in Git Bash (or Terminal on a Mac):

```bash
export SABLEWATCH_KEYSTORE=/c/Users/you/keys/tenseven-release.jks
export SABLEWATCH_KEY_ALIAS=tenseven
```

These are a path and a name, not secrets. The script asks for the keystore password once, at the prompt, and hands it to `apksigner` on stdin; it is never written to a file, an environment variable or the command line.

## Making a release

1. Raise `versionCode` and `versionName` in `watchface/`, `watch/` and `phone/build.gradle.kts` (keep the three in step), merge to `main` and pull it.
2. In Git Bash (or Terminal on a Mac): `bash tools/build-release.sh`. With clean sources it runs the unit tests and the face schema check, builds the four release APKs, signs them with `apksigner` after one password prompt, checks every package name, permission and the signing certificate (`tools/check-apks.sh --signed`), and writes the APKs, `SHA256SUMS.txt` and `SOURCE_COMMIT.txt` to `~/Downloads/sable-watch-release/<version>/` (`SABLEWATCH_RELEASE_DIR` overrides it).
3. Install the face and the watch app on the watch and the phone app on the phone, and check them.
4. Publish: the script prints the `gh release create` command, which tags `v<version>` and uploads the APKs and `SHA256SUMS.txt`.

| File | Install on |
| --- | --- |
| `sable-watchface-<version>.apk` | Watch |
| `sable-watch-wear-<version>.apk` | Watch |
| `sable-watch-phone-sable-<version>.apk` | Phone running SableOS |
| `sable-watch-phone-generic-<version>.apk` | Any other Android phone |

Install on the watch over Wi-Fi debugging with `adb -s <watch-ip>:<port> install <file>.apk` (see [Getting started](GETTING_STARTED.md#5-the-galaxy-watch-7-over-wi-fi)).

## Package names

Two names to register in the Android Developer Console:

```
io.github.aimindseye.sablewatch          the watch app and the phone app (both builds)
io.github.aimindseye.sablewatch.face     the watch face
```

Registration links a package name to a signing certificate's SHA-256; it doesn't list APKs. The watch app and both phone builds have the same name and the same key, so one registration should cover all three. Google's guide doesn't mention Wear OS specifically, so check the console's answer when you register. The face needs its own name because a Watch Face Format face must be a separate, code-free APK. The key is already verified for the Sable Games names, and Google says reusing a verified key for more names in the same account skips the APK upload step.

Not registered yet (2026-11-14).
