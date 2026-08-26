package uk.co.extraspecialstudio.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.block.PlacedScrapbookBlockEntity;
import uk.co.extraspecialstudio.network.DeadLettersNetwork;
import uk.co.extraspecialstudio.network.RemoveNoteFromNotebookPacket;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.StoryDefinition;
import uk.co.extraspecialstudio.story.StoryPageTextures;
import uk.co.extraspecialstudio.story.StoryRegistry;
import uk.co.extraspecialstudio.story.StoryRegistrySnapshot;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscAnchor;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscLayoutSpec;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscPanel;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscRect;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscScreen;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscText;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScrapbookScreen extends EscScreen {
    private static final int TEX_W = 163;
    private static final int TEX_H = 216;
    private static final float SCALE = 1.32F;
    private static final int MARGIN_BOTTOM = 26;
    private static final int BOTTOM_CONTROLS_TEX = 14;
    /** Nudge index column slightly toward the spiral (texture px, scaled in screen space). */
    private static final int INDEX_COLUMN_SHIFT_TEX = -4;
    /** Extra column width for index wrapping so long titles rarely need ellipsis. */
    private static final int INDEX_EXTRA_WIDTH_TEX = 6;

    private static final ResourceLocation SCRAPBOOK_ARROW_LEFT = ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "textures/gui/scrapbook_arrow_left.png");
    private static final ResourceLocation SCRAPBOOK_ARROW_RIGHT = ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "textures/gui/scrapbook_arrow_right.png");
    /** Hit box / blit size for page arrows (texture is 20×40: normal + hover rows). */
    private static final int PAGE_ARROW_SIZE = 20;
    /** Same gap from parchment vertical edge to the nearest arrow edge on both sides. */
    private static final int PAGE_ARROW_GAP = 12;

    private static final List<String> GUIDE_PAGE_ONE = List.of(
            "Dead Letters is a story collection mod.",
            "",
            "Find note fragments in loot chests.",
            "Read them to unlock progression.",
            "",
            "Use Add to Notebook in a note",
            "to archive a permanent copy here."
    );
    private static final List<String> GUIDE_PAGE_TWO = List.of(
            "Notebook tips:",
            "",
            "- Index can span several pages.",
            "- Stories stay grouped by title.",
            "- Parts are sorted in order.",
            "",
            "Keep collecting fragments to",
            "grow your archive."
    );

    private final ItemStack scrapbookStack;
    @Nullable
    private final PlacedScrapbookBlockEntity scrapbookBlockEntity;
    private List<String> notebookNoteIds = new ArrayList<>();
    private List<PageData> pageData = List.of();
    private List<List<FormattedCharSequence>> renderedPages = List.of();
    private int pageIndex;

    private GuiSheetButton prevPageButton;
    private GuiSheetButton nextPageButton;
    private Button removeNoteButton;
    private final List<AbstractWidget> indexWidgets = new ArrayList<>();

    public ScrapbookScreen(ItemStack scrapbookStack, @Nullable PlacedScrapbookBlockEntity scrapbookBlockEntity) {
        super(Component.literal("Scrapbook"));
        this.scrapbookStack = scrapbookStack;
        this.scrapbookBlockEntity = scrapbookBlockEntity;
    }

    public void updateNotebookEntries(List<String> noteIds) {
        this.notebookNoteIds = new ArrayList<>(noteIds);
        rebuildContent();
        this.pageIndex = Math.min(this.pageIndex, Math.max(0, this.renderedPages.size() - 1));
        if (this.minecraft != null) {
            buildLayout();
        }
    }

    @Override
    protected void init() {
        rebuildContent();
        super.init();
    }

    @Override
    protected void buildLayout() {
        layoutScrapbookControls();
    }

    private EscRect pageBounds() {
        int scaledW = scaledPageWidth();
        int scaledH = scaledPageHeight();
        EscRect content = contentRect();
        return resolve(content, EscLayoutSpec.of(EscAnchor.CENTER, 0, 0, scaledW, scaledH));
    }

    private void layoutScrapbookControls() {
        if (this.prevPageButton != null) {
            this.removeWidget(this.prevPageButton);
        }
        if (this.nextPageButton != null) {
            this.removeWidget(this.nextPageButton);
        }
        if (this.removeNoteButton != null) {
            this.removeWidget(this.removeNoteButton);
        }

        EscRect page = pageBounds();
        int scaledW = page.width();
        int btnH = 18;
        int bottomInset = Math.max(8, (int) (6 * SCALE));

        EscRect prevBounds = resolve(page, EscLayoutSpec.of(
                EscAnchor.CENTER_LEFT, -(PAGE_ARROW_GAP + PAGE_ARROW_SIZE), 0, PAGE_ARROW_SIZE, PAGE_ARROW_SIZE));
        this.prevPageButton = new GuiSheetButton(
                prevBounds.x(),
                prevBounds.y(),
                PAGE_ARROW_SIZE,
                SCRAPBOOK_ARROW_LEFT,
                () -> setPage(pageIndex - 1),
                Component.translatable("gui.dead_letters.scrapbook.page_prev"));

        EscRect nextBounds = resolve(page, EscLayoutSpec.of(
                EscAnchor.CENTER_RIGHT, PAGE_ARROW_GAP + PAGE_ARROW_SIZE, 0, PAGE_ARROW_SIZE, PAGE_ARROW_SIZE));
        this.nextPageButton = new GuiSheetButton(
                nextBounds.x(),
                nextBounds.y(),
                PAGE_ARROW_SIZE,
                SCRAPBOOK_ARROW_RIGHT,
                () -> setPage(pageIndex + 1),
                Component.translatable("gui.dead_letters.scrapbook.page_next"));

        int removeLabelW = this.font.width("Take Note");
        int removeW = Math.min(Math.max(110, removeLabelW + 12), scaledW - 14);
        this.removeNoteButton = addAnchoredButton(
                Component.literal("Take Note"),
                page,
                EscLayoutSpec.of(EscAnchor.BOTTOM_CENTER, 0, -bottomInset, removeW, btnH),
                btn -> {
                    PageData current = currentPageData();
                    if (current == null || current.noteId() == null || current.noteId().isBlank()) {
                        return;
                    }
                    DeadLettersNetwork.sendToServer(new RemoveNoteFromNotebookPacket(current.noteId()));
                });

        this.addRenderableWidget(this.prevPageButton);
        this.addRenderableWidget(this.nextPageButton);
        refreshIndexButtons();
        updateButtons();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        rebuildContent();
        this.pageIndex = Math.min(this.pageIndex, Math.max(0, this.renderedPages.size() - 1));
        super.resize(minecraft, width, height);
    }

    @Override
    public void onClose() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (scrapbookBlockEntity != null && !scrapbookBlockEntity.isRemoved()) {
                ScrapbookAnimationController.playClose(mc.player, scrapbookBlockEntity);
            } else if (!scrapbookStack.isEmpty()) {
                ScrapbookAnimationController.playClose(mc.player, scrapbookStack);
            }
        }
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        EscRect panel = contentRect();
        EscPanel.renderPanel(graphics, panel, style);

        EscRect page = pageBounds();
        int left = page.x();
        int top = page.y();
        int scaledW = page.width();
        int scaledH = page.height();

        StoryRegistrySnapshot snapshot = StoryRegistry.snapshot();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        String storyIdForTexture = null;
        if (pageIndex >= 0 && pageIndex < pageData.size()) {
            storyIdForTexture = pageData.get(pageIndex).storyId();
        }
        ResourceLocation pageTexture = StoryPageTextures.resolveGuiPageTexture(storyIdForTexture, snapshot, resourceManager);

        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0);
        graphics.pose().scale(SCALE, SCALE, 1.0F);
        graphics.blit(pageTexture, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
        graphics.pose().popPose();

        StoryPageTextures.ParchmentTextLayout layout = StoryPageTextures.layoutForStoryPage(storyIdForTexture, pageTexture, snapshot, SCALE);
        int bodyX;
        if (pageIndex >= 0 && pageIndex < pageData.size() && pageData.get(pageIndex).indexOnly()) {
            bodyX = indexTextColumnLeftScreen(left, snapshot, resourceManager);
        } else {
            bodyX = textColumnLeftScreen(left, storyIdForTexture, pageTexture, snapshot);
        }
        int bodyY = top + (int) (layout.marginTopTex() * SCALE);
        int color = 0x1E1A17;

        List<FormattedCharSequence> current = renderedPages.isEmpty() ? List.of() : renderedPages.get(pageIndex);
        int lineStep = layout.lineStepScreen();
        for (int i = 0; i < current.size(); i++) {
            graphics.drawString(this.font, current.get(i), bodyX, bodyY + (i * lineStep), color, false);
        }

        if (renderedPages.size() > 1) {
            String label = (pageIndex + 1) + "/" + renderedPages.size();
            int labelW = this.font.width(label);
            EscText.drawString(graphics, this.font, label, left + (scaledW - labelW) / 2, top + scaledH - (int) (12 * SCALE), color, false, uk.co.extraspecialstudio.extraspecial.esc.ui.EscFonts.DEFAULT);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void rebuildContent() {
        StoryRegistrySnapshot snapshot = StoryRegistry.snapshot();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        this.pageData = buildPageData(snapshot, notebookNoteIds, resourceManager, this.font);

        List<List<FormattedCharSequence>> built = new ArrayList<>();
        for (PageData page : pageData) {
            ResourceLocation tex = StoryPageTextures.resolveGuiPageTexture(page.storyId(), snapshot, resourceManager);
            StoryPageTextures.ParchmentTextLayout layout = StoryPageTextures.layoutForStoryPage(page.storyId(), tex, snapshot, SCALE);
            int lineWidth = page.indexOnly()
                    ? indexTextColumnWidthScreen(snapshot, resourceManager)
                    : textColumnWidthScreen(page.storyId(), tex, snapshot);
            int linesPerPage = bodyLinesForLayout(layout, page.noteId() != null);
            built.add(buildRenderedPage(page, lineWidth, linesPerPage));
        }
        this.renderedPages = built;

        if (this.renderedPages.isEmpty()) {
            this.renderedPages = List.of(List.of(FormattedCharSequence.EMPTY));
        }
    }

    private static List<PageData> buildPageData(StoryRegistrySnapshot snapshot, List<String> notebookIds, ResourceManager resourceManager, Font font) {
        List<NoteDefinition> notes = notebookIds.stream()
                .map(id -> snapshot.notesById().get(id))
                .filter(note -> note != null)
                .distinct()
                .sorted(Comparator
                        .comparing((NoteDefinition note) -> {
                            StoryDefinition story = snapshot.storiesById().get(note.storyId());
                            return story != null ? story.name() : note.storyId();
                        }, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(NoteDefinition::order)
                        .thenComparing(NoteDefinition::id))
                .toList();

        List<PageData> middle = new ArrayList<>();
        middle.add(new PageData("Dead Letters Guide", GUIDE_PAGE_ONE, null, null, false, List.of(), null));
        middle.add(new PageData("Dead Letters Guide", GUIDE_PAGE_TWO, null, null, false, List.of(), null));
        for (NoteDefinition note : notes) {
            StoryDefinition story = snapshot.storiesById().get(note.storyId());
            String storyName = story != null ? story.name() : note.storyId();
            String title = storyName + " Part " + note.order();
            middle.add(new PageData(title, note.body(), note.storyId(), note.id(), false, List.of(), null));
        }
        middle = expandPaginatedMiddle(middle, snapshot, resourceManager, font);

        int indexPagesGuess = 1;
        List<List<IndexEntry>> sheets = List.of(List.of());
        for (int guard = 0; guard < 32; guard++) {
            List<IndexEntry> flat = buildFlatIndexEntries(snapshot, middle, indexPagesGuess);
            int bodyW = indexTextColumnWidthScreenStatic(snapshot, resourceManager);
            int lineBudget = Math.max(1, maxIndexBodyLines(snapshot, resourceManager) - 1);
            sheets = partitionIndexEntries(flat, bodyW, lineBudget, font);
            int need = Math.max(1, sheets.size());
            if (need == indexPagesGuess) {
                break;
            }
            indexPagesGuess = need;
        }

        List<PageData> pages = new ArrayList<>();
        for (int s = 0; s < sheets.size(); s++) {
            String title = sheets.size() == 1 ? "Index" : "Index (" + (s + 1) + ")";
            pages.add(new PageData(title, List.of(), null, null, true, List.copyOf(sheets.get(s)), null));
        }
        pages.addAll(middle);
        return pages;
    }

    private static List<IndexEntry> buildFlatIndexEntries(StoryRegistrySnapshot snapshot, List<PageData> middle, int indexSheetCount) {
        List<IndexEntry> list = new ArrayList<>();
        list.add(new IndexEntry("Guide", indexSheetCount));
        Map<String, Integer> firstPageByStory = new LinkedHashMap<>();
        for (int i = 0; i < middle.size(); i++) {
            PageData p = middle.get(i);
            if (p.storyId() != null) {
                firstPageByStory.putIfAbsent(p.storyId(), indexSheetCount + i);
            }
        }
        for (Map.Entry<String, Integer> entry : firstPageByStory.entrySet()) {
            StoryDefinition story = snapshot.storiesById().get(entry.getKey());
            String label = story != null ? story.name() : entry.getKey();
            list.add(new IndexEntry(label, entry.getValue()));
        }
        return list;
    }

    private static List<List<IndexEntry>> partitionIndexEntries(List<IndexEntry> entries, int bodyWidth, int maxLinesPerSheet, Font font) {
        List<List<IndexEntry>> sheets = new ArrayList<>();
        List<IndexEntry> current = new ArrayList<>();
        int usedLines = 0;
        for (IndexEntry e : entries) {
            int need = wrappedIndexEntryLineCount(e, bodyWidth, font);
            if (need > maxLinesPerSheet) {
                if (!current.isEmpty()) {
                    sheets.add(current);
                    current = new ArrayList<>();
                    usedLines = 0;
                }
                sheets.add(List.of(e));
                continue;
            }
            if (!current.isEmpty() && usedLines + need > maxLinesPerSheet) {
                sheets.add(current);
                current = new ArrayList<>();
                usedLines = 0;
            }
            current.add(e);
            usedLines += need;
        }
        if (!current.isEmpty()) {
            sheets.add(current);
        }
        if (sheets.isEmpty()) {
            sheets.add(new ArrayList<>());
        }
        return sheets;
    }

    private static int wrappedIndexEntryLineCount(IndexEntry entry, int bodyWidth, Font font) {
        return buildDisplayLinesForIndexEntry(entry, bodyWidth, font).size();
    }

    /**
     * First line uses "- title"; continuation lines are indented with spaces only (no hyphen).
     * Wraps on word boundaries so a word is never split across lines (overlong single words stay on one line).
     */
    private static List<String> buildDisplayLinesForIndexEntry(IndexEntry entry, int bodyWidth, Font font) {
        String label = entry.label().trim();
        String prefix = "- ";
        String indent = "  ";
        if (font.width(prefix + label) <= bodyWidth) {
            return new ArrayList<>(List.of(prefix + label));
        }
        List<String> words = new ArrayList<>();
        for (String w : label.split("\\s+")) {
            if (!w.isEmpty()) {
                words.add(w);
            }
        }
        if (words.isEmpty()) {
            return new ArrayList<>(List.of(prefix));
        }
        int firstInner = Math.max(1, bodyWidth - font.width(prefix));
        int contInner = Math.max(1, bodyWidth - font.width(indent));
        List<String> innerLines = wrapWordsToLines(words, firstInner, contInner, font);
        List<String> out = new ArrayList<>();
        out.add(prefix + innerLines.get(0));
        for (int i = 1; i < innerLines.size(); i++) {
            out.add(indent + innerLines.get(i));
        }
        return out;
    }

    /** Builds text lines without leading "- " / indent; first line uses {@code firstMaxWidth}, later lines {@code contMaxWidth}. */
    private static List<String> wrapWordsToLines(List<String> words, int firstMaxWidth, int contMaxWidth, Font font) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int maxW = firstMaxWidth;

        for (String word : words) {
            String trial = cur.isEmpty() ? word : cur + " " + word;
            if (font.width(trial) <= maxW) {
                if (!cur.isEmpty()) {
                    cur.append(' ');
                }
                cur.append(word);
                continue;
            }
            if (cur.length() > 0) {
                lines.add(cur.toString());
                cur.setLength(0);
                maxW = contMaxWidth;
            }
            if (font.width(word) <= maxW) {
                cur.append(word);
            } else {
                lines.add(word);
            }
        }
        if (cur.length() > 0) {
            lines.add(cur.toString());
        }
        return lines;
    }

    private static List<PageData> expandPaginatedMiddle(
            List<PageData> middle,
            StoryRegistrySnapshot snapshot,
            ResourceManager resourceManager,
            Font font
    ) {
        List<PageData> expanded = new ArrayList<>();
        for (PageData page : middle) {
            ResourceLocation tex = StoryPageTextures.resolveGuiPageTexture(page.storyId(), snapshot, resourceManager);
            StoryPageTextures.ParchmentTextLayout layout = StoryPageTextures.layoutForStoryPage(page.storyId(), tex, snapshot, SCALE);
            int lineWidth = textColumnWidthScreen(page.storyId(), tex, snapshot);
            int linesPerPage = bodyLinesForLayoutStatic(layout, page.noteId() != null);
            List<FormattedCharSequence> allLines = collectBodyLines(page, lineWidth, font);
            if (allLines.size() <= linesPerPage) {
                expanded.add(page);
                continue;
            }
            for (int i = 0; i < allLines.size(); i += linesPerPage) {
                int end = Math.min(allLines.size(), i + linesPerPage);
                expanded.add(new PageData(
                        i == 0 ? page.title() : "",
                        List.of(),
                        page.storyId(),
                        page.noteId(),
                        false,
                        List.of(),
                        new ArrayList<>(allLines.subList(i, end))
                ));
            }
        }
        return expanded;
    }

    private static List<FormattedCharSequence> collectBodyLines(PageData page, int lineWidth, Font font) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(font.split(Component.literal(page.title()), lineWidth));
        lines.add(FormattedCharSequence.EMPTY);
        for (String rawLine : page.lines()) {
            if (rawLine.isBlank()) {
                lines.add(FormattedCharSequence.EMPTY);
            } else {
                lines.addAll(font.split(Component.literal(rawLine), lineWidth));
            }
        }
        return lines;
    }

    private static int bodyLinesForLayoutStatic(StoryPageTextures.ParchmentTextLayout layout, boolean reserveControls) {
        int scaledH = scaledPageHeight();
        int topPad = (int) (layout.marginTopTex() * SCALE);
        int bottomPad = (int) (MARGIN_BOTTOM * SCALE);
        int controls = reserveControls ? (int) (BOTTOM_CONTROLS_TEX * SCALE) : 0;
        return Math.max(4, (scaledH - topPad - bottomPad - controls) / layout.lineStepScreen());
    }

    private static List<FormattedCharSequence> buildRenderedPage(PageData page, int lineWidth, int linesPerPage) {
        if (page.prebuiltLines() != null) {
            return new ArrayList<>(page.prebuiltLines());
        }
        Minecraft mc = Minecraft.getInstance();
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.addAll(mc.font.split(Component.literal(page.title()), lineWidth));
        if (page.indexOnly()) {
            if (lines.size() > linesPerPage) {
                return new ArrayList<>(lines.subList(0, linesPerPage));
            }
            return lines;
        }
        lines.add(FormattedCharSequence.EMPTY);

        for (String rawLine : page.lines()) {
            if (rawLine.isBlank()) {
                lines.add(FormattedCharSequence.EMPTY);
            } else {
                lines.addAll(mc.font.split(Component.literal(rawLine), lineWidth));
            }
        }

        if (lines.size() > linesPerPage) {
            return new ArrayList<>(lines.subList(0, linesPerPage));
        }
        return lines;
    }

    private void refreshIndexButtons() {
        for (AbstractWidget w : indexWidgets) {
            this.removeWidget(w);
        }
        indexWidgets.clear();

        PageData page = currentPageData();
        if (page == null || !page.indexOnly() || page.indexSheet().isEmpty()) {
            return;
        }

        EscRect pageRect = pageBounds();
        int left = pageRect.x();
        int top = pageRect.y();
        StoryRegistrySnapshot snapshot = StoryRegistry.snapshot();
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        ResourceLocation indexTex = StoryPageTextures.resolveGuiPageTexture(null, snapshot, rm);
        StoryPageTextures.ParchmentTextLayout idxLayout = StoryPageTextures.layoutForStoryPage(null, indexTex, snapshot, SCALE);
        int bodyX = indexTextColumnLeftScreen(left, snapshot, rm);
        int bodyY = top + (int) (idxLayout.marginTopTex() * SCALE);
        int bodyWidth = indexTextColumnWidthScreen(snapshot, rm);
        int lineStep = idxLayout.lineStepScreen();

        int y = bodyY + lineStep;
        for (IndexEntry entry : page.indexSheet()) {
            List<String> displayLines = buildDisplayLinesForIndexEntry(entry, bodyWidth, this.font);
            int blockH = displayLines.size() * lineStep;
            IndexEntryBlockWidget w = new IndexEntryBlockWidget(bodyX, y, bodyWidth, blockH, lineStep, displayLines, entry.targetPage());
            indexWidgets.add(w);
            this.addRenderableWidget(w);
            y += blockH;
        }
    }

    private void setPage(int targetPage) {
        int clamped = Math.max(0, Math.min(targetPage, renderedPages.size() - 1));
        if (clamped == pageIndex) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (scrapbookBlockEntity != null && !scrapbookBlockEntity.isRemoved()) {
                if (clamped > pageIndex) {
                    ScrapbookAnimationController.playPageForward(mc.player, scrapbookBlockEntity);
                } else {
                    ScrapbookAnimationController.playPageBackward(mc.player, scrapbookBlockEntity);
                }
            } else if (!scrapbookStack.isEmpty()) {
                if (clamped > pageIndex) {
                    ScrapbookAnimationController.playPageForward(mc.player, scrapbookStack);
                } else {
                    ScrapbookAnimationController.playPageBackward(mc.player, scrapbookStack);
                }
            }
        }

        pageIndex = clamped;
        refreshIndexButtons();
        updateButtons();
    }

    private void updateButtons() {
        boolean multi = renderedPages.size() > 1;
        if (prevPageButton != null) {
            prevPageButton.visible = multi && pageIndex > 0;
            prevPageButton.active = pageIndex > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.visible = multi && pageIndex < renderedPages.size() - 1;
            nextPageButton.active = pageIndex < renderedPages.size() - 1;
        }
        if (removeNoteButton != null) {
            PageData current = currentPageData();
            boolean canTake = current != null && !current.indexOnly() && current.noteId() != null && !current.noteId().isBlank();
            removeNoteButton.visible = canTake;
            removeNoteButton.active = canTake;
        }
    }

    @Nullable
    private PageData currentPageData() {
        if (pageIndex < 0 || pageIndex >= pageData.size()) {
            return null;
        }
        return pageData.get(pageIndex);
    }

    private static int maxIndexBodyLines(StoryRegistrySnapshot snapshot, ResourceManager rm) {
        ResourceLocation indexTex = StoryPageTextures.resolveGuiPageTexture(null, snapshot, rm);
        StoryPageTextures.ParchmentTextLayout layout = StoryPageTextures.layoutForStoryPage(null, indexTex, snapshot, SCALE);
        int scaledH = scaledPageHeight();
        int topPad = (int) (layout.marginTopTex() * SCALE);
        int bottomPad = (int) (MARGIN_BOTTOM * SCALE);
        return Math.max(4, (scaledH - topPad - bottomPad) / layout.lineStepScreen());
    }

    private int indexTextColumnLeftScreen(int pageLeft, StoryRegistrySnapshot snapshot, ResourceManager rm) {
        ResourceLocation indexTex = StoryPageTextures.resolveGuiPageTexture(null, snapshot, rm);
        return textColumnLeftScreen(pageLeft, null, indexTex, snapshot) + (int) (INDEX_COLUMN_SHIFT_TEX * SCALE);
    }

    private int indexTextColumnWidthScreen(StoryRegistrySnapshot snapshot, ResourceManager rm) {
        return indexTextColumnWidthScreenStatic(snapshot, rm);
    }

    private static int indexTextColumnWidthScreenStatic(StoryRegistrySnapshot snapshot, ResourceManager rm) {
        ResourceLocation indexTex = StoryPageTextures.resolveGuiPageTexture(null, snapshot, rm);
        return textColumnWidthScreen(null, indexTex, snapshot) + (int) (INDEX_EXTRA_WIDTH_TEX * SCALE);
    }

    /** Screen X of the text column left edge; matches {@link StoryPageTextures.ParchmentTextLayout} for the page texture. */
    private static int textColumnLeftScreen(int pageLeft, String storyId, ResourceLocation pageTexture, StoryRegistrySnapshot snapshot) {
        StoryPageTextures.ParchmentTextLayout layout = StoryPageTextures.layoutForStoryPage(storyId, pageTexture, snapshot, SCALE);
        return pageLeft + (int) (layout.marginLeftTex() * SCALE);
    }

    /** Screen width for wrapping; matches layout inner width. */
    private static int textColumnWidthScreen(String storyId, ResourceLocation pageTexture, StoryRegistrySnapshot snapshot) {
        StoryPageTextures.ParchmentTextLayout layout = StoryPageTextures.layoutForStoryPage(storyId, pageTexture, snapshot, SCALE);
        return layout.innerWidthPixels(SCALE);
    }

    private int bodyLinesForLayout(StoryPageTextures.ParchmentTextLayout layout, boolean reserveControls) {
        int scaledH = scaledPageHeight();
        int topPad = (int) (layout.marginTopTex() * SCALE);
        int bottomPad = (int) (MARGIN_BOTTOM * SCALE);
        int controls = reserveControls ? (int) (BOTTOM_CONTROLS_TEX * SCALE) : 0;
        return Math.max(4, (scaledH - topPad - bottomPad - controls) / layout.lineStepScreen());
    }

    private static int scaledPageWidth() {
        return (int) (TEX_W * SCALE);
    }

    private static int scaledPageHeight() {
        return (int) (TEX_H * SCALE);
    }

    private record PageData(
            String title,
            List<String> lines,
            @Nullable String storyId,
            @Nullable String noteId,
            boolean indexOnly,
            List<IndexEntry> indexSheet,
            @Nullable List<FormattedCharSequence> prebuiltLines
    ) {
    }

    private record IndexEntry(String label, int targetPage) {
    }

    /**
     * Clickable wrapped index row(s); first line may start with "- ", continuations are space-indented only.
     */
    private final class IndexEntryBlockWidget extends AbstractWidget {
        private static final int TEXT_COLOR = 0x1E1A17;
        private static final int TEXT_HOVER = 0xFFF5E6C8;
        private static final int HOLO_DIM = 0xFF4A3D2E;
        private final List<String> lines;
        private final int lineStepPx;
        private final int targetPageIndex;

        IndexEntryBlockWidget(int x, int y, int width, int height, int lineStepPx, List<String> lines, int targetPageIndex) {
            super(x, y, width, height, Component.empty());
            this.lines = lines;
            this.lineStepPx = lineStepPx;
            this.targetPageIndex = targetPageIndex;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Font f = ScrapbookScreen.this.font;
            int ly = getY();
            boolean hi = isHovered();
            for (String line : lines) {
                if (hi) {
                    graphics.drawString(f, line, getX() - 1, ly, HOLO_DIM, false);
                    graphics.drawString(f, line, getX() + 1, ly, HOLO_DIM, false);
                    graphics.drawString(f, line, getX(), ly - 1, HOLO_DIM, false);
                    graphics.drawString(f, line, getX(), ly + 1, HOLO_DIM, false);
                    graphics.drawString(f, line, getX(), ly, TEXT_HOVER, false);
                    int lw = f.width(line);
                    graphics.fill(getX(), ly + f.lineHeight - 1, getX() + lw, ly + f.lineHeight, 0xA0C9A575);
                } else {
                    graphics.drawString(f, line, getX(), ly, TEXT_COLOR, false);
                }
                ly += lineStepPx;
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            ScrapbookScreen.this.setPage(targetPageIndex);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }
}
