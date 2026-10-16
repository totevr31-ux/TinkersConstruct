#!/usr/bin/env python3
"""Production Fabric bootstrap using installed Gradle artifacts, without metadata egress.

Checks initialization at the EULA gate only. Does not accept the EULA or claim
world/gameplay validation. Run verify_jar.py first; JAVA_HOME/PATH must use Java 25.
"""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PROPS = dict(line.split('=', 1) for line in (ROOT / 'gradle.properties').read_text().splitlines()
             if '=' in line and not line.startswith('#'))
CACHE = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle'))) / 'caches'
RUNTIME = ROOT / 'build' / 'smoke-cached-server'
LOGS = ROOT / 'ci-logs'


def artifact(group, name, version):
    files = list((CACHE / 'modules-2/files-2.1' / group / name / version).glob('*/*.jar'))
    if len(files) != 1:
        raise RuntimeError(f'Expected one installed artifact: {group}:{name}:{version}')
    return files[0].resolve()


loader = artifact('net.fabricmc', 'fabric-loader', PROPS['loader_version'])
classpath = [loader]
with zipfile.ZipFile(loader) as archive:
    libraries = json.loads(archive.read('fabric-installer.json'))['libraries']
for library in libraries['common'] + libraries['server']:
    path = artifact(*library['name'].split(':'))
    assert hashlib.sha256(path.read_bytes()).hexdigest() == library['sha256'], library['name']
    classpath.append(path)
server = CACHE / 'fabric-loom' / PROPS['minecraft_version'] / 'minecraft-server.jar'
metadata = json.loads((server.parent / 'mojang_minecraft_info.json').read_text())
assert hashlib.sha1(server.read_bytes()).hexdigest() == metadata['downloads']['server']['sha1']
classpath.append(server.resolve())
jar = ROOT / 'dist' / f"{PROPS['archives_base_name']}-{PROPS['mod_version']}+{PROPS['minecraft_version']}.jar"
assert jar.is_file(), 'Run scripts/verify_jar.py first'
if RUNTIME.exists():
    shutil.rmtree(RUNTIME)
(RUNTIME / 'mods').mkdir(parents=True)
LOGS.mkdir(exist_ok=True)
shutil.copy2(jar, RUNTIME / 'mods' / jar.name)
api = artifact('net.fabricmc.fabric-api', 'fabric-api', PROPS['fabric_api_version'])
shutil.copy2(api, RUNTIME / 'mods' / api.name)
(RUNTIME / 'eula.txt').write_text('eula=false\n')
(RUNTIME / 'server.properties').write_text('')
with (LOGS / 'server-cached-smoke.log').open('w') as log:
    result = subprocess.run(['java', '-Xmx2G', f'-Dfabric.gameJarPath={server.resolve()}',
        '-cp', os.pathsep.join(map(str, classpath)),
        'net.fabricmc.loader.impl.launch.server.FabricServerLauncher', 'nogui'],
        cwd=RUNTIME, stdout=log, stderr=subprocess.STDOUT, timeout=180)
output = (LOGS / 'server-cached-smoke.log').read_text(errors='replace')
assert (RUNTIME / 'eula.txt').read_text().strip() == 'eula=false'
report = {'jar': jar.name, 'jar_sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
    'exit_code': result.returncode, 'initializer_executed': "Radi's Construct initialized:" in output,
    'stopped_at_eula_gate': 'You need to agree to the EULA' in output,
    'eula_accepted': False, 'world_started': False, 'client_tested': False}
report['passed'] = result.returncode == 0 and report['initializer_executed'] and report['stopped_at_eula_gate']
(LOGS / 'server-cached-smoke.json').write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
if not report['passed']:
    raise SystemExit('Production cached bootstrap failed; see ci-logs/server-cached-smoke.log')
