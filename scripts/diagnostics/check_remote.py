#!/usr/bin/env python3
"""Exercise the archive credential with a temporary draft release and clean it up."""
import os
from pathlib import Path
import tempfile
from archive import gh, REPOSITORY, sha

run = os.environ['GITHUB_RUN_ID']
attempt = os.environ.get('GITHUB_RUN_ATTEMPT', '1')
assert run.isdigit() and attempt.isdigit()
tag = f'archive-check-{run}-{attempt}'
created = False
try:
    with tempfile.TemporaryDirectory() as directory:
        root = Path(directory)
        probe = root / 'archive-check.txt'
        probe.write_text('Synthetic archive connectivity verification. No application or user data.\n')
        gh('release', 'create', tag, str(probe), '--repo', REPOSITORY,
           '--draft', '--title', 'Temporary archive credential check', '--notes', 'Automatically removed after verification.')
        created = True
        download = root / 'download'
        download.mkdir()
        gh('release', 'download', tag, '--repo', REPOSITORY, '--pattern', probe.name, '--dir', str(download))
        assert sha(probe.read_bytes()) == sha((download / probe.name).read_bytes())
        print('Archive credential can create releases, upload assets and retrieve matching bytes.')
finally:
    if created:
        gh('release', 'delete', tag, '--repo', REPOSITORY, '--yes', '--cleanup-tag')
        print('Temporary draft release removed.')
