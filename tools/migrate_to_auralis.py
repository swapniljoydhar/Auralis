from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
TEXT_SUFFIXES = {'.kt', '.java', '.xml', '.gradle', '.properties', '.md', '.yml', '.yaml', '.json', '.txt'}
OLD_PACKAGE = 'org.oxycblt.auxio'
NEW_PACKAGE = 'com.auralis.player'


def tracked_files():
    result = subprocess.run(
        ['git', 'ls-files', '-z'],
        cwd=ROOT,
        check=True,
        stdout=subprocess.PIPE,
    )
    return [ROOT / name for name in result.stdout.decode().split('\0') if name]


def rewrite_source(text: str, suffix: str) -> str:
    # Preserve the leading copyright/license block verbatim. It is provenance,
    # not product UI, and must remain accurate for GPL-derived files.
    preserved = ''
    body = text
    if suffix in {'.kt', '.java'} and text.startswith('/*'):
        end = text.find('*/')
        if end != -1:
            end += 2
            preserved = text[:end]
            body = text[end:]

    body = body.replace(OLD_PACKAGE, NEW_PACKAGE)
    body = body.replace('org/oxycblt/auxio', 'com/auralis/player')
    body = body.replace('Theme.Auxio', 'Theme.Auralis')
    body = body.replace('ic_auxio', 'ic_auralis')
    body = body.replace('AuxioService', 'AuralisService')
    body = body.replace('AuxioRecyclerView', 'AuralisRecyclerView')
    body = body.replace('class Auxio', 'class Auralis')
    body = body.replace('Auxio.INTENT', 'Auralis.INTENT')
    body = body.replace('AuxioService.', 'AuralisService.')
    body = body.replace('Auxio', 'Auralis')
    body = body.replace('auxio', 'auralis')
    body = body.replace('OxygenCobalt/Auxio', 'swapniljoydhar/Auxio')
    return preserved + body


for path in tracked_files():
    if path.suffix not in TEXT_SUFFIXES or not path.is_file():
        continue
    try:
        original = path.read_text(encoding='utf-8')
    except UnicodeDecodeError:
        continue
    updated = rewrite_source(original, path.suffix)
    if updated != original:
        path.write_text(updated, encoding='utf-8')

# Rename source/resource paths after content migration.
renames = []
for path in sorted(tracked_files(), key=lambda p: len(p.parts), reverse=True):
    if not path.exists():
        continue
    rel = path.relative_to(ROOT)
    rel_text = str(rel)
    new_rel_text = rel_text.replace('org/oxycblt/auxio', 'com/auralis/player')
    new_rel_text = new_rel_text.replace('AuxioService', 'AuralisService')
    new_rel_text = new_rel_text.replace('AuxioRecyclerView', 'AuralisRecyclerView')
    new_rel_text = new_rel_text.replace('Auxio.kt', 'Auralis.kt')
    new_rel_text = new_rel_text.replace('Auxio', 'Auralis')
    new_rel_text = new_rel_text.replace('auxio', 'auralis')
    destination = ROOT / new_rel_text
    if destination != path:
        destination.parent.mkdir(parents=True, exist_ok=True)
        path.rename(destination)
        renames.append(f'{rel} -> {destination.relative_to(ROOT)}')

print(f'Renamed {len(renames)} tracked paths')
for item in renames:
    print(item)
