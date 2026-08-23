# Changelog

All notable changes to Dead Letters will be documented in this file.

## 1.0.3

- **ESS domain swap** — Java packages moved from `uk.creatopia.unbound…` to `uk.co.extraspecialstudio…`.

## 1.0.2

Update by: Extra_Special_K

Changed:

-Note reader and scrapbook screens migrated to EscScreen anchor layout.
-Title bar, scroll regions, and footer buttons use ESC panel reserves and resize cleanly at any GUI scale.
-Requires ExtraSpecialCore 1.2.0+, range [1.0.0,2.0).

Fixed:

-Scrapbook now paginates multi-page story parts the same way as the note reader — all pages of a part are shown when cycling, not just the first page.

## 1.0.1

Update by: Extra_Special_K

Changed:

-Integrated ExtraSpecialCore (ESC) as a required dependency for shared UI building blocks.
-Updated note reader screen integration to use ESC panel/button helpers so UI behavior and styling stay aligned with the rest of the mod suite.
-Release/version bump for ESC-compatible pack testing.
