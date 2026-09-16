#!/usr/bin/env python3
"""Decode a crash JSON supplied on stdin using the matching public compiler archive."""
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile
from archive import inspect, mapping_id, REPOSITORY


def exception_lines(error, prefix=''):
    lines = [prefix + error.get('type', 'unknown') + (': ' + error['message'] if error.get('message') else '')]
    for frame in error.get('frames', []):
        location = 'Native Method' if frame.get('native') else f"{frame.get('file', 'Unknown Source')}:{frame['line']}"
        lines.append(f"\tat {frame['class']}.{frame['method']}({location})")
    for child in error.get('suppressed', []):
        lines += exception_lines(child, 'Suppressed: ')
    if error.get('cause'):
        lines += exception_lines(error['cause'], 'Caused by: ')
    return lines


def decode(report):
    trace = '\n'.join(exception_lines(report['crash'])) + '\n'
    ids = set(re.findall(r'r8-map-id-([a-f0-9]{7,64})', trace))
    if report.get('build', {}).get('mappingId'):
        ids.add(report['build']['mappingId'])
    if not ids:
        if report.get('build', {}).get('buildType') == 'release':
            raise ValueError('Report has no mapping ID; refusing to guess a release mapping')
        print(trace)
        return
    if len(ids) != 1 or not re.fullmatch('[a-f0-9]{7,64}', next(iter(ids))):
        raise ValueError('Ambiguous or invalid mapping ID')
    identity = next(iter(ids))
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    retrace = os.environ.get('RETRACE') or shutil.which('retrace')
    if not retrace and sdk:
        candidate = Path(sdk) / 'cmdline-tools/latest/bin/retrace'
        if candidate.is_file(): retrace = str(candidate)
    if not retrace:
        raise ValueError('Set RETRACE to the Android SDK cmdline-tools/.../bin/retrace executable')
    with tempfile.TemporaryDirectory(prefix='macrion-retrace-') as directory:
        root = Path(directory)
        subprocess.run(['gh', 'release', 'download', 'r8-' + identity, '--repo', REPOSITORY,
            '--pattern', 'diagnostics.zip', '--dir', directory], check=True, capture_output=True)
        archive = root / 'diagnostics.zip'
        manifest = inspect(archive)
        if manifest['mappingId'] != identity: raise ValueError('Downloaded the wrong mapping')
        with zipfile.ZipFile(archive) as z:
            data = z.read('mapping.txt')
        (root / 'mapping.txt').write_bytes(data)
        result = subprocess.run([retrace, str(root / 'mapping.txt')], input=trace,
            text=True, check=True, capture_output=True)
        print(result.stdout, end='')
        # Also decode obfuscated component names without printing arbitrary report content.
        components = list(dict.fromkeys(e['component'] for e in report.get('recentOperations', []) if e.get('component')))
        mapped_components = {}
        if components:
            mapped = subprocess.run([retrace, str(root / 'mapping.txt')], input='\n'.join(components)+'\n',
                text=True, check=True, capture_output=True).stdout.splitlines()
            if len(mapped) == len(components): mapped_components = dict(zip(components, mapped))
        for event in report.get('recentOperations', []):
            component = event.get('component')
            mapped = mapped_components.get(component, component or '')
            print(f"{event['millisecondsBeforeCrash']} ms before crash: {event['event']} {mapped} "
                  f"count={event.get('count', 1)} state={event.get('state')} attached={event.get('attached')}")
        for error in report.get('recentErrors', []):
            result = subprocess.run([retrace, str(root / 'mapping.txt')], input='\n'.join(exception_lines(error)),
                text=True, check=True, capture_output=True)
            print('Recent caught error:', result.stdout)

if __name__ == '__main__':
    try:
        decode(json.load(sys.stdin))
    except (ValueError, subprocess.CalledProcessError, KeyError, OSError) as error:
        # Do not echo subprocess arguments/output that could include sensitive report messages.
        print(str(error) if isinstance(error, ValueError) else 'Could not retrieve or decode compiler diagnostics.', file=sys.stderr)
        sys.exit(1)
