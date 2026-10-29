#!/bin/sh
set -eu
task_root=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
task_output=$(mktemp -d)
trap 'rm -rf "$task_output"' EXIT HUP INT TERM
task_java=${JAVA_HOME:+$JAVA_HOME/bin/}
"${task_java}javac" --release 8 -d "$task_output" \
  "$task_root/TeamCode/src/main/java/org/firstinspires/ftc/teamcode/tests/shooter/ShotSpeedTrial.java" \
  "$task_root/tests/shooter/ShotSpeedTrialChecks.java"
"${task_java}java" -cp "$task_output" ShotSpeedTrialChecks
