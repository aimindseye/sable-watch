# Getting started on Windows or a Mac

This gets the face onto the Wear OS emulator, then onto a Galaxy Watch 7. The steps are written for Windows; [On a Mac](#on-a-mac) lists what differs.

## Why the old project didn't run

The first version (`Tensevenwatchface`) built, but Android Studio couldn't start it, for these reasons. All are fixed here.

1. **A watch face has no screen to launch.** Android Studio's default run configuration launches the app's main activity, and fails with "Default Activity not found". This repository ships run configurations in `.run/` that install the face and stop there ("Launch: Nothing").
2. **Mac-only paths.** The zip carried `local.properties` pointing at `/Users/.../Library/Android/sdk`, plus `.gradle`, `.idea` and `build` folders from the Mac. None of these are in this repository; Android Studio writes `local.properties` for your Windows SDK on first sync.
3. **Toolchain.** The old project needed AGP 9.5 and Gradle 9.6 with a downloaded JDK. This one uses the same stack as Sable Games (AGP 8.13, Gradle 8.14, JDK 17), which current Android Studio opens without changes.

## 1. Install

1. Install the current stable [Android Studio](https://developer.android.com/studio). Let it install the SDK, Platform-Tools and the emulator.
2. In Windows, open **Turn Windows features on or off**, turn on **Windows Hypervisor Platform**, and reboot. Virtualization must also be on in the BIOS (Task Manager > Performance > CPU shows "Virtualization: Enabled").
3. Install [Git for Windows](https://git-scm.com/download/win). It includes Git Bash, which runs the scripts in `tools/`.

## 2. Open the project

1. Clone `https://github.com/aimindseye/sable-watch` to a short path such as `C:\dev\sable-watch`.
2. In Android Studio: **File > Open** and choose that folder. Accept the Gradle sync. If it asks for a JDK, choose **JDK 17** (Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK).

## 3. Make a Wear OS emulator

1. **Tools > Device Manager > + > Create Virtual Device**.
2. Category **Wear OS**, device **Wear OS Large Round**.
3. System image: Wear OS 5 or later (API 34+), x86_64. Download it if needed. Finish.
4. Start it and wait for the watch face.

## 4. Run the face

1. In the run configuration list (top toolbar), pick **Watch face**, and the emulator as the device. Press Run. It installs the face and stops; that's expected.
2. On the emulator, long-press the watch face, swipe left to the end, tap **+ Add**, and pick **Sable**.
3. Pick **Watch app (complications)** and Run, to install Sable World Clock.
4. Long-press the face, tap **Edit** (or the pencil), tap the top arc, and choose **Sable World Clock**. The arc now shows `LDN 14:09 • NYC 09:09 • TOK 23:09` in your current times.

If a run configuration says the module isn't set, edit it (Run > Edit Configurations) and pick the `watchface`, `watch` or `phone` module.

## 5. The Galaxy Watch 7 over Wi-Fi

Debugging the watch doesn't need a cable or Samsung's tools:

1. On the watch: **Settings > About watch > Software information**, tap **Software version** five times to turn on Developer options.
2. **Settings > Developer options**: turn on **ADB debugging** and **Wireless debugging**. Put the watch and the laptop on the same Wi-Fi.
3. In Wireless debugging, tap **Pair new device**. It shows an IP, a port and a code.
4. On the laptop, in a terminal: `adb pair <ip>:<pairing-port>` and enter the code. Then `adb connect <ip>:<port>` with the port shown on the main Wireless debugging screen (it differs from the pairing port).
5. The watch now shows up in Android Studio's device list. Run **Watch face** with it selected, then pick the face on the watch as in step 4.

Wireless debugging turns itself off when the watch sleeps for a while; repeat `adb connect` if it drops.

## 6. The phone app

Pick **Build > Select Build Variant**, choose `sableDebug` (SableOS) or `genericDebug` (any phone), then run **Phone app** on the phone.

## On a Mac

Everything above works the same in Android Studio for macOS, with these differences:

- **Install**: Android Studio for your chip (Apple silicon or Intel). Skip the Windows Hypervisor and Git Bash steps; macOS has Git (it offers the Command Line Tools on first use) and the scripts run in Terminal.
- **Clone** anywhere, for example `~/dev/sable-watch`. Android Studio writes `local.properties` with your Mac SDK path (`~/Library/Android/sdk`); it stays out of Git, so the same clone never carries Mac paths to Windows.
- **Gradle JDK**: Android Studio's bundled JDK works (the build targets Java 17 bytecode). If sync asks, pick JDK 17 or the bundled one.
- **Emulator image**: on Apple silicon choose the **arm64-v8a** Wear OS image (API 34+), not x86_64.
- **adb** for the watch over Wi-Fi: `~/Library/Android/sdk/platform-tools/adb pair …`, or add `platform-tools` to your `PATH`.
- **Signing a release**: `export ANDROID_HOME=~/Library/Android/sdk` first, then follow [RELEASING.md](RELEASING.md). The scripts run with macOS's own bash 3.2 and use `shasum -a 256` where Linux has `sha256sum`; `tools/validate-watchface.sh` also needs `javac` on the `PATH` (Android Studio's: `export PATH="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin:$PATH"`).
