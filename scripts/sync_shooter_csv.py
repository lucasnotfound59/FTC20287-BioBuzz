#!/usr/bin/env python3
"""Copy shooter CSVs from a USB-connected Robot Controller; never delete remote files."""
import argparse
import hashlib
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
import tempfile
import time

REMOTE = '/sdcard/Android/data/com.qualcomm.ftcrobotcontroller/files/shooter-tuning'
ROOT = Path(__file__).resolve().parents[1]


def find_adb():
    candidates = [shutil.which('adb')]
    for name in ('ANDROID_HOME', 'ANDROID_SDK_ROOT'):
        if os.environ.get(name):
            candidates.append(str(Path(os.environ[name]) / 'platform-tools/adb'))
    candidates.append(str(Path.home() / 'Library/Android/sdk/platform-tools/adb'))
    return next((p for p in candidates if p and Path(p).is_file()), None)


def command(adb, *args):
    result = subprocess.run([adb, *args], capture_output=True, text=True, timeout=20)
    if result.returncode:
        raise RuntimeError((result.stderr or result.stdout).strip())
    return result.stdout.strip()


def device_folder(serial):
    safe = re.sub(r'[^A-Za-z0-9_.-]', '_', serial)[:80]
    return safe + '-' + hashlib.sha256(serial.encode()).hexdigest()[:8]


def sync_once(adb, output, remote, previous, run=command):
    # -d selects USB only and refuses ambiguity instead of choosing another robot.
    serial = run(adb, '-d', 'get-serialno')
    if not serial or serial == 'unknown':
        raise RuntimeError('没有已授权的 USB 设备；请检查数据线、供电和 USB 调试授权。')
    prefix = (adb, '-s', serial)

    def shell(*args):
        return run(*prefix, 'shell', ' '.join(shlex.quote(arg) for arg in args))

    names = shell('ls', '-1', remote).splitlines()
    observed = {}
    copied = []
    for name in sorted(names):
        if not re.fullmatch(r'trial-[0-9]+\.csv', name):
            continue
        source = remote.rstrip('/') + '/' + name
        size = int(shell('stat', '-c', '%s', source))
        key = (serial, name)
        observed[key] = size
        # Two polls with equal nonzero size avoid copying the normal save-in-progress case.
        if size <= 0 or previous.get(key) != size:
            continue
        folder = output / device_folder(serial)
        folder.mkdir(parents=True, exist_ok=True)
        destination = folder / name
        if destination.exists() and destination.stat().st_size == size:
            continue
        fd, temporary = tempfile.mkstemp(prefix='.transfer-', dir=folder)
        os.close(fd)
        temp = Path(temporary)
        try:
            run(*prefix, 'pull', source, str(temp))
            if temp.stat().st_size != size or int(shell('stat', '-c', '%s', source)) != size:
                continue  # File changed during transfer; retry on a later poll.
            content = temp.read_text(encoding='utf-8')
            if (not content.startswith('target_tps,p,i,d,f,')
                    or '\nmanual_marker,' not in content or not content.endswith('\n')):
                continue  # Incomplete save; do not publish it as a finished CSV.
            temp.replace(destination)
            copied.append(destination)
        finally:
            temp.unlink(missing_ok=True)
    return observed, copied


def main():
    parser = argparse.ArgumentParser(description='通过 USB 自动接收 Shooter 调参 CSV；Ctrl+C 退出。')
    parser.add_argument('--adb', default=find_adb(), help='adb 可执行文件路径')
    parser.add_argument('--output', type=Path, default=ROOT / 'shooter-data', help='电脑保存目录')
    parser.add_argument('--remote-dir', default=REMOTE, help='机器人 CSV 目录，以 DS 显示为准')
    parser.add_argument('--once', action='store_true', help='扫描两次，传输现有文件后退出')
    args = parser.parse_args()
    if not args.adb:
        parser.error('找不到 adb。请安装 Android SDK Platform Tools，或指定 --adb。')
    output = args.output.expanduser().resolve()
    print('电脑保存目录：' + str(output), flush=True)
    print('仅使用 USB。调参器松开 RB 后按 Y 保存；首次传输约需 2–4 秒。Ctrl+C 退出。', flush=True)
    previous = {}
    last_error = None
    try:
        while True:
            try:
                previous, copied = sync_once(args.adb, output, args.remote_dir, previous)
                if last_error:
                    print('USB 连接已恢复。', flush=True)
                last_error = None
                for path in copied:
                    print('已传回：' + str(path), flush=True)
                if args.once:
                    time.sleep(2)
                    _, copied = sync_once(args.adb, output, args.remote_dir, previous)
                    for path in copied:
                        print('已传回：' + str(path), flush=True)
                    print('本次扫描完成。', flush=True)
                    return 0
            except (OSError, RuntimeError, ValueError, subprocess.TimeoutExpired) as error:
                message = str(error)
                if message != last_error:
                    print('等待 CSV / USB 连接：' + message, flush=True)
                    print('若目录不存在，请先在调参器中按 Y 保存一次。', flush=True)
                last_error = message
                previous = {}
                if args.once:
                    return 1
            time.sleep(2)
    except KeyboardInterrupt:
        print('\n同步已停止，已接收文件保留。')
        return 0


if __name__ == '__main__':
    sys.exit(main())
