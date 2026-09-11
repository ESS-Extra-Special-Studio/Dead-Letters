# Dead Letters

Notes / scrapbook content for the Dead Air world.

- Version **1.0.4** (Forge 1.20.1) — see `CHANGELOG.md` for unreleased server fixes in this workspace

## Custom stories

Put folders under `config/deadletters/stories/<story_id>/` (path configurable).

Either layout works:

1. **Flat (config-style)**
   - `story.json`
   - `part_1.txt` / `part_1.json`, `part_2…`
2. **Datapack / jar copy**
   - `story.json`
   - `notes/part_1.json`, `notes/part_2.json`, …

`story.json` needs at least `"id"` and `"name"`. Part JSON needs `"title"` and `"body"` (array of lines), same as the datapack notes in the jar.
