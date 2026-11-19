#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

echo "==> Building standalone Impostor debug APK (no Firebase or secrets needed)..."
chmod +x ./gradlew
./gradlew assembleDebug --no-daemon

APK_PATH="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "$APK_PATH" ]; then
  echo "ERROR: APK not found at $APK_PATH"
  exit 1
fi

echo "==> Build succeeded: $APK_PATH"

if command -v adb >/dev/null 2>&1; then
  DEVICES="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
  if [ -n "$DEVICES" ]; then
    echo "==> Installing onto connected Android device..."
    adb install -r "$APK_PATH"
    echo "==> Installed! Launching Impostor..."
    adb shell monkey -p com.aistudio.impostorgame.qvnxkp -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1 || true
    echo "==> Done."
    exit 0
  fi
fi

echo ""
echo "No USB/wireless ADB device detected."
echo "Copy this APK to your Android phone and tap it to install:"
echo "  $APK_PATH"
