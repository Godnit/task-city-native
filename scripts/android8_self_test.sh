#!/usr/bin/env bash
set -euo pipefail

apk_path="app/build/outputs/apk/release/app-release.apk"
package_name="com.godnit.handgesturecube"
activity_name="$package_name/.MainActivity"

adb install -g "$apk_path"
adb logcat -c
timeout 15s adb shell am start -n "$activity_name" --ez tracker_self_test true || true

passed=0
for _attempt in $(seq 1 240); do
  logs="$(adb logcat -d)"
  if grep -q 'HAND_TRACKER_SELF_TEST_PASSED' <<<"$logs"; then
    passed=1
    break
  fi
  if grep -Eq 'HAND_TRACKER_SELF_TEST_FAILED|FATAL EXCEPTION|Fatal signal|UnsatisfiedLinkError' <<<"$logs"; then
    printf '%s\n' "$logs"
    exit 1
  fi
  sleep 1
done

if [ "$passed" -ne 1 ]; then
  adb logcat -d
  echo 'Hand tracker did not complete native inference on Android 8.1'
  exit 1
fi

sleep 5
logs="$(adb logcat -d)"
if grep -Eq 'HAND_TRACKER_SELF_TEST_FAILED|FATAL EXCEPTION|Fatal signal|UnsatisfiedLinkError' <<<"$logs"; then
  printf '%s\n' "$logs"
  exit 1
fi

adb shell pidof "$package_name"
