#!/usr/bin/env python3
"""Package and publish compiler diagnostics, never application or crash data."""
import argparse
import hashlib
import json
import re
import subprocess
import tempfile
import zipfile
from pathlib import Path

REPOSITORY = 'vibhor1102/macrion-assets'

def sha(data):
    return hashlib.sha256(data).hexdigest()

def mapping_id(data):
    match = re.search(rb'^# pg_map_id: ([0-9a-f]{7,64})$', data, re.M)
    if not match:
        raise ValueError('Missing R8 mapping ID')
    return match[1].decode()

def package(mapping_dir, output, version, commit, native_root=None):
    mapping = (mapping_dir / 'mapping.txt').read_bytes()
    identity = mapping_id(mapping)
    files = {'mapping.txt': mapping}
    for name in ('configuration.txt', 'seeds.txt', 'usage.txt'):
        if (mapping_dir / name).is_file():
            files[name] = (mapping_dir / name).read_bytes()
    def write_archive(path, contents):
        manifest = {'schemaVersion': 1, 'sourceRepository': 'vibhor1102/Macrion',
                    'version': version, 'commit': commit, 'mappingId': identity,
                    'compiler': {key.decode(): value.decode() for key, value in
                        re.findall(rb'^# (compiler(?:_version|_hash)?): (.+)$', mapping, re.M)},
                    'files': {n: sha(data) for n, data in contents.items()}}
        members = dict(contents)
        members['manifest.json'] = (json.dumps(manifest, indent=2, sort_keys=True) + '\n').encode()
        with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
            for name, data in sorted(members.items()):
                entry = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
                entry.compress_type = zipfile.ZIP_DEFLATED
                archive.writestr(entry, data)
    output.mkdir(parents=True, exist_ok=True)
    path = output / 'diagnostics.zip'
    write_archive(path, files)
    native_files_data = {}
    if native_root is not None:
        native_files = list(native_root.glob('*/obj/*/*.so'))
        if not native_files:
            raise ValueError('Expected native release libraries are missing')
        for library in native_files:
            abi = library.parent.name
            if abi not in ('arm64-v8a', 'armeabi-v7a', 'x86', 'x86_64'):
                raise ValueError('Unexpected native ABI')
            name = f'native/{abi}/{library.name}'
            data = library.read_bytes()
            if name in native_files_data and native_files_data[name] != data:
                raise ValueError('Conflicting native builds in archive input')
            native_files_data[name] = data
        # Native-only changes can share an R8 ID. Keep each binary archive by content hash.
        native_path = output / 'native.zip'
        write_archive(native_path, {**files, **native_files_data})
        native_path.rename(output / ('native-' + sha(native_path.read_bytes()) + '.zip'))
    print(f'Packaged mapping {identity}')
    return path

def inspect(path):
    with zipfile.ZipFile(path) as archive:
        if sum(i.file_size for i in archive.infolist()) > 512 * 1024 * 1024:
            raise ValueError('Archive exceeds diagnostic size limit')
        manifest = json.loads(archive.read('manifest.json'))
        if manifest.get('schemaVersion') != 1 or manifest.get('sourceRepository') != 'vibhor1102/Macrion':
            raise ValueError('Unknown diagnostic archive format')
        allowed = re.compile(r'(mapping\.txt|configuration\.txt|seeds\.txt|usage\.txt|native/(arm64-v8a|armeabi-v7a|x86|x86_64)/[A-Za-z0-9_.+-]+\.so)')
        if not all(allowed.fullmatch(name) for name in manifest['files']):
            raise ValueError('Unexpected diagnostic archive file')
        names = archive.namelist()
        if (len(names) != len(set(names)) or
                set(names) != {'manifest.json', *manifest['files']} or
                'mapping.txt' not in manifest['files']):
            raise ValueError('Archive members do not match the manifest')
        for name, digest in manifest['files'].items():
            if sha(archive.read(name)) != digest:
                raise ValueError('Archive checksum mismatch')
        if mapping_id(archive.read('mapping.txt')) != manifest['mappingId']:
            raise ValueError('Mapping ID mismatch')
        return manifest

def gh(*args, check=True):
    return subprocess.run(['gh', *args], check=check, capture_output=True, text=True)

def publish(path):
    manifest = inspect(path)
    tag = 'r8-' + manifest['mappingId']
    existing = gh('release', 'view', tag, '--repo', REPOSITORY, check=False)
    if existing.returncode != 0:
        # A create failure (including authentication/network errors) is fatal, never overwritten.
        gh('release', 'create', tag, str(path), '--repo', REPOSITORY,
           '--title', f"Macrion {manifest['version']} compiler diagnostics",
           '--notes', f"Developer archive for source commit {manifest['commit']}. No crash reports or user data.",
           '--latest=false')
    with tempfile.TemporaryDirectory() as directory:
        gh('release', 'download', tag, '--repo', REPOSITORY, '--pattern', 'diagnostics.zip', '--dir', directory)
        remote = inspect(Path(directory) / 'diagnostics.zip')
        if remote['files'] != manifest['files'] or remote['mappingId'] != manifest['mappingId']:
            raise ValueError('Existing archive differs; refusing to replace compiler diagnostics')
    for companion in sorted(path.parent.glob('native-*.zip')):
        inspect(companion)
        with tempfile.TemporaryDirectory() as directory:
            result = gh('release', 'download', tag, '--repo', REPOSITORY,
                        '--pattern', companion.name, '--dir', directory, check=False)
            if result.returncode != 0:
                gh('release', 'upload', tag, str(companion), '--repo', REPOSITORY)
                gh('release', 'download', tag, '--repo', REPOSITORY,
                   '--pattern', companion.name, '--dir', directory)
            if sha((Path(directory) / companion.name).read_bytes()) != sha(companion.read_bytes()):
                raise ValueError('Native archive verification failed')
    print(f'Archive verified: https://github.com/{REPOSITORY}/releases/tag/{tag}')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    pack = commands.add_parser('package')
    pack.add_argument('--mapping-dir', type=Path, required=True)
    pack.add_argument('--output', type=Path, required=True)
    pack.add_argument('--version', required=True)
    pack.add_argument('--commit', required=True)
    pack.add_argument('--native-root', type=Path)
    upload = commands.add_parser('publish')
    upload.add_argument('archive', type=Path)
    args = parser.parse_args()
    if args.command == 'package':
        package(args.mapping_dir, args.output, args.version, args.commit, args.native_root)
    else:
        publish(args.archive)
