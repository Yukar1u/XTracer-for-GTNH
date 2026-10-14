package com.yukar1n.x_tracer.client.gui;

import com.yukar1n.x_tracer.ClientConfig;
import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.Config;
import com.yukar1n.x_tracer.client.catalog.BlockCatalogEntry;
import com.yukar1n.x_tracer.client.catalog.OreCatalogEntry;
import com.yukar1n.x_tracer.client.catalog.XTracerCatalog;
import com.yukar1n.x_tracer.network.TerminalRequestMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import net.minecraft.client.resources.I18n;
import com.yukar1n.x_tracer.client.gui.IndustrialPanel.PanelButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import com.yukar1n.x_tracer.SelectorLogic;
import com.yukar1n.x_tracer.storage.ContainerReference;
import com.yukar1n.x_tracer.storage.TerminalProfile;

public class GuiXTracerTerminal extends GuiScreen {
    private static final int COLUMNS = 9;
    private static final int ROWS = 5;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int CELL_SIZE = 20;
    private static final int PANEL_WIDTH = 250;
    private static final int PANEL_HEIGHT = 234;
    private static final int CONTAINER_ROWS = 4;
    private static final int CONTAINER_ROW_HEIGHT = 25;
    private boolean initialized;
    private boolean radiusDirty;
    private boolean targetDirty;
    private static final int BLOCK_PAGE = 0;
    private static final int ORE_PAGE = 1;
    private static final int CONTAINER_PAGE = 2;

