#!/usr/bin/env bash
set -euo pipefail

adb install -r -g "$RUNNER_TEMP/v13-full.apk"
adb logcat -c
adb shell am start -W -n com.godnit.handgesturecube/.MainActivity

processed=0
for attempt in $(seq 1 90); do
  if adb logcat -d -s HandGestureCube:I '*:S' | grep -q HAND_TRACKER_RESULT_10; then
    processed=1
    break
  fi
  if adb logcat -d | grep -Eq 'FATAL EXCEPTION|Fatal signal|UnsatisfiedLinkError'; then
    adb logcat -d
    exit 1
  fi
  sleep 1
done

if [ "$processed" -ne 1 ]; then
  adb logcat -d
  echo 'Restored v1.3 tracker did not process 10 frames on Android 8.1'
  exit 1
fi

adb shell pidof com.godnit.handgesturecube
