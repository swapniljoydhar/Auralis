#!/usr/bin/env python3
"""
Identity scrub: Remove all Auralis/Auralis references from the codebase.
Replaces with Auralis branding throughout.
"""

import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

files_modified = 0
replacements = 0

SKIP_DIRS = {'.git', 'build', '.gradle', '.idea'}
SKIP_EXTS = {'.png', '.jpg', '.jpeg', '.gif', '.webp', '.ico', '.so', '.a', '.jar', '.aar', '.apk', '.class'}

def scrub_text(text):
    global replacements
    original = text

    # 1. Copyright headers: "Auralis Project" → "Auralis Project"
    text, n = re.subn(r'Auralis Project', 'Auralis Project', text)
    replacements += n

    # 2. Author lines
    text, n = re.subn(r'Alexander Capehart \(Auralis\)', 'Auralis Contributors', text)
    replacements += n
    text, n = re.subn(r'@author Auralis Contributors', '@author Auralis Contributors', text)
    replacements += n

    # 3. "Auralis" standalone (in comments, docs, etc.)
    text, n = re.subn(r'Auralis', 'Auralis', text)
    replacements += n

    # 4. "Auralis" in string literals, comments, docs (but NOT in code identifiers)
    #    Match "Auralis" when preceded by non-alphanumeric or start of line
    text, n = re.subn(r'(?<![a-zA-Z0-9_])Auralis(?![a-zA-Z0-9_])', 'Auralis', text)
    replacements += n

    # 5. "auralis" lowercase in identifiers/strings
    text, n = re.subn(r'(?<![a-zA-Z0-9_])auralis(?![a-zA-Z0-9_])', 'auralis', text)
    replacements += n

    # 6. "AURALIS" uppercase
    text, n = re.subn(r'AURALIS', 'AURALIS', text)
    replacements += n

    # 7. GitHub URL update (already done in some files, ensure consistency)
    text, n = re.subn(r'github\.com/Auralis/Auralis', 'github.com/swapniljoydhar/Auralis', text)
    replacements += n

    return text, text != original

def walk_directory(directory):
    global files_modified
    for root, dirs, files in os.walk(directory):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        for fname in files:
            ext = os.path.splitext(fname)[1].lower()
            if ext in SKIP_EXTS:
                continue
            if ext not in ('.kt', '.xml', '.gradle', '.md', '.properties', '.json',
                          '.toml', '.txt', '.pro', '.cfg', '.yml', '.yaml', '.py', '.sh',
                          '.kts'):
                continue
            filepath = os.path.join(root, fname)
            try:
                with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()
            except (IOError, UnicodeDecodeError):
                continue

            new_content, changed = scrub_text(content)
            if changed:
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                files_modified += 1
                rel = os.path.relpath(filepath, ROOT)
                print(f"  scrubbed: {rel}")

if __name__ == "__main__":
    print(f"Starting identity scrub in: {ROOT}")
    walk_directory(ROOT)
    print(f"\nDone! Modified {files_modified} files with {replacements} replacements.")
