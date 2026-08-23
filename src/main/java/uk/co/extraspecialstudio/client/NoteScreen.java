package uk.co.extraspecialstudio.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import uk.co.extraspecialstudio.Dead_letters;
import uk.co.extraspecialstudio.network.AddNoteToNotebookPacket;
import uk.co.extraspecialstudio.network.DeadLettersNetwork;
import uk.co.extraspecialstudio.story.NoteDefinition;
import uk.co.extraspecialstudio.story.StoryPageTextures;
import uk.co.extraspecialstudio.story.StoryRegistry;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscAnchor;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscLayoutSpec;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscPanel;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscRect;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscScreen;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscText;

import java.util.ArrayList;
import java.util.List;

public class NoteScreen extends EscScreen {
    private static final int TEX_W = StoryPageTextures.NOTE_PAGE_TEX_W;
    private static final int TEX_H = StoryPageTextures.NOTE_PAGE_TEX_H;
    private static final float SCALE = 1.32F;
    private static final int MARGIN_BOTTOM = 26;
    private static final int BOTTOM_CONTROLS_TEX = 14;

    private static final ResourceLocation SCRAPBOOK_ARROW_LEFT = ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "textures/gui/scrapbook_arrow_left.png");
    private static final ResourceLocation SCRAPBOOK_ARROW_RIGHT = ResourceLocation.fromNamespaceAndPath(Dead_letters.MODID, "textures/gui/scrapbook_arrow_right.png");
    private static final int PAGE_ARROW_SIZE = 20;
    private static final int PAGE_ARROW_GAP = 12;

    private final NoteDefinition note;
    private ResourceLocation pageTexture = StoryPageTextures.guiFallbackTexture();
    private StoryPageTextures.ParchmentTextLayout parchmentLayout = StoryPageTextures.ParchmentTextLayout.forTexture(pageTexture, SCALE);
    private List<List<FormattedCharSequence>> pages = List.of();
    private int pageIndex = 0;
    private ImageButton prevPageButton;
    private ImageButton nextPageButton;
    private Button addToNotebookButton;

    public NoteScreen(NoteDefinition note) {
        super(Component.literal(note.title() != null ? note.title() : ""));
        this.note = note;
    }

    public String getNoteId() {
        return note.id();
    }

    @Override
    protected void init() {
        this.pageTexture = resolvePageTexture();
        this.parchmentLayout = StoryPageTextures.layoutForStoryPage(note.storyId(), this.pageTexture, StoryRegistry.snapshot(), SCALE);
        rebuildPages();
        super.init();
    }

    @Override
    protected void buildLayout() {
        layoutNoteControls();
    }

    private EscRect pageBounds() {
        int scaledW = scaledPageWidth();
        int scaledH = scaledPageHeight();
        EscRect content = contentRect();
        return resolve(content, EscLayoutSpec.of(EscAnchor.CENTER, 0, 0, scaledW, scaledH));
    }

    private void layoutNoteControls() {
        if (this.prevPageButton != null) {
            this.removeWidget(this.prevPageButton);
        }
        if (this.nextPageButton != null) {
            this.removeWidget(this.nextPageButton);
        }
        if (this.addToNotebookButton != null) {
            this.removeWidget(this.addToNotebookButton);
        }

        EscRect page = pageBounds();
        int scaledW = page.width();
        int btnH = 18;
        int bottomInset = Math.max(8, (int) (6 * SCALE));

        EscRect prevBounds = resolve(page, EscLayoutSpec.of(
                EscAnchor.CENTER_LEFT, -(PAGE_ARROW_GAP + PAGE_ARROW_SIZE), 0, PAGE_ARROW_SIZE, PAGE_ARROW_SIZE));
        this.prevPageButton = new ImageButton(
                prevBounds.x(),
                prevBounds.y(),
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE,
                0,
                0,
                0,
                SCRAPBOOK_ARROW_LEFT,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE,
                btn -> {
                    pageIndex = Math.max(0, pageIndex - 1);
                    updateButtons();
                },
                Component.translatable("gui.dead_letters.scrapbook.page_prev"));

        EscRect nextBounds = resolve(page, EscLayoutSpec.of(
                EscAnchor.CENTER_RIGHT, PAGE_ARROW_GAP + PAGE_ARROW_SIZE, 0, PAGE_ARROW_SIZE, PAGE_ARROW_SIZE));
        this.nextPageButton = new ImageButton(
                nextBounds.x(),
                nextBounds.y(),
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE,
                0,
                0,
                0,
                SCRAPBOOK_ARROW_RIGHT,
                PAGE_ARROW_SIZE,
                PAGE_ARROW_SIZE,
                btn -> {
                    pageIndex = Math.min(pages.size() - 1, pageIndex + 1);
                    updateButtons();
                },
                Component.translatable("gui.dead_letters.scrapbook.page_next"));

        int addLabelW = this.font.width("Add to Notebook");
        int addW = Math.min(Math.max(120, addLabelW + 12), scaledW - 14);
        this.addToNotebookButton = addAnchoredButton(
                Component.literal("Add to Notebook"),
                page,
                EscLayoutSpec.of(EscAnchor.BOTTOM_CENTER, 0, -bottomInset, addW, btnH),
                btn -> DeadLettersNetwork.CHANNEL.sendToServer(new AddNoteToNotebookPacket(note.id())));

        this.addRenderableWidget(this.prevPageButton);
        this.addRenderableWidget(this.nextPageButton);
        updateButtons();
    }

    private ResourceLocation resolvePageTexture() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return StoryPageTextures.guiFallbackTexture();
        }
        return StoryPageTextures.resolveGuiPageTexture(note.storyId(), StoryRegistry.snapshot(), mc.getResourceManager());
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        this.pageTexture = resolvePageTexture();
        this.parchmentLayout = StoryPageTextures.layoutForStoryPage(note.storyId(), this.pageTexture, StoryRegistry.snapshot(), SCALE);
        rebuildPages();
        pageIndex = Math.min(pageIndex, Math.max(0, pages.size() - 1));
        super.resize(minecraft, width, height);
    }

    private void rebuildPages() {
        this.pages = buildPages(this.font, note, getTextWidth(), getBodyLinesPerPage());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        EscRect panel = contentRect();
        EscPanel.renderPanel(graphics, panel, style);

        EscRect page = pageBounds();
        int left = page.x();
        int top = page.y();
        int scaledW = page.width();
        int scaledH = page.height();

        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0);
        graphics.pose().scale(SCALE, SCALE, 1.0F);
        graphics.blit(pageTexture, 0, 0, 0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
        graphics.pose().popPose();

        int bodyX = left + (int) (parchmentLayout.marginLeftTex() * SCALE);
        int bodyY = top + (int) (parchmentLayout.marginTopTex() * SCALE);
        int color = 0x1E1A17;

        List<FormattedCharSequence> current = pages.isEmpty() ? List.of() : pages.get(pageIndex);
        int lineStep = parchmentLayout.lineStepScreen();
        for (int i = 0; i < current.size(); i++) {
            graphics.drawString(this.font, current.get(i), bodyX, bodyY + (i * lineStep), color, false);
        }

        if (pages.size() > 1) {
            String label = (pageIndex + 1) + "/" + pages.size();
            int labelW = this.font.width(label);
            EscText.drawString(graphics, this.font, label, left + (scaledW - labelW) / 2, top + scaledH - (int) (12 * SCALE), color, false, uk.co.extraspecialstudio.extraspecial.esc.ui.EscFonts.DEFAULT);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int getTextWidth() {
        return parchmentLayout.innerWidthPixels(SCALE);
    }

    private int getBodyLinesPerPage() {
        int scaledH = scaledPageHeight();
        int topPad = (int) (parchmentLayout.marginTopTex() * SCALE);
        int bottomPad = (int) (MARGIN_BOTTOM * SCALE);
        int controls = parchmentLayout.lineStepScreen() == 11
                ? (int) (BOTTOM_CONTROLS_TEX * SCALE)
                : (int) Math.max(8, 8 * SCALE);
        return Math.max(4, (scaledH - topPad - bottomPad - controls) / parchmentLayout.lineStepScreen());
    }

    private static int scaledPageWidth() {
        return (int) (TEX_W * SCALE);
    }

    private static int scaledPageHeight() {
        return (int) (TEX_H * SCALE);
    }

    private void updateButtons() {
        boolean multi = pages.size() > 1;
        if (prevPageButton != null) {
            prevPageButton.visible = multi && pageIndex > 0;
            prevPageButton.active = pageIndex > 0;
        }
        if (nextPageButton != null) {
            nextPageButton.visible = multi && pageIndex < pages.size() - 1;
            nextPageButton.active = pageIndex < pages.size() - 1;
        }
    }

    private static List<List<FormattedCharSequence>> buildPages(Font font, NoteDefinition note, int lineWidth, int linesPerPage) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        String title = note.title() != null ? note.title() : "";
        lines.addAll(font.split(EscText.literal(title), lineWidth));
        lines.add(FormattedCharSequence.EMPTY);
        for (String rawLine : note.body()) {
            if (rawLine == null || rawLine.isBlank()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(font.split(EscText.literal(rawLine), lineWidth));
        }

        List<List<FormattedCharSequence>> pages = new ArrayList<>();
        if (lines.isEmpty()) {
            pages.add(List.of(FormattedCharSequence.EMPTY));
            return pages;
        }

        for (int i = 0; i < lines.size(); i += linesPerPage) {
            int end = Math.min(lines.size(), i + linesPerPage);
            pages.add(new ArrayList<>(lines.subList(i, end)));
        }
        return pages;
    }
}
