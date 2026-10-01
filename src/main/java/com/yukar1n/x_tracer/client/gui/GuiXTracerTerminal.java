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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
import com.yukar1n.x_tracer.SelectorLogic;

public class GuiXTracerTerminal extends GuiScreen {
    private static final int COLUMNS = 9;
    private static final int ROWS = 5;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int CELL_SIZE = 24;
    private static final int BLOCK_PAGE = 0;
    private static final int ORE_PAGE = 1;

    private GuiTextField search;
    private GuiTextField radius;
    private int pageType;
    private int page;
    private boolean drop;
    private String selectedBlock;
    private final Set<String> selectedOres = new HashSet<>();
    private final Set<String> expandedBlocks = new HashSet<>();
    private List<BlockCatalogEntry> cachedFilteredBlocks;
    private List<BlockCatalogEntry> cachedGroupedBlocks;
    private List<OreCatalogEntry> cachedFilteredOres;
    private String cachedQuery = "";
    private int cachedPageType = -1;
    private String cachedExpansionKey = "";
    private final RenderItem renderItem = new RenderItem();

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        XTracerCatalog.ensureBuilt();
        ClientConfig config = Config.getPlayerConfig(Minecraft.getMinecraft().thePlayer.getUniqueID());
        pageType = "ore".equals(config.curMatchMode) ? ORE_PAGE : BLOCK_PAGE;
        drop = "True".equals(config.getDrop);
        selectedBlock = config.targetList.isEmpty() ? "" : config.targetList.get(0);
        selectedOres.addAll(config.targetOreNameList);
        int left = width / 2 - (COLUMNS * CELL_SIZE) / 2;
        search = new GuiTextField(fontRendererObj, left, 34, COLUMNS * CELL_SIZE, 20);
        radius = new GuiTextField(fontRendererObj, left, 205, 55, 20);
        radius.setText(String.valueOf(config.radius));
        rebuildButtons();
    }

    private int gridLeft() {
        return width / 2 - (COLUMNS * CELL_SIZE) / 2;
    }

    private void rebuildButtons() {
        buttonList.clear();
        int left = gridLeft();
        buttonList.add(new GuiButton(0, left, 10, 105, 20, "Block"));
        buttonList.add(new GuiButton(1, left + 110, 10, 110, 20, "Ore"));
        buttonList.add(new GuiButton(2, left, 230, 55, 20, "Prev"));
        buttonList.add(new GuiButton(3, left + 60, 230, 55, 20, "Next"));
        buttonList.add(new GuiButton(4, left + 120, 230, 100, 20, "Save target"));
        buttonList.add(new GuiButton(5, left, 255, 105, 20, "Drops: " + drop));
        buttonList.add(new GuiButton(6, left + 110, 255, 110, 20, "Scan"));
        buttonList.add(new GuiButton(7, left, 280, 105, 20, "Ray select"));
        buttonList.add(new GuiButton(8, left + 110, 280, 110, 20, "Save radius"));
    }

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
        int left = gridLeft();
        if (button.id == 0 || button.id == 1) {
            pageType = button.id;
            page = 0;
            rebuildButtons();
        } else if (button.id == 2) {
            if (page > 0) page--;
        } else if (button.id == 3) {
            int size = pageType == BLOCK_PAGE ? groupedBlocks().size() : filteredOres().size();
            if ((page + 1) * PAGE_SIZE < size) page++;
        } else if (button.id == 4) {
            if (pageType == BLOCK_PAGE && selectedBlock != null && !selectedBlock.isEmpty()) {
                CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_BLOCK, selectedBlock, 0, drop));
            } else if (pageType == ORE_PAGE && !selectedOres.isEmpty()) {
                CommonProxy.network.sendToServer(new TerminalRequestMessage(new ArrayList<>(selectedOres), drop));
            }
        } else if (button.id == 5) {
            drop = !drop;
            button.displayString = "Drops: " + drop;
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_DROP, "", 0, drop));
        } else if (button.id == 6) {
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SCAN, "", 0, drop));
        } else if (button.id == 7) {
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.RAY, "", 0, drop));
        } else if (button.id == 8) {
            try {
                CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_RADIUS, "", Integer.parseInt(radius.getText()), drop));
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        String before = search.getText();
        search.textboxKeyTyped(typedChar, keyCode);
        radius.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_ESCAPE) super.keyTyped(typedChar, keyCode);
        page = SelectorLogic.pageAfterSearch(page, before, search.getText());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        search.mouseClicked(mouseX, mouseY, mouseButton);
        radius.mouseClicked(mouseX, mouseY, mouseButton);
        int left = gridLeft();
        int top = 60;
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
                } else selectedBlock = entry.target();
            }
        } else {
            List<OreCatalogEntry> entries = filteredOres();
            if (index < entries.size()) {
                String name = entries.get(index).name;
                if (!selectedOres.add(name)) selectedOres.remove(name);
            }
        }
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int size = pageType == BLOCK_PAGE ? groupedBlocks().size() : filteredOres().size();
            if (wheel < 0 && (page + 1) * PAGE_SIZE < size) page++;
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
        int left = gridLeft();
        drawCenteredString(fontRendererObj, "XTracer Terminal", width / 2, 0, 0xFFFFFF);
        drawString(fontRendererObj, pageType == BLOCK_PAGE ? "Placeable blocks" : "Ore Dictionary (ore* only, multi-select)", left, 58, 0xFFFFFF);
        search.drawTextBox();
        drawCatalog(left, mouseX, mouseY);
        drawString(fontRendererObj, "Radius", left + 65, 210, 0xFFFFFF);
        radius.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
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
                int y = 60 + (i / COLUMNS) * CELL_SIZE;
                drawBlockEntry(entry, x, y);
                if (hasVariants(entry, filteredBlocks())) {
                    drawString(fontRendererObj, expandedBlocks.contains(entry.registryName) ? "-" : "+", x + 1, y + 1, 0xFFFFFF);
                }
                if (isHovered(x, y, mouseX, mouseY)) {
                    hover = java.util.Arrays.asList(entry.displayName, entry.target());
                }
            }
        } else {
            List<OreCatalogEntry> entries = filteredOres();
            for (int i = 0; i < PAGE_SIZE && page * PAGE_SIZE + i < entries.size(); i++) {
                OreCatalogEntry entry = entries.get(page * PAGE_SIZE + i);
                int x = left + (i % COLUMNS) * CELL_SIZE;
                int y = 60 + (i / COLUMNS) * CELL_SIZE;
                drawString(fontRendererObj, selectedOres.contains(entry.name) ? "X" : "·", x + 8, y + 7, selectedOres.contains(entry.name) ? 0x55FF55 : 0xAAAAAA);
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
            renderItem.renderItemAndEffectIntoGUI(fontRendererObj, Minecraft.getMinecraft().getTextureManager(), entry.stack, x + 4, y + 4);
        } catch (Throwable ignored) {
            drawRect(x + 5, y + 5, x + 19, y + 19, 0xff555555);
        }
    }

    private boolean isHovered(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + CELL_SIZE && mouseY >= y && mouseY < y + CELL_SIZE;
    }

}
