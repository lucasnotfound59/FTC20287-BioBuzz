import importlib.util
from pathlib import Path
import shlex
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('csv_sync', Path(__file__).resolve().parents[2] / 'scripts/sync_shooter_csv.py')
sync = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sync)
CSV = 'target_tps,p,i,d,f,tolerance_tps\n1560,10,0,0,12,40\nmanual_marker,pre_tps\n1,1560\n'


class UsbDevice:
    def __init__(self):
        self.serial = 'HUB-123'
        self.content = CSV
        self.pulls = 0
        self.interrupt = False

    def run(self, adb, *args):
        if args == ('-d', 'get-serialno'):
            return self.serial
        assert args[:2] == ('-s', self.serial)
        if args[2] == 'pull':
            self.pulls += 1
            Path(args[4]).write_text(self.content)
            if self.interrupt:
                raise RuntimeError('USB disconnected')
            return ''
        shell = shlex.split(args[3])
        if shell[0] == 'ls':
            return 'trial-123.csv\nignore.txt\n../../escape.csv'
        if shell[0] == 'stat':
            return str(len(self.content.encode()))
        raise AssertionError(args)


class SyncChecks(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.output = Path(self.temp.name)
        self.device = UsbDevice()

    def scan(self, previous):
        return sync.sync_once('adb', self.output, sync.REMOTE, previous, self.device.run)

    def test_waits_for_stable_file_then_copies_and_skips_duplicates(self):
        previous, files = self.scan({})
        self.assertEqual(files, [])
        previous, files = self.scan(previous)
        self.assertEqual(len(files), 1)
        self.assertEqual(files[0].read_text(), CSV)
        self.assertEqual(self.scan(previous)[1], [])
        self.assertEqual(self.device.pulls, 1)

    def test_growing_file_waits_again(self):
        previous, _ = self.scan({})
        self.device.content += '2,1550\n'
        previous, files = self.scan(previous)
        self.assertEqual(files, [])
        self.assertEqual(len(self.scan(previous)[1]), 1)

    def test_partial_csv_not_published(self):
        self.device.content = 'target_tps,p,i,d,f,\n'
        previous, _ = self.scan({})
        self.assertEqual(self.scan(previous)[1], [])
        self.assertEqual(list(self.output.rglob('*.csv')), [])
        self.assertEqual(list(self.output.rglob('.transfer-*')), [])

    def test_interrupted_pull_leaves_no_partial_file(self):
        previous, _ = self.scan({})
        self.device.interrupt = True
        with self.assertRaises(RuntimeError):
            self.scan(previous)
        self.assertEqual(list(self.output.rglob('*.csv')), [])
        self.assertEqual(list(self.output.rglob('.transfer-*')), [])
        self.device.interrupt = False
        self.assertEqual(len(self.scan(previous)[1]), 1)

    def test_two_devices_do_not_overwrite_each_other(self):
        previous, _ = self.scan({})
        _, first = self.scan(previous)
        self.device.serial = 'HUB-456'
        previous, files = self.scan(previous)
        self.assertEqual(files, [])
        _, second = self.scan(previous)
        self.assertNotEqual(first[0], second[0])
        self.assertEqual(first[0].read_text(), CSV)


if __name__ == '__main__':
    unittest.main()
