"""Generate Dead Letters page and item icon textures from book.png."""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
BOOK = Path(r"C:/Users/Ksivi/Downloads/book.png")
GUI_OUT = ROOT / "src/main/resources/assets/dead_letters/textures/gui/note_page_clean.png"
ICON_OUT = ROOT / "src/main/resources/assets/dead_letters/textures/item/note_icon.png"

# Source texture is 256x256.
# Clean page crop keeps border while removing stray controls.
PAGE_CROP = (3, 1, 166, 217)
# Inner parchment for icon.
ICON_CROP = (26, 30, 146, 196)
ICON_RENDER_SIZE = (20, 28)
ICON_CANVAS_SIZE = (32, 32)


def main() -> None:
    im = Image.open(BOOK).convert("RGBA")

    page = im.crop(PAGE_CROP)
    GUI_OUT.parent.mkdir(parents=True, exist_ok=True)
    page.save(GUI_OUT)

    inner = im.crop(ICON_CROP)
    icon = inner.resize(ICON_RENDER_SIZE, Image.Resampling.NEAREST)
    canvas = Image.new("RGBA", ICON_CANVAS_SIZE, (0, 0, 0, 0))
    paste_x = (ICON_CANVAS_SIZE[0] - ICON_RENDER_SIZE[0]) // 2
    paste_y = (ICON_CANVAS_SIZE[1] - ICON_RENDER_SIZE[1]) // 2
    canvas.paste(icon, (paste_x, paste_y), icon)
    ICON_OUT.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(ICON_OUT)
    print(f"Wrote {GUI_OUT}")
    print(f"Wrote {ICON_OUT}")


if __name__ == "__main__":
    main()
