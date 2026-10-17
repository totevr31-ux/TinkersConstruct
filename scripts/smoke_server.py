#!/usr/bin/env python3
"""Load the packaged mod in a clean Fabric server, stopping at the EULA gate.

This checks production JAR discovery and ModInitializer execution, not a world
tick, client rendering, or gameplay. It never accepts the Minecraft EULA.
"""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
PROPS = dict(line.split('=', 1) for line in
             (ROOT / 'gradle.properties').read_text().splitlines()
             if '=' in line and not line.startswith('#'))
RUNTIME = ROOT / 'build' / 'smoke-server'
LOGS = ROOT / 'ci-logs'
LOGS.mkdir(exist_ok=True)
if RUNTIME.exists():
    shutil.rmtree(RUNTIME)
(RUNTIME / 'mods').mkdir(parents=True)
mc = PROPS['minecraft_version']
loader = PROPS['loader_version']
api = PROPS['fabric_api_version']
jar = ROOT / 'dist' / (PROPS['archives_base_name'] + '-' +
                      PROPS['mod_version'] + '+' + mc + '.jar')
shutil.copy2(jar, RUNTIME / 'mods' / jar.name)

def download(url, target):
    with urllib.request.urlopen(url, timeout=120) as response:
        target.write_bytes(response.read())

download(f'https://meta.fabricmc.net/v2/versions/loader/{mc}/{loader}/1.1.2/server/jar',
         RUNTIME / 'fabric-server-launch.jar')
download(f'https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/{api}/fabric-api-{api}.jar',
         RUNTIME / 'mods' / f'fabric-api-{api}.jar')
(RUNTIME / 'eula.txt').write_text('eula=false\n')
with (LOGS / 'server-smoke.log').open('w') as log:
    result = subprocess.run(['java', '-Xmx2G', '-jar', 'fabric-server-launch.jar', 'nogui'],
                            cwd=RUNTIME, stdout=log, stderr=subprocess.STDOUT, timeout=180)
output = (LOGS / 'server-smoke.log').read_text(errors='replace')
initialized = "Radi's Construct initialized" in output
eula_gate = 'You need to agree to the EULA' in output
assert 'eula=false' in (RUNTIME / 'eula.txt').read_text(), 'EULA was unexpectedly changed'
report = {'minecraft': mc, 'loader': loader, 'jar': jar.name,
          'jar_sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
          'exit_code': result.returncode, 'initializer_executed': initialized,
          'stopped_at_eula_gate': eula_gate, 'eula_accepted': False,
          'world_started': False, 'client_tested': False,
          'passed': result.returncode == 0 and initialized and eula_gate}
(LOGS / 'server-smoke.json').write_text(json.dumps(report, indent=2) + '\n')
print(output[-12000:])
print(json.dumps(report, indent=2))
if not report['passed']:
    raise SystemExit('Production Fabric bootstrap check failed; see server-smoke.log')
