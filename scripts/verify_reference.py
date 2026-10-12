#!/usr/bin/env python3
"""Ensure the archived upstream source stays complete and unchanged."""
import hashlib
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
reference = root / 'upstream/Hephaestus'
expected = json.loads((root / 'docs/upstream-sha256.json').read_text())
actual = {p.relative_to(reference).as_posix() for p in reference.rglob('*') if p.is_file()}
if actual != set(expected):
    raise SystemExit(f'Reference inventory changed: missing={len(set(expected)-actual)}, extra={len(actual-set(expected))}')
for name, digest in expected.items():
    if hashlib.sha256((reference / name).read_bytes()).hexdigest() != digest:
        raise SystemExit(f'Reference file changed: {name}')
print(f'PASS: {len(expected)} original Hephaestus files preserved unchanged.')
