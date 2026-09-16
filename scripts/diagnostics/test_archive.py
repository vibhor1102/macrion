import tempfile
import unittest
import zipfile
from pathlib import Path
from archive import package, inspect

class ArchiveTests(unittest.TestCase):
    def test_deterministic_and_allowlisted(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            (root/'mapping.txt').write_text('# pg_map_id: abcdef123\nA -> a:\n')
            (root/'credentials.txt').write_text('not an archive input')
            first = package(root, root/'a', '0.5.0', 'a'*40)
            second = package(root, root/'b', '0.5.0', 'a'*40)
            self.assertEqual(first.read_bytes(), second.read_bytes())
            self.assertEqual(inspect(first)['mappingId'], 'abcdef123')
            with zipfile.ZipFile(first) as z:
                self.assertEqual(set(z.namelist()), {'manifest.json', 'mapping.txt'})
    def test_tampering_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            (root/'mapping.txt').write_text('# pg_map_id: abcdef123\n')
            archive = package(root, root/'out', '0.5.0', 'a'*40)
            with zipfile.ZipFile(archive) as z: manifest = z.read('manifest.json')
            with zipfile.ZipFile(archive, 'w') as z:
                z.writestr('manifest.json', manifest)
                z.writestr('mapping.txt', '# pg_map_id: 111111111\n')
            with self.assertRaises(ValueError): inspect(archive)

    def test_unlisted_and_duplicate_members_are_rejected(self):
        for member in ('credentials.txt', 'mapping.txt'):
            with self.subTest(member=member), tempfile.TemporaryDirectory() as d:
                root = Path(d)
                (root/'mapping.txt').write_text('# pg_map_id: abcdef123\n')
                archive = package(root, root/'out', '0.5.0', 'a'*40)
                with zipfile.ZipFile(archive, 'a') as z:
                    z.writestr(member, 'unexpected')
                with self.assertRaises(ValueError): inspect(archive)

    def test_native_changes_preserve_mapping_archive(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            (root/'mapping.txt').write_text('# pg_map_id: abcdef123\n')
            library = root/'native/build/obj/arm64-v8a/libmacrion.so'
            library.parent.mkdir(parents=True)
            library.write_bytes(b'first native build')
            first = package(root, root/'first', '0.5.0', 'a'*40, root/'native')
            library.write_bytes(b'second native build')
            second = package(root, root/'second', '0.5.0', 'a'*40, root/'native')
            self.assertEqual(first.read_bytes(), second.read_bytes())
            one = next(first.parent.glob('native-*.zip'))
            two = next(second.parent.glob('native-*.zip'))
            self.assertNotEqual(one.name, two.name)
            self.assertIn('native/arm64-v8a/libmacrion.so', inspect(two)['files'])

if __name__ == '__main__': unittest.main()
