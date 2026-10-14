package com.yukar1n.x_tracer.client.gui;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.container.ContainerXTracerStorage;
import com.yukar1n.x_tracer.network.ContainerEditMessage;
import com.yukar1n.x_tracer.network.ContainerContentsMessage;
import com.yukar1n.x_tracer.network.ContainerContentsRequestMessage;
import com.yukar1n.x_tracer.network.ContainerExtractMessage;
import com.yukar1n.x_tracer.storage.RoutingExpression;
import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GuiXTracerStorage extends GuiContainer {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 266;
    private static final int GRID_X = 20;
    private static final int GRID_Y = 56;
    private static final int CELL_SIZE = 20;
    private static final int TEXT = 0xdbe5e9;
    private static final int MUTED = 0x94a6af;
    private static final int ACCENT = 0x63d2df;
    private final TileEntityXTracerContainer tile;
    private GuiTextField search;
    private int searchTicks;
    private int loadingTicks;
    private static int nextRequestId;
    private int requestId;
    private GuiTextField name;
    private GuiTextField accepts;
    private GuiTextField excludes;
    private long revision;
    private boolean rulesReceived;
    private int ruleRequestTicks;
    private boolean settings;
    private boolean saving;
    private int saveTicks;
    private boolean loading;
    private String savedName = "";
    private List<String> acceptValues = new ArrayList<>();
    private List<String> excludeValues = new ArrayList<>();
    private final List<ContainerContentsMessage.Entry> contents = new ArrayList<>();
    private int contentPage;
    private long contentRevision;
    private int totalEntries;
    private final RenderItem renderItem = new RenderItem();

    public GuiXTracerStorage(InventoryPlayer inventory, TileEntityXTracerContainer tile) {
        super(new ContainerXTracerStorage(inventory, tile));
        this.tile = tile;
        xSize = PANEL_WIDTH;
        ySize = PANEL_HEIGHT;
    }

    private static String text(String key, Object... args) {
        return I18n.format("xtracer.gui." + key, args);
    }

    @Override public void initGui() {
        String draftName = name == null ? tile.getCustomName() : name.getText();
        String draftAccepts = accepts == null ? join(tile.getAccepts()) : accepts.getText();
        String draftExcludes = excludes == null ? join(tile.getExcludes()) : excludes.getText();
        String draftSearch = search == null ? "" : search.getText();
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        search = field(16, 35, 130, 14);
        search.setMaxStringLength(128);
        search.setText(draftSearch);
        name = field(16, 67, 188, 14);
        accepts = field(16, 102, 188, 14);
        excludes = field(16, 137, 188, 14);
        name.setMaxStringLength(32);
        accepts.setMaxStringLength(32 * 129);
        excludes.setMaxStringLength(32 * 129);
        name.setText(draftName);
        accepts.setText(draftAccepts);
        excludes.setText(draftExcludes);
        buttonList.clear();
        buttonList.add(new PanelButton(4, guiLeft + 120, guiTop + 10, 38, 16, text("storage")));
        buttonList.add(new PanelButton(5, guiLeft + 162, guiTop + 10, 48, 16, text("settings")));
        buttonList.add(new PanelButton(0, guiLeft + 132, guiTop + 156, 76, 18, text("save")));
        buttonList.add(new PanelButton(1, guiLeft + 152, guiTop + 31, 56, 18, text("refresh")));
        buttonList.add(new PanelButton(2, guiLeft + 112, guiTop + 157, 18, 16, "<"));
        buttonList.add(new PanelButton(3, guiLeft + 190, guiTop + 157, 18, 16, ">"));
        updateButtons();
        requestContents();
        CommonProxy.network.sendToServer(ContainerEditMessage.request(tile.xCoord, tile.yCoord, tile.zCoord));
    }

    private GuiTextField field(int x, int y, int width, int height) {
        GuiTextField field = new GuiTextField(fontRendererObj, x, y, width, height);
        field.setEnableBackgroundDrawing(false);
        field.setTextColor(TEXT);
        return field;
    }

    public boolean isContainer(int x, int y, int z) {
        return tile.xCoord == x && tile.yCoord == y && tile.zCoord == z;
    }

    public void applyState(String value, long stateRevision, List<String> accept, List<String> exclude) {
        if (name == null) return;
        String incomingName = value == null ? "" : value;
        List<String> incomingAccepts = accept == null ? new ArrayList<String>() : new ArrayList<>(accept);
        List<String> incomingExcludes = exclude == null ? new ArrayList<String>() : new ArrayList<>(exclude);
        // A repeated synchronization must not erase an unsaved draft.
        boolean replaceDraft = !rulesReceived || saving || !incomingName.equals(savedName)
            || !incomingAccepts.equals(acceptValues) || !incomingExcludes.equals(excludeValues);
        rulesReceived = true;
        saving = false;
        revision = stateRevision;
        savedName = incomingName;
        acceptValues = incomingAccepts;
        excludeValues = incomingExcludes;
        if (replaceDraft) {
            name.setText(savedName);
            accepts.setText(join(acceptValues));
            excludes.setText(join(excludeValues));
        }
        updateButtons();
    }

    private static String join(List<String> values) {
        return values == null ? "" : String.join(",", values);
    }

    private static List<String> split(String value) {
        List<String> result = new ArrayList<>();
        if (value != null) for (String part : value.split("[,;|，；]+")) {
            String normalized = new RoutingExpression(part).value();
            if (!normalized.isEmpty() && !result.contains(normalized)) result.add(normalized);
        }
        return result;
    }

    private boolean dirty() {
        return !name.getText().trim().equals(savedName) || !split(accepts.getText()).equals(acceptValues)
            || !split(excludes.getText()).equals(excludeValues);
    }

    private boolean validRules() {
        for (List<String> values : Arrays.asList(split(accepts.getText()), split(excludes.getText()))) {
            if (values.size() > 32) return false;
            for (String value : values) if (value.length() > 64) return false;
        }
        return true;
    }

    private void requestContents() {
        loading = true;
        searchTicks = 0;
        loadingTicks = 0;
        updateButtons();
        CommonProxy.network.sendToServer(new ContainerContentsRequestMessage(tile.xCoord, tile.yCoord, tile.zCoord, contentPage, search.getText(), requestId = ++nextRequestId));
    }

    public void applyContents(int page, long revision, int totalEntries, List<ContainerContentsMessage.Entry> values, int responseId) {
        if (responseId != requestId || searchTicks > 0) return;
        contentPage = Math.max(0, page);
        contentRevision = revision;
        this.totalEntries = Math.max(0, totalEntries);
        contents.clear();
        if (values != null) contents.addAll(values);
        loading = false;
        if (contentPage >= totalPages()) {
            contentPage = totalPages() - 1;
            requestContents();
        }
        updateButtons();
    }

    public boolean isContentsRequestCurrent(int id) {
        return id == requestId && searchTicks == 0;
    }

    public void contentsLookupProgress(int id) {
        if (isContentsRequestCurrent(id)) loadingTicks = 0;
    }

    private int totalPages() {
        return totalEntries == 0 ? 1 : (totalEntries - 1) / ContainerContentsMessage.PAGE_SIZE + 1;
    }

    private void updateButtons() {
        for (Object value : buttonList) {
            PanelButton button = (PanelButton) value;
            button.visible = button.id >= 4 || (button.id == 0 ? settings : !settings);
            if (button.id == 0) button.enabled = rulesReceived && !saving && dirty() && validRules();
            if (button.id == 1) button.enabled = !loading;
            if (button.id == 2) button.enabled = !loading && contentPage > 0;
            if (button.id == 3) button.enabled = !loading && contentPage + 1 < totalPages();
            button.selected = button.id == (settings ? 5 : 4);
        }
    }

    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            saving = true;
            saveTicks = 0;
            CommonProxy.network.sendToServer(new ContainerEditMessage(ContainerEditMessage.SAVE,
                tile.xCoord, tile.yCoord, tile.zCoord, revision, name.getText(),
                split(accepts.getText()), split(excludes.getText())));
        } else if (button.id == 1) {
            requestContents();
        } else if (button.id == 2 || button.id == 3) {
            contentPage += button.id == 2 ? -1 : 1;
            requestContents();
        } else if (button.id == 4 || button.id == 5) {
            settings = button.id == 5;
            search.setFocused(false);
            name.setFocused(false);
            accepts.setFocused(false);
            excludes.setFocused(false);
        }
        updateButtons();
    }

    @Override protected void keyTyped(char typedChar, int keyCode) {
        if (!settings && keyCode != Keyboard.KEY_ESCAPE && search.isFocused()) {
            String before = search.getText();
            search.textboxKeyTyped(typedChar, keyCode);
            if (!before.equals(search.getText())) {
                contentPage = 0;
                loading = true;
                searchTicks = 5;
                updateButtons();
            }
            return;
        }
        if (settings && keyCode != Keyboard.KEY_ESCAPE) {
            if (keyCode == Keyboard.KEY_TAB) {
                boolean focusName = name.isFocused();
                boolean focusAccepts = accepts.isFocused();
                name.setFocused(!focusName && !focusAccepts);
                accepts.setFocused(focusName);
                excludes.setFocused(focusAccepts);
                return;
            }
            if (!saving && (name.textboxKeyTyped(typedChar, keyCode)
                || accepts.textboxKeyTyped(typedChar, keyCode)
                || excludes.textboxKeyTyped(typedChar, keyCode))) {
                updateButtons();
                return;
            }
            if (name.isFocused() || accepts.isFocused() || excludes.isFocused()) return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private int hoveredEntry(int x, int y) {
        if (settings || loading || x < GRID_X || y < GRID_Y) return -1;
        int column = (x - GRID_X) / CELL_SIZE;
        int row = (y - GRID_Y) / CELL_SIZE;
        if (column >= 9 || row >= 5 || (x - GRID_X) % CELL_SIZE >= 19
            || (y - GRID_Y) % CELL_SIZE >= 19) return -1;
        int index = row * 9 + column;
        return index < contents.size() ? index : -1;
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int localX = mouseX - guiLeft;
        int localY = mouseY - guiTop;
        int index = hoveredEntry(localX, localY);
        if (mouseButton == 0 && index >= 0) {
            CommonProxy.network.sendToServer(new ContainerExtractMessage(tile.xCoord, tile.yCoord, tile.zCoord,
                contents.get(index).sourceIndex, 64, contentRevision));
            requestContents();
            return;
        }
        if (!settings) search.mouseClicked(localX, localY, mouseButton);
        if (settings && !saving) {
            name.mouseClicked(localX, localY, mouseButton);
            accepts.mouseClicked(localX, localY, mouseButton);
            excludes.mouseClicked(localX, localY, mouseButton);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        int x = Mouse.getEventX() * width / mc.displayWidth - guiLeft;
        int y = height - Mouse.getEventY() * height / mc.displayHeight - 1 - guiTop;
        if (settings || loading || wheel == 0 || x < GRID_X || x >= GRID_X + 180
            || y < GRID_Y || y >= GRID_Y + 100) return;
        int next = Math.max(0, Math.min(totalPages() - 1, contentPage + (wheel < 0 ? 1 : -1)));
        if (next != contentPage) { contentPage = next; requestContents(); }
    }

    private static String formatCount(long count) {
        if (count >= 1000000000000L) return String.format(Locale.ROOT, "%.1fT", count / 1000000000000D);
        if (count >= 1000000000L) return String.format(Locale.ROOT, "%.1fB", count / 1000000000D);
        if (count >= 1000000L) return String.format(Locale.ROOT, "%.1fM", count / 1000000D);
        if (count >= 1000L) return String.format(Locale.ROOT, "%.1fK", count / 1000D);
        return String.valueOf(count);
    }

    @Override public void updateScreen() {
        super.updateScreen();
        if (!rulesReceived && ++ruleRequestTicks % 20 == 0) {
            CommonProxy.network.sendToServer(ContainerEditMessage.request(tile.xCoord, tile.yCoord, tile.zCoord));
        }
        if (saving && ++saveTicks >= 100) saving = false;
        if (searchTicks > 0 && --searchTicks == 0) requestContents();
        if (loading && searchTicks == 0 && ++loadingTicks >= 600) requestContents();
        search.updateCursorCounter();
        name.updateCursorCounter();
        accepts.updateCursorCounter();
        excludes.updateCursorCounter();
        updateButtons();
    }

    @Override public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    private static void bevel(int x, int y, int width, int height, int fill, int light, int dark) {
        drawRect(x, y, x + width, y + height, fill);
        drawRect(x, y, x + width, y + 1, light);
        drawRect(x, y, x + 1, y + height, light);
        drawRect(x, y + height - 1, x + width, y + height, dark);
        drawRect(x + width - 1, y, x + width, y + height, dark);
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        int x = guiLeft;
        int y = guiTop;
        drawRect(x + 3, y + 3, x + xSize + 3, y + ySize + 3, 0x70000000);
        bevel(x, y, xSize, ySize, 0xff303e47, 0xff71838c, 0xff121b22);
        bevel(x + 3, y + 3, xSize - 6, ySize - 6, 0xff3d4c55, 0xff4d5f69, 0xff23313a);
        bevel(x + 7, y + 7, 206, 21, 0xff202c34, 0xff142028, 0xff576974);
        drawRect(x + 9, y + 8, x + 11, y + 26, 0xff63d2df);
        bevel(x + 8, y + 29, 204, 145, 0xff26333c, 0xff17232b, 0xff596c76);
        bevel(x + 8, y + 176, 204, 88, 0xff34434c, 0xff1e2b33, 0xff61747d);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            bevel(x + ContainerXTracerStorage.INVENTORY_X + col * 18 - 1,
                y + ContainerXTracerStorage.INVENTORY_Y + row * 18 - 1, 18, 18,
                0xff879197, 0xff1a252c, 0xffc0c9cc);
        }
        for (int col = 0; col < 9; col++) bevel(x + ContainerXTracerStorage.INVENTORY_X + col * 18 - 1,
            y + ContainerXTracerStorage.HOTBAR_Y - 1, 18, 18, 0xff879197, 0xff1a252c, 0xffc0c9cc);
        if (settings) {
            drawField(name, 62, ACCENT);
            drawField(accepts, 97, 0x86cba6);
            drawField(excludes, 132, 0xdfa17c);
        } else {
            bevel(x + 12, y + 31, 136, 18, 0xff17232b, 0xff0e171e, 0xff536874);
            for (int i = 0; i < ContainerContentsMessage.PAGE_SIZE; i++) {
                int cellX = x + GRID_X + i % 9 * CELL_SIZE;
                int cellY = y + GRID_Y + i / 9 * CELL_SIZE;
                bevel(cellX, cellY, 19, 19, 0xff66747d, 0xff142129, 0xffa2b0b7);
                if (hoveredEntry(mouseX - x, mouseY - y) == i) {
                    drawRect(cellX + 1, cellY + 1, cellX + 19, cellY + 19, 0x4063d2df);
                    drawRect(cellX, cellY, cellX + 20, cellY + 1, 0xff63d2df);
                }
            }
        }
        for (int screwX : new int[] {4, 212}) for (int screwY : new int[] {4, 258}) {
            drawRect(x + screwX, y + screwY, x + screwX + 3, y + screwY + 3, 0xff152028);
            drawRect(x + screwX, y + screwY + 1, x + screwX + 2, y + screwY + 2, 0xff81919a);
        }
    }

    private void drawField(GuiTextField field, int y, int color) {
        bevel(guiLeft + 12, guiTop + y, 196, 23, 0xff17232b, 0xff0e171e, 0xff536874);
        drawRect(guiLeft + 13, guiTop + y + 1, guiLeft + 15, guiTop + y + 22,
            0xff000000 | (field.isFocused() ? color : 0x435762));
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = savedName.isEmpty() ? I18n.format("container.x_tracer") : savedName;
        float scale = Math.min(1F, 97F / Math.max(1, fontRendererObj.getStringWidth(title)));
        GL11.glPushMatrix();
        GL11.glTranslatef(17F, 14F, 0F);
        GL11.glScalef(scale, scale, 1F);
        fontRendererObj.drawString(title, 0, 0, TEXT);
        GL11.glPopMatrix();
        fontRendererObj.drawString(I18n.format("container.inventory"), 14, 179, MUTED);
        if (settings) {
            fontRendererObj.drawString(text("name"), 14, 56, MUTED);
            fontRendererObj.drawString(text("accept"), 14, 91, 0x86cba6);
            fontRendererObj.drawString(text("exclude"), 14, 126, 0xdfa17c);
            fontRendererObj.drawString("?", 199, 91, MUTED);
            fontRendererObj.drawString("?", 199, 126, MUTED);
            String status = !rulesReceived ? "syncing" : saving ? "saving" : !validRules() ? "invalid" : dirty() ? "unsaved" : "saved";
            int color = !validRules() ? 0xdfa17c : dirty() ? 0xdfb879 : 0x86cba6;
            drawRect(14, 162, 17, 165, 0xff000000 | color);
            fontRendererObj.drawString(text(status), 21, 160, color);
            name.drawTextBox();
            accepts.drawTextBox();
            excludes.drawTextBox();
        } else {
            search.drawTextBox();
            if (search.getText().isEmpty() && !search.isFocused())
                fontRendererObj.drawString(text("search"), 16, 35, MUTED);
            String types = text("types", totalEntries);
            GL11.glPushMatrix();
            GL11.glTranslatef(14, 161, 0);
            float typeScale = Math.min(1F, 92F / Math.max(1, fontRendererObj.getStringWidth(types)));
            GL11.glScalef(typeScale, typeScale, 1);
            fontRendererObj.drawString(types, 0, 0, TEXT);
            GL11.glPopMatrix();
            for (int i = 0; !loading && i < contents.size() && i < ContainerContentsMessage.PAGE_SIZE; i++) {
                ContainerContentsMessage.Entry entry = contents.get(i);
                ItemStack stack = stack(entry);
                if (stack == null) continue;
                int x = GRID_X + i % 9 * CELL_SIZE;
                int y = GRID_Y + i / 9 * CELL_SIZE;
                RenderHelper.enableGUIStandardItemLighting();
                renderItem.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x + 2, y + 1);
                RenderHelper.disableStandardItemLighting();
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glPushMatrix();
                GL11.glTranslatef(x + 19, y + 14, 0);
                GL11.glScalef(0.65F, 0.65F, 1F);
                String count = formatCount(entry.count);
                fontRendererObj.drawStringWithShadow(count, -fontRendererObj.getStringWidth(count), 0, 0xffffff);
                GL11.glPopMatrix();
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            }
            if (loading || contents.isEmpty()) {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                String message = text(loading ? "loading" : search.getText().trim().isEmpty() ? "empty" : "no_results");
                drawRect(12, 96, 207, 112, 0xe026333c);
                fontRendererObj.drawString(message, (xSize - fontRendererObj.getStringWidth(message)) / 2, 100, TEXT);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            }
            String page = (contentPage + 1) + " / " + totalPages();
            float pageScale = Math.min(1F, 54F / Math.max(1, fontRendererObj.getStringWidth(page)));
            GL11.glPushMatrix();
            GL11.glTranslatef(160, 161, 0);
            GL11.glScalef(pageScale, pageScale, 1);
            fontRendererObj.drawString(page, -fontRendererObj.getStringWidth(page) / 2, 0, TEXT);
            GL11.glPopMatrix();
        }
        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private ItemStack stack(ContainerContentsMessage.Entry entry) {
        return entry.key.toStack(1);
    }

    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        int x = mouseX - guiLeft;
        int y = mouseY - guiTop;
        int index = hoveredEntry(x, y);
        if (index >= 0) {
            ContainerContentsMessage.Entry entry = contents.get(index);
            ItemStack stack = stack(entry);
            List<String> tooltip = stack == null ? new ArrayList<String>() : new ArrayList<>(stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips));
            if (tooltip.isEmpty()) tooltip.add(entry.key.itemId());
            tooltip.add("§7" + text("count", String.format(Locale.ROOT, "%,d", entry.count)));
            tooltip.add("§b" + text("extract_hint"));
            drawHoveringText(tooltip, mouseX, mouseY, fontRendererObj);
        } else if (settings && !validRules() && x >= 12 && x < 130 && y >= 156 && y < 175) {
            drawHoveringText(Arrays.asList(text("limits")), mouseX, mouseY, fontRendererObj);
        } else if (settings && x >= 190 && x < 209) {
            if (y >= 88 && y < 101) drawHoveringText(Arrays.asList(text("accept_help"), text("separators"), text("limits")), mouseX, mouseY, fontRendererObj);
            if (y >= 123 && y < 136) drawHoveringText(Arrays.asList(text("exclude_help"), text("separators"), text("limits")), mouseX, mouseY, fontRendererObj);
        }
    }

    private static class PanelButton extends GuiButton {
        private boolean selected;

        PanelButton(int id, int x, int y, int width, int height, String label) {
            super(id, x, y, width, height, label);
        }

        @Override public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) return;
            boolean hovered = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
            int fill = !enabled ? 0xff2b363e : selected ? 0xff315a65 : hovered ? 0xff4b6470 : 0xff3a4e59;
            bevel(xPosition, yPosition, width, height, fill, hovered && enabled ? 0xff88c4ce : 0xff657e89, 0xff14232c);
            if (selected) drawRect(xPosition + 1, yPosition + height - 2, xPosition + width - 1, yPosition + height - 1, 0xff63d2df);
            float scale = Math.min(1F, (width - 6F) / Math.max(1, mc.fontRenderer.getStringWidth(displayString)));
            GL11.glPushMatrix();
            GL11.glTranslatef(xPosition + width / 2F, yPosition + (height - 8 * scale) / 2F, 0);
            GL11.glScalef(scale, scale, 1);
            mc.fontRenderer.drawStringWithShadow(displayString, -mc.fontRenderer.getStringWidth(displayString) / 2, 0,
                !enabled ? 0x71818a : selected ? ACCENT : TEXT);
            GL11.glPopMatrix();
            GL11.glColor4f(1F, 1F, 1F, 1F);
        }
    }
}
