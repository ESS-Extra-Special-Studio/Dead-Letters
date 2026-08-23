from pathlib import Path
import json
import re
import shutil

ROOT = Path(r"C:/Users/Ksivi/IdeaProjects/Dead Letters")
STORIES_SRC = Path(r"C:/Users/Ksivi/Desktop/Dead Letters/Stories")
DATAPACK_OUT = ROOT / "src/main/resources/data/dead_letters/deadletters/stories"
DATAPACK_OUT.mkdir(parents=True, exist_ok=True)

texture_src = Path(r"C:/Users/Ksivi/Downloads/book.png")
texture_dst = ROOT / "src/main/resources/assets/dead_letters/textures/item/note.png"
texture_dst.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(texture_src, texture_dst)

STORY_MAP = {
    "Code Zero": ("code_zero", "Code Zero", 10),
    "The Final Broadcast": ("final_broadcast", "The Final Broadcast", 10),
    "The Final Heartbeat": ("final_heartbeat", "The Final Heartbeat", 10),
    "The Red Horizon Protocol": ("red_horizon_protocol", "The Red Horizon Protocol", 10),
    "The Silent Playground": ("silent_playground", "The Silent Playground", 10),
}


def parse_story_text(text: str):
    text = text.replace("\r\n", "\n")
    if re.search(r"(?im)^note\s*\d+\s*:", text):
        chunks = []
        parts = re.split(r"(?im)^note\s*\d+\s*:\s*", text)
        for part in parts:
            stripped = part.strip()
            if stripped:
                chunks.append(stripped)
    else:
        chunks = []
        current = []
        for line in text.split("\n"):
            if re.match(r"(?i)^day\s*\d+\s*:", line.strip()) and current:
                chunks.append("\n".join(current).strip())
                current = [line]
            else:
                current.append(line)
        tail = "\n".join(current).strip()
        if tail:
            chunks.append(tail)

    notes = []
    for index, chunk in enumerate(chunks, start=1):
        lines = [line.rstrip() for line in chunk.split("\n")]
        lines = [line for line in lines if line.strip() != ""]
        if not lines:
            continue
        title = lines[0].strip()
        body = lines[1:] if len(lines) > 1 else ["..."]
        notes.append((index, title, body))
    return notes


for folder, (story_id, story_name, story_weight) in STORY_MAP.items():
    src_file = STORIES_SRC / folder / f"{folder}.txt"
    story_text = src_file.read_text(encoding="utf-8")
    notes = parse_story_text(story_text)

    story_dir = DATAPACK_OUT / story_id
    notes_dir = story_dir / "notes"
    notes_dir.mkdir(parents=True, exist_ok=True)

    story_json = {
        "id": story_id,
        "name": story_name,
        "max_order": len(notes),
        "weight": story_weight,
    }
    (story_dir / "story.json").write_text(json.dumps(story_json, indent=2), encoding="utf-8")

    for order, title, body in notes:
        note_id = f"{story_id}_{order:02d}"
        note_json = {
            "id": note_id,
            "story": story_id,
            "order": order,
            "title": title,
            "body": body,
            "weight": 10,
        }
        (notes_dir / f"part_{order}.json").write_text(json.dumps(note_json, indent=2), encoding="utf-8")

EXAMPLE_ROOT = ROOT / "examples/custom-stories"
EXAMPLE_ROOT.mkdir(parents=True, exist_ok=True)

for folder, (story_id, story_name, story_weight) in STORY_MAP.items():
    src_file = STORIES_SRC / folder / f"{folder}.txt"
    story_text = src_file.read_text(encoding="utf-8")
    notes = parse_story_text(story_text)

    story_dir = EXAMPLE_ROOT / story_name
    story_dir.mkdir(parents=True, exist_ok=True)
    story_json = {
        "id": story_id,
        "name": story_name,
        "weight": story_weight,
        "max_order": len(notes),
    }
    (story_dir / "story.json").write_text(json.dumps(story_json, indent=2), encoding="utf-8")

    for order, title, body in notes:
        contents = "\n".join([title, ""] + body) + "\n"
        (story_dir / f"part_{order}.txt").write_text(contents, encoding="utf-8")

(EXAMPLE_ROOT / "README.txt").write_text(
    "Copy these folders into config/deadletters/stories/ then restart or run /reload.\n"
    "Each story folder contains story.json + part_N.txt files.\n",
    encoding="utf-8",
)

print("Imported stories and copied note texture.")
