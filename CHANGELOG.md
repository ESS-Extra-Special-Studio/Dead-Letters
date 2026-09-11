## 1.0.5

Fixed:
Dedicated servers sync story/note definitions to clients so letters show story name + part instead of plain "Dead Letter", and right-click reading works.
Custom stories under config/deadletters/stories accept jar/datapack layout (notes/part_n.json) as well as flat part_n.txt|json.
Player story progress, notebook archive, and chest-inject flags now survive death/respawn.
Placed letters drop as items when their support is broken (or the block is otherwise removed), not only when a player breaks them by hand.
First Lootr chest guarantee counts toward maxNotesPerChest so it no longer stacks an extra note on top of the normal inject.
Custom story overrides remove prior datapack notes for that story id; part order always follows the part_N filename.
Changed:
Placing letters or the scrapbook requires sneak + right-click by default (placement.notesRequireSneak / placement.scrapbookRequireSneak); plain right-click opens/reads.
Common config comments regrouped like RadioTowers (section banners, start-here loot options, clearer placement/custom-story help); key paths unchanged.

# Changelog

All notable changes to Dead Letters will be documented in this file.

## 1.0.4

- NeoForge 1.21.1 port.
- Requires ExtraSpecialCore 2.0.0+ (`[2.0.0,3.0)`).
- Depends on Lootr, Lootr Liaison, and AzureLib for NeoForge 1.21.1.
- Fixed scrapbook and note reader page arrows on NeoForge (atlas-safe GUI blit; no missing-texture buttons).

## 1.0.3

- **ESS domain swap** â€” Java packages moved from `uk.creatopia.unboundâ€¦` to `uk.co.extraspecialstudioâ€¦`.

## 1.0.2

Update by: Extra_Special_K

Changed:

-Note reader and scrapbook screens migrated to EscScreen anchor layout.
-Title bar, scroll regions, and footer buttons use ESC panel reserves and resize cleanly at any GUI scale.
-Requires ExtraSpecialCore 1.2.0+, range [1.0.0,2.0).

Fixed:

-Scrapbook now paginates multi-page story parts the same way as the note reader â€” all pages of a part are shown when cycling, not just the first page.

## 1.0.1

Update by: Extra_Special_K

Changed:

-Integrated ExtraSpecialCore (ESC) as a required dependency for shared UI building blocks.
-Updated note reader screen integration to use ESC panel/button helpers so UI behavior and styling stay aligned with the rest of the mod suite.
-Release/version bump for ESC-compatible pack testing.

