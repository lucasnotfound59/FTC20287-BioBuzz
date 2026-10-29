#!/bin/zsh
cd -- "${0:A:h}" || exit 1
python3 ./sync_shooter_csv.py "$@"
result=$?
if (( result != 0 )); then
  read '?同步未启动，按回车关闭。'
fi
exit "$result"
