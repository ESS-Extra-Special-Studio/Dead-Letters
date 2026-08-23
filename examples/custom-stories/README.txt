Copy these folders into config/deadletters/stories/ then restart or run /reload.

Each story folder contains:
- story.json
- part_N.txt OR part_N.json (part_1 required, no gaps)

story.json fields:
- id (required): unique story id, e.g. "my_story"
- name (optional): display name shown in UI
- max_order (optional): max expected part number
- weight (optional): story weight for loot selection
- texture_slot (optional, 1..6): choose one of the built-in provided parchment variants (T01..T06)
- page_texture (optional): full ResourceLocation to a GUI page texture PNG for this story, e.g.
  "my_pack:textures/item/story_pages/my_story_page.png"
- page_texture_lined (optional, true/false): set true if your custom page_texture uses lined-paper
  geometry (red margin + fixed horizontal rules) so text alignment uses lined metrics

Texture behavior summary:
- If page_texture is set and exists in loaded resources, Dead Letters uses it.
- Otherwise it uses built-in dead_letters story pages by slot.
- If texture_slot is set, that slot is used.
- If texture_slot is NOT set, slot is derived from a stable hash of story id (does not shift when
  other stories are added/removed).

Using your own textures:
- Place your texture in a resource pack or mod assets (not in config folder).
- Example path in pack: assets/my_pack/textures/item/story_pages/my_story_page.png
- Then set page_texture to: "my_pack:textures/item/story_pages/my_story_page.png"

Part file formats:
- part_N.txt: first line = title, remaining non-empty lines = body
- part_N.json:
  {
    "id": "my_story_01",      // optional, defaults to storyId_order
    "title": "Part title",    // optional
    "weight": 1,              // optional
    "body": ["line 1", "line 2"]
  }