    private GuiTextField search;
    private GuiTextField radius;
    private int pageType;
    private int page;
    private boolean drop;
    private boolean remoteInput;
    private String selectedBlock;
    private String pendingBlock;
    private final Set<String> selectedOres = new HashSet<>();
    private final Set<String> expandedBlocks = new HashSet<>();
    private List<BlockCatalogEntry> cachedFilteredBlocks;
    private List<BlockCatalogEntry> cachedGroupedBlocks;
    private List<OreCatalogEntry> cachedFilteredOres;
    private String cachedQuery = "";
    private int cachedPageType = -1;
    private String cachedExpansionKey = "";
    private final RenderItem renderItem = new RenderItem();
    private List<String> containerSummaries = new ArrayList<>();
    private int selectedContainer = 0;

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        XTracerCatalog.ensureBuilt();
        ClientConfig config = Config.getPlayerConfig(Minecraft.getMinecraft().thePlayer.getUniqueID());
        String query = search == null ? "" : search.getText();
        String radiusDraft = radius == null ? String.valueOf(config.radius) : radius.getText();
        boolean searchFocused = search != null && search.isFocused();
        boolean radiusFocused = radius != null && radius.isFocused();
        if (!initialized) {
            pageType = "ore".equals(config.curMatchMode) ? ORE_PAGE : BLOCK_PAGE;
            drop = "True".equals(config.getDrop);
            selectedBlock = config.targetList.isEmpty() ? "" : config.targetList.get(0);
            selectedOres.addAll(config.targetOreNameList);
        }
        search = new GuiTextField(fontRendererObj, panelLeft() + 9, panelTop() + 39, 232, 14);
        search.setMaxStringLength(256);
        search.setText(query);
        search.setFocused(searchFocused);
        radius = new GuiTextField(fontRendererObj, panelLeft() + 42, panelTop() + 179, 42, 14);
        radius.setMaxStringLength(10);
        radius.setText(radiusDraft);
        radius.setFocused(radiusFocused);
        rebuildButtons();
        if (!initialized) requestStateRefresh();
        initialized = true;
    }

    public void applyServerState(int serverRadius, boolean serverDrop, String mode, String blockTarget, String oreTargets) {
        applyServerState(serverRadius, serverDrop, mode, blockTarget, oreTargets, false);
    }

    public void applyServerState(int serverRadius, boolean serverDrop, String mode, String blockTarget, String oreTargets, boolean serverRemoteInput) {
        if (!radiusDirty && radius != null) radius.setText(String.valueOf(serverRadius));
        drop = serverDrop;
        remoteInput = serverRemoteInput;
        if (!targetDirty) {
            selectedBlock = blockTarget == null ? "" : blockTarget;
            selectedOres.clear();
            if (oreTargets != null && !oreTargets.isEmpty()) {
                for (String value : oreTargets.split("\\n")) if (!value.isEmpty()) selectedOres.add(value);
            }
        }
        // A state acknowledgement must not change the tab or discard a local draft.
        rebuildButtons();
    }

    private int panelLeft() { return (width - PANEL_WIDTH) / 2; }
    private int panelTop() { return (height - PANEL_HEIGHT) / 2; }
    private int gridLeft() { return panelLeft() + 35; }
    private int gridTop() { return panelTop() + 57; }
    private String text(String key) { return I18n.format("xtracer.gui." + key); }
    private String toggleLabel(String key, boolean enabled) {
        return text(key) + ": " + text(enabled ? "on" : "off");
    }

    private void addButton(int id, int x, int y, int width, String label) {
        PanelButton button = new PanelButton(id, panelLeft() + x, panelTop() + y, width, 16, label);
        button.selected = (id == 0 && pageType == BLOCK_PAGE) || (id == 1 && pageType == ORE_PAGE)
            || (id == 9 && pageType == CONTAINER_PAGE);
        button.enabled = id != 4 || pageType != CONTAINER_PAGE;
        buttonList.add(button);
    }

    private void rebuildButtons() {
        buttonList.clear();
        addButton(0, 8, 19, 76, text("blocks"));
        addButton(1, 87, 19, 76, text("ores"));
        addButton(9, 166, 19, 76, text("containers"));
        addButton(2, 8, 159, 38, text("previous"));
        addButton(3, 49, 159, 38, text("next"));
        addButton(4, 159, 159, 83, text("save_target"));
        addButton(7, 89, 178, 45, text("apply_radius"));
        addButton(5, 137, 178, 105, toggleLabel("drops", drop));
        addButton(10, 8, 197, 112, toggleLabel("remote_input", remoteInput));
        addButton(8, 123, 197, 57, text("ray_select"));
        addButton(6, 183, 197, 59, text("trace"));
        addButton(11, 8, 216, 115, text("bind_looked_at"));
        addButton(12, 126, 216, 116, text("unbind_looked_at"));
    }

    private List<ContainerReference> bindings() {
        return Minecraft.getMinecraft().thePlayer == null ? new ArrayList<ContainerReference>()
            : TerminalProfile.bindings(Minecraft.getMinecraft().thePlayer.getHeldItem());
    }

    private int entryCount() {
        return pageType == CONTAINER_PAGE ? bindings().size()
            : pageType == BLOCK_PAGE ? groupedBlocks().size() : filteredOres().size();
    }

    private int pageSize() { return pageType == CONTAINER_PAGE ? CONTAINER_ROWS : PAGE_SIZE; }
    private int lastPage() { return Math.max(0, (entryCount() - 1) / pageSize()); }
    private void clampPage() { page = Math.max(0, Math.min(page, lastPage())); }

    private void refreshSearchCache() {
        String query = SelectorLogic.matchKey(search.getText());
        String expansionKey = expandedBlocks.toString();
        if (query.equals(cachedQuery) && pageType == cachedPageType && expansionKey.equals(cachedExpansionKey)
            && cachedFilteredBlocks != null && cachedFilteredOres != null) return;
        cachedQuery = query;
        cachedPageType = pageType;
        cachedExpansionKey = expansionKey;
        cachedFilteredBlocks = new ArrayList<>();
        for (BlockCatalogEntry entry : XTracerCatalog.blocks()) {
            if (query.isEmpty() || entry.searchText().contains(query)) cachedFilteredBlocks.add(entry);
        }
        Map<String, List<BlockCatalogEntry>> groups = new LinkedHashMap<>();
        for (BlockCatalogEntry entry : cachedFilteredBlocks) {
            List<BlockCatalogEntry> members = groups.get(entry.registryName);
            if (members == null) { members = new ArrayList<>(); groups.put(entry.registryName, members); }
            members.add(entry);
        }
        cachedGroupedBlocks = new ArrayList<>();
        for (Map.Entry<String, List<BlockCatalogEntry>> group : groups.entrySet()) {
            if (expandedBlocks.contains(group.getKey())) cachedGroupedBlocks.addAll(group.getValue());
            else cachedGroupedBlocks.add(group.getValue().get(0));
        }
        cachedFilteredOres = new ArrayList<>();
        for (OreCatalogEntry entry : XTracerCatalog.ores()) {
            if (query.isEmpty() || entry.searchText().contains(query)) cachedFilteredOres.add(entry);
        }
    }

    private List<BlockCatalogEntry> filteredBlocks() {
        refreshSearchCache();
        return cachedFilteredBlocks;
    }

    private List<BlockCatalogEntry> groupedBlocks() {
        refreshSearchCache();
        return cachedGroupedBlocks;
    }

    private List<OreCatalogEntry> filteredOres() {
        refreshSearchCache();
        return cachedFilteredOres;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0 || button.id == 1 || button.id == 9) {
            pageType = button.id == 9 ? CONTAINER_PAGE : button.id;
            page = 0;
            rebuildButtons();
        } else if (button.id == 2) {
            if (page > 0) page--;
        } else if (button.id == 3) {
            if (page < lastPage()) page++;
        } else if (button.id == 4) {
            if (pageType == BLOCK_PAGE && selectedBlock != null && !selectedBlock.isEmpty()) {
                CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_BLOCK, selectedBlock, 0, drop));
                targetDirty = false;
            } else if (pageType == ORE_PAGE && !selectedOres.isEmpty()) {
                CommonProxy.network.sendToServer(new TerminalRequestMessage(new ArrayList<>(selectedOres), drop));
                targetDirty = false;
            }
        } else if (button.id == 5) {
            drop = !drop;
            button.displayString = toggleLabel("drops", drop);
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_DROP, "", 0, drop));
        } else if (button.id == 10) {
            remoteInput = !remoteInput;
            button.displayString = toggleLabel("remote_input", remoteInput);
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_REMOTE_INPUT, "", 0, remoteInput));
        } else if (button.id == 6) {
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SCAN, "", 0, drop));
        } else if (button.id == 8) {
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.RAY, "", 0, drop));
        } else if (button.id == 11) {
            net.minecraft.util.MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;
            if (hit != null && hit.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
                CommonProxy.network.sendToServer(new com.yukar1n.x_tracer.network.TerminalBindingMessage(true, hit.blockX, hit.blockY, hit.blockZ));
                requestStateRefresh();
            }
        } else if (button.id == 12) {
            net.minecraft.util.MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;
            if (hit != null && hit.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
                CommonProxy.network.sendToServer(new com.yukar1n.x_tracer.network.TerminalBindingMessage(false, hit.blockX, hit.blockY, hit.blockZ));
                requestStateRefresh();
            }
        } else if (button.id == 7) {
            try {
                CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_RADIUS, "", Integer.parseInt(radius.getText()), drop));
                radiusDirty = false;
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        String before = search.getText();
        String radiusBefore = radius.getText();
        if (pageType != CONTAINER_PAGE) search.textboxKeyTyped(typedChar, keyCode);
        radius.textboxKeyTyped(typedChar, keyCode);
        if (!radiusBefore.equals(radius.getText())) radiusDirty = true;
        if (keyCode == Keyboard.KEY_ESCAPE) super.keyTyped(typedChar, keyCode);
        page = SelectorLogic.pageAfterSearch(page, before, search.getText());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (pageType != CONTAINER_PAGE) search.mouseClicked(mouseX, mouseY, mouseButton);
        radius.mouseClicked(mouseX, mouseY, mouseButton);
        int left = gridLeft();
        int top = gridTop();
        if (pageType == CONTAINER_PAGE) {
            int row = (mouseY - top) / CONTAINER_ROW_HEIGHT;
            if (mouseX >= panelLeft() + 8 && mouseX < panelLeft() + 242
                && mouseY >= top && mouseY < top + CONTAINER_ROWS * CONTAINER_ROW_HEIGHT
                && page * CONTAINER_ROWS + row < bindings().size()) {
                selectedContainer = page * CONTAINER_ROWS + row;
            }
            return;
        }
        if (mouseX < left || mouseX >= left + COLUMNS * CELL_SIZE || mouseY < top
            || mouseY >= top + ROWS * CELL_SIZE) return;
        int column = (mouseX - left) / CELL_SIZE;
        int row = (mouseY - top) / CELL_SIZE;
        int index = page * PAGE_SIZE + row * COLUMNS + column;
        if (pageType == BLOCK_PAGE) {
            List<BlockCatalogEntry> entries = groupedBlocks();
            if (index < entries.size()) {
                BlockCatalogEntry entry = entries.get(index);
                if ((Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU))
                    && hasVariants(entry, filteredBlocks())) {
                    String key = entry.registryName;
                    if (!expandedBlocks.add(key)) expandedBlocks.remove(key);
                    page = 0;
                } else {
                    selectedBlock = entry.target();
                    pendingBlock = selectedBlock;
                    targetDirty = true;
                }
            }
        } else {
            List<OreCatalogEntry> entries = filteredOres();
            if (index < entries.size()) {
                String name = entries.get(index).name;
                if (!selectedOres.add(name)) selectedOres.remove(name);
                targetDirty = true;
            }
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            if (wheel < 0 && page < lastPage()) page++;
            if (wheel > 0 && page > 0) page--;
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        clampPage();
        int left = panelLeft();
        int top = panelTop();
        IndustrialPanel.bevel(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF252E34, false);
        IndustrialPanel.bevel(left + 5, top + 5, 240, 11, 0xFF18242B, true);
        IndustrialPanel.fitText(fontRendererObj, text("terminal"), left + 10, top + 7, 220, IndustrialPanel.ACCENT, 0.8F);
        if (pageType != CONTAINER_PAGE) search.drawTextBox();
        else IndustrialPanel.fitText(fontRendererObj, text("containers_read_only"), left + 9, top + 42, 232, IndustrialPanel.MUTED, 0.8F);
        IndustrialPanel.bevel(left + 8, gridTop() - 1, 234, 102, 0xFF172127, true);
        IndustrialPanel.fitText(fontRendererObj, (page + 1) + " / " + (lastPage() + 1), left + 92, top + 163, 62, IndustrialPanel.MUTED, 0.8F);
        IndustrialPanel.fitText(fontRendererObj, text("radius"), left + 8, top + 182, 31, IndustrialPanel.MUTED, 0.75F);
        radius.drawTextBox();
        for (Object value : buttonList) {
            GuiButton button = (GuiButton) value;
            if (button.id == 2) button.enabled = page > 0;
            if (button.id == 3) button.enabled = page < lastPage();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (pageType == CONTAINER_PAGE) drawContainers(left + 8, mouseX, mouseY);
        else drawCatalog(gridLeft(), mouseX, mouseY);
    }

    private void drawCatalog(int left, int mouseX, int mouseY) {
        List<String> hover = null;
        RenderHelper.enableGUIStandardItemLighting();
        try {
        if (pageType == BLOCK_PAGE) {
            List<BlockCatalogEntry> entries = groupedBlocks();
            for (int i = 0; i < PAGE_SIZE && page * PAGE_SIZE + i < entries.size(); i++) {
                BlockCatalogEntry entry = entries.get(page * PAGE_SIZE + i);
                int x = left + (i % COLUMNS) * CELL_SIZE;
                int y = gridTop() + (i / COLUMNS) * CELL_SIZE;
                IndustrialPanel.bevel(x, y, CELL_SIZE, CELL_SIZE, 0xFF27343B, true);
                drawBlockEntry(entry, x, y);
                org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
                if (entry.target().equals(selectedBlock)) {
                    drawRect(x, y, x + CELL_SIZE, y + CELL_SIZE, 0x4055c5d1);
                    drawRect(x, y, x + CELL_SIZE, y + 1, 0xff55c5d1);
                    drawRect(x, y + CELL_SIZE - 1, x + CELL_SIZE, y + CELL_SIZE, 0xff55c5d1);
                    drawRect(x, y, x + 1, y + CELL_SIZE, 0xff55c5d1);
                    drawRect(x + CELL_SIZE - 1, y, x + CELL_SIZE, y + CELL_SIZE, 0xff55c5d1);
                }
                if (hasVariants(entry, filteredBlocks())) {
                    drawString(fontRendererObj, expandedBlocks.contains(entry.registryName) ? "-" : "+", x + 1, y + 1, 0xFFFFFF);
                }
                org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
                if (isHovered(x, y, mouseX, mouseY)) {
                    hover = java.util.Arrays.asList(entry.displayName, entry.target());
                }
            }
        } else {
            List<OreCatalogEntry> entries = filteredOres();
            for (int i = 0; i < PAGE_SIZE && page * PAGE_SIZE + i < entries.size(); i++) {
                OreCatalogEntry entry = entries.get(page * PAGE_SIZE + i);
                int x = left + (i % COLUMNS) * CELL_SIZE;
                int y = gridTop() + (i / COLUMNS) * CELL_SIZE;
                IndustrialPanel.bevel(x, y, CELL_SIZE, CELL_SIZE, 0xFF27343B, true);
                drawString(fontRendererObj, selectedOres.contains(entry.name) ? "X" : "·", x + 8, y + 7, selectedOres.contains(entry.name) ? IndustrialPanel.ACCENT : IndustrialPanel.MUTED);
                if (isHovered(x, y, mouseX, mouseY)) hover = java.util.Collections.singletonList(entry.name);
            }
        }
        } finally {
            RenderHelper.disableStandardItemLighting();
        }
        if (hover != null) drawHoveringText(hover, mouseX, mouseY, fontRendererObj);
    }

    private boolean hasVariants(BlockCatalogEntry entry, List<BlockCatalogEntry> entries) {
        int count = 0;
        for (BlockCatalogEntry candidate : entries) {
            if (entry.registryName.equals(candidate.registryName) && ++count > 1) return true;
        }
        return false;
    }

    private void drawBlockEntry(BlockCatalogEntry entry, int x, int y) {
        try {
            renderItem.renderItemAndEffectIntoGUI(fontRendererObj, Minecraft.getMinecraft().getTextureManager(), entry.stack, x + 2, y + 2);
        } catch (Throwable ignored) {
            drawRect(x + 5, y + 5, x + 19, y + 19, 0xff555555);
        }
    }

    private void requestStateRefresh() {
        CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.REQUEST_STATE, "", 0, drop));
    }

    public void applyContainerSummaries(List<String> summaries) {
        containerSummaries = summaries == null ? new ArrayList<String>() : new ArrayList<>(summaries);
    }

    private void drawContainers(int left, int mouseX, int mouseY) {
        List<ContainerReference> bindings = bindings();
        if (bindings.isEmpty()) {
            IndustrialPanel.fitText(fontRendererObj, text("no_containers"), left + 6, gridTop() + 10, 222, IndustrialPanel.MUTED, 0.8F);
            return;
        }
        selectedContainer = Math.max(0, Math.min(selectedContainer, bindings.size() - 1));
        List<String> hover = null;
        for (int row = 0; row < CONTAINER_ROWS && page * CONTAINER_ROWS + row < bindings.size(); row++) {
            int index = page * CONTAINER_ROWS + row;
            int y = gridTop() + row * CONTAINER_ROW_HEIGHT;
            ContainerReference ref = bindings.get(index);
            IndustrialPanel.bevel(left, y, 234, CONTAINER_ROW_HEIGHT, index == selectedContainer ? 0xFF29434D : 0xFF202D34, true);
            String location = (index + 1) + ". " + ref.dimension + "  " + ref.x + "," + ref.y + "," + ref.z;
            String summary = index < containerSummaries.size() ? containerSummaries.get(index) : text("container_rules_hint");
            IndustrialPanel.fitText(fontRendererObj, location, left + 5, y + 4, 224, IndustrialPanel.TEXT, 0.8F);
            IndustrialPanel.fitText(fontRendererObj, summary, left + 5, y + 14, 224, IndustrialPanel.MUTED, 0.75F);
            if (mouseX >= left && mouseX < left + 234 && mouseY >= y && mouseY < y + CONTAINER_ROW_HEIGHT) {
                hover = new ArrayList<>();
                hover.add(location);
                hover.addAll(fontRendererObj.listFormattedStringToWidth(summary, 230));
            }
        }
        if (hover != null) drawHoveringText(hover, mouseX, mouseY, fontRendererObj);
    }

    private boolean isHovered(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + CELL_SIZE && mouseY >= y && mouseY < y + CELL_SIZE;
    }

}
