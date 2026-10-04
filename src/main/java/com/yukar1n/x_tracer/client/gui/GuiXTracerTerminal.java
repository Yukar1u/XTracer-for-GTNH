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
import com.yukar1n.x_tracer.storage.ContainerReference;
import com.yukar1n.x_tracer.storage.TerminalProfile;

public class GuiXTracerTerminal extends GuiScreen {
    private static final int COLUMNS = 9;
    private static final int ROWS = 5;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int CELL_SIZE = 24;
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
        remoteInput = false;
        selectedBlock = config.targetList.isEmpty() ? "" : config.targetList.get(0);
        selectedOres.addAll(config.targetOreNameList);
        int left = width / 2 - (COLUMNS * CELL_SIZE) / 2;
        search = new GuiTextField(fontRendererObj, left, 34, COLUMNS * CELL_SIZE, 20);
        radius = new GuiTextField(fontRendererObj, left, 230, 105, 20);
        radius.setText(String.valueOf(config.radius));
        rebuildButtons();
        CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.REQUEST_STATE, "", 0, drop));
    }

    public void applyServerState(int serverRadius, boolean serverDrop, String mode, String blockTarget, String oreTargets) {
        applyServerState(serverRadius, serverDrop, mode, blockTarget, oreTargets, false);
    }

    public void applyServerState(int serverRadius, boolean serverDrop, String mode, String blockTarget, String oreTargets, boolean serverRemoteInput) {
        radius.setText(String.valueOf(serverRadius));
        drop = serverDrop;
        remoteInput = serverRemoteInput;
        selectedBlock = blockTarget == null ? "" : blockTarget;
        selectedOres.clear();
        if (oreTargets != null && !oreTargets.isEmpty()) {
            for (String value : oreTargets.split("\\n")) if (!value.isEmpty()) selectedOres.add(value);
        }
        pageType = "ore".equals(mode) ? ORE_PAGE : BLOCK_PAGE;
        rebuildButtons();
    }

    private int gridLeft() {
        return width / 2 - (COLUMNS * CELL_SIZE) / 2;
    }

    private void rebuildButtons() {
        buttonList.clear();
        int left = gridLeft();
        buttonList.add(new GuiButton(0, left, 10, 70, 20, "Block"));
        buttonList.add(new GuiButton(1, left + 75, 10, 70, 20, "Ore"));
        buttonList.add(new GuiButton(9, left + 150, 10, 70, 20, "Containers"));
        buttonList.add(new GuiButton(2, left, 205, 55, 20, "Prev"));
        buttonList.add(new GuiButton(3, left + 60, 205, 55, 20, "Next"));
        buttonList.add(new GuiButton(4, left + 120, 205, 100, 20, "Save target"));
        buttonList.add(new GuiButton(7, left + 110, 230, 110, 20, "Save radius"));
        buttonList.add(new GuiButton(5, left, 255, 105, 20, "Drops: " + drop));
        buttonList.add(new GuiButton(10, left + 110, 255, 110, 20, "Remote input: " + remoteInput));
        buttonList.add(new GuiButton(6, left + 110, 280, 110, 20, "Trace"));
        buttonList.add(new GuiButton(8, left, 280, 220, 20, "Ray select"));
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
        if (button.id == 0 || button.id == 1 || button.id == 9) {
            pageType = button.id == 9 ? CONTAINER_PAGE : button.id;
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
        } else if (button.id == 10) {
            remoteInput = !remoteInput;
            button.displayString = "Remote input: " + remoteInput;
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_REMOTE_INPUT, "", 0, remoteInput));
        } else if (button.id == 6) {
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SCAN, "", 0, drop));
        } else if (button.id == 8) {
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.RAY, "", 0, drop));
        } else if (button.id == 7) {
            try {
                CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_RADIUS, "", Integer.parseInt(radius.getText()), drop));
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (pageType == CONTAINER_PAGE) { if (keyCode == Keyboard.KEY_ESCAPE) super.keyTyped(typedChar, keyCode); return; }
        String before = search.getText();
        search.textboxKeyTyped(typedChar, keyCode);
        radius.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_ESCAPE) super.keyTyped(typedChar, keyCode);
        page = SelectorLogic.pageAfterSearch(page, before, search.getText());
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (pageType != CONTAINER_PAGE) search.mouseClicked(mouseX, mouseY, mouseButton);
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
                } else {
                    selectedBlock = entry.target();
                    pendingBlock = selectedBlock;
                }
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
        drawString(fontRendererObj, pageType == BLOCK_PAGE ? "Blocks" : pageType == ORE_PAGE ? "Ore Dictionary (ore* only, multi-select)" : "Bound containers (rules are read-only)", left, 58, 0xFFFFFF);
        if (pageType != CONTAINER_PAGE) search.drawTextBox();
        if (pageType == CONTAINER_PAGE) drawContainers(left);
        else drawCatalog(left, mouseX, mouseY);
        drawString(fontRendererObj, "Radius", left + 112, 235, 0xFFFFFF);
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
                if (entry.target().equals(selectedBlock)) {
                    drawRect(x, y, x + CELL_SIZE, y + CELL_SIZE, 0x8033ff66);
                    drawRect(x, y, x + CELL_SIZE, y + 1, 0xff55ff88);
                    drawRect(x, y + CELL_SIZE - 1, x + CELL_SIZE, y + CELL_SIZE, 0xff55ff88);
                    drawRect(x, y, x + 1, y + CELL_SIZE, 0xff55ff88);
                    drawRect(x + CELL_SIZE - 1, y, x + CELL_SIZE, y + CELL_SIZE, 0xff55ff88);
                }
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

    public void applyContainerSummaries(List<String> summaries) {
        containerSummaries = summaries == null ? new ArrayList<String>() : new ArrayList<>(summaries);
    }

    private void drawContainers(int left) {
        List<ContainerReference> bindings = new ArrayList<>();
        if (Minecraft.getMinecraft().thePlayer != null && Minecraft.getMinecraft().thePlayer.getHeldItem() != null) {
            bindings = TerminalProfile.bindings(Minecraft.getMinecraft().thePlayer.getHeldItem());
        }
        if (bindings.isEmpty()) {
            drawString(fontRendererObj, "No bound containers", left, 82, 0xAAAAAA);
            return;
        }
        int y = 82;
        for (int i = 0; i < bindings.size() && i < 8; i++) {
            ContainerReference ref = bindings.get(i);
            drawString(fontRendererObj, (i + 1) + ". " + ref.dimension + "  " + ref.x + "," + ref.y + "," + ref.z, left, y, 0xFFFFFF);
            String summary = i < containerSummaries.size() ? containerSummaries.get(i) : "Rules are configured on the container";
            drawString(fontRendererObj, summary.length() > 52 ? summary.substring(0, 52) : summary, left + 8, y + 12, 0xAAAAAA);
            y += 28;
        }
        drawString(fontRendererObj, "Remote input: " + (remoteInput ? "enabled" : "disabled"), left, 200, remoteInput ? 0x55FF55 : 0xAAAAAA);
    }

    private boolean isHovered(int x, int y, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + CELL_SIZE && mouseY >= y && mouseY < y + CELL_SIZE;
    }

}
