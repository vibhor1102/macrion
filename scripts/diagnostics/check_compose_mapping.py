#!/usr/bin/env python3
"""Verify real release Compose mappings using synthetic group-key frames, not user reports."""
import argparse
from pathlib import Path
import re
import subprocess
from archive import mapping_id


def check(mapping, retrace):
    data = mapping.read_bytes()
    identity = mapping_id(data)
    text = data.decode()
    section = text.split('ComposeStackTrace -> $$compose:\n', 1)
    if len(section) != 2:
        raise ValueError('Release mapping contains no Compose group-key mappings')
    frames = []
    expected = []
    for line in section[1].splitlines():
        if line and not line.startswith((' ', '#')):
            break
        match = re.fullmatch(r'    (\d+):\d+:\S+ (io\.github\.vibhor1102\.macrion\.[^(]+)\(.*\):\d+:\d+ -> (m\$-?\d+)', line)
        if match:
            number, method, group = match.groups()
            frames.append(f'\tat $$compose.{group}(SourceFile:{number})')
            expected.append(method)
        if len(frames) == 20:
            break
    if not frames:
        raise ValueError('No Macrion Compose frames found')
    trace = 'java.lang.RuntimeException: Synthetic Compose mapping check\n' + '\n'.join(frames) + '\n'
    result = subprocess.run([str(retrace), str(mapping)], input=trace,
                            text=True, capture_output=True, check=True)
    for method in expected:
        if method not in result.stdout:
            raise ValueError(f'Retrace did not recover {method}')
    print(f'PASS: {len(frames)} Compose group-key frames decoded with mapping {identity}')
    print('This checks release mapping/retrace compatibility, not runtime exception capture.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mapping', type=Path)
    parser.add_argument('retrace', type=Path)
    args = parser.parse_args()
    check(args.mapping, args.retrace)
