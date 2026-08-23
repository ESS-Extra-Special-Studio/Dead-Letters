from pathlib import Path
import re
import shutil

ROOT = Path(r"C:/Users/Ksivi/Desktop/Dead Letters/Stories")
BACKUP_ROOT = ROOT / "_original_combined_backup"
BACKUP_ROOT.mkdir(parents=True, exist_ok=True)

story_files = []
for path in ROOT.glob("*/*.txt"):
    if path.name.lower() == "readme.txt":
        continue
    story_files.append(path)


def split_story(text: str):
    text = text.replace("\r\n", "\n")
    if re.search(r"(?im)^note\s*\d+\s*:", text):
        parts = [p.strip() for p in re.split(r"(?im)^note\s*\d+\s*:\s*", text) if p.strip()]
    else:
        parts = []
        current = []
        for line in text.split("\n"):
            if re.match(r"(?i)^day\s*\d+\s*:", line.strip()) and current:
                chunk = "\n".join(current).strip()
                if chunk:
                    parts.append(chunk)
                current = [line]
            else:
                current.append(line)
        tail = "\n".join(current).strip()
        if tail:
            parts.append(tail)

    formatted = []
    for chunk in parts:
        lines = [ln.rstrip() for ln in chunk.split("\n")]
        lines = [ln for ln in lines if ln.strip()]
        if not lines:
            continue
        title = lines[0].strip()
        body = lines[1:]
        content = "\n".join([title, ""] + body).strip() + "\n"
        formatted.append(content)
    return formatted


for story_file in story_files:
    folder = story_file.parent
    text = story_file.read_text(encoding="utf-8")
    notes = split_story(text)
    if not notes:
        continue

    backup_file = BACKUP_ROOT / f"{folder.name}.txt"
    shutil.copy2(story_file, backup_file)

    for old_part in folder.glob("part_*.txt"):
        old_part.unlink()

    for idx, note in enumerate(notes, start=1):
        (folder / f"part_{idx}.txt").write_text(note, encoding="utf-8")

    story_file.unlink()

print(f"Restructured {len(story_files)} stories into part_N.txt files.")
