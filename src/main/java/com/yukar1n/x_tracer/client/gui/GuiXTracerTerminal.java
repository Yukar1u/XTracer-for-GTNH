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
import net.minecraft.item.ItemStack;
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
    private static final int PANEL_WIDTH = 400;
    private static final int PANEL_HEIGHT = 330;
    private static final int CONTAINER_ROWS = 6;
    private static final int CONTAINER_ROW_HEIGHT = 25;
    private static final int ORE_ROW_HEIGHT = 22;
    private int oreScroll;
    private boolean onlySelected;
    private boolean draggingOreScrollbar;
    private int scrollbarGrab;
    private int limitNoticeTicks;
    private boolean initialized;
    private boolean radiusDirty;
    private boolean targetDirty;
    private int serverRadius;
    private String serverMode = "block";
    private String serverBlockTarget = "";
    private String serverOreTargets = "";
    private static final int SCAN_PAGE = 0;
    private static final int BLOCK_PAGE = 1;
    private static final int ORE_PAGE = 2;
    private static final int CONTAINER_PAGE = 3;

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
    private List<Boolean> containerRouteEnabled = new ArrayList<>();
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
            pageType = SCAN_PAGE;
            drop = "True".equals(config.getDrop);
            selectedBlock = config.targetList.isEmpty() ? "" : config.targetList.get(0);
            for (String name : config.targetOreNameList) selectedOres.add(SelectorLogic.matchKey(name));
        }
        int searchX = pageType == ORE_PAGE ? oreSearchX() : 9;
        int searchWidth = pageType == ORE_PAGE ? panelWidth() - searchX - 54 : 232;
        search = new GuiTextField(fontRendererObj, panelLeft() + searchX,
            panelTop() + (pageType == ORE_PAGE ? 43 : 43), searchWidth, 14);
        search.setMaxStringLength(256);
        search.setText(query);
        search.setFocused(searchFocused);
        radius = new GuiTextField(fontRendererObj, panelLeft() + 42, panelTop() + controlsTop() + 1, 42, 14);
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
        this.serverRadius = serverRadius;
        this.serverMode = mode == null ? "block" : mode;
        this.serverBlockTarget = blockTarget == null ? "" : blockTarget;
        this.serverOreTargets = oreTargets == null ? "" : oreTargets;
        if (!radiusDirty && radius != null) radius.setText(String.valueOf(serverRadius));
        drop = serverDrop;
        remoteInput = serverRemoteInput;
        if (!targetDirty) {
            selectedBlock = blockTarget == null ? "" : blockTarget;
            selectedOres.clear();
            if (oreTargets != null && !oreTargets.isEmpty()) {
                for (String value : oreTargets.split("\\n")) if (!value.isEmpty()) selectedOres.add(SelectorLogic.matchKey(value));
            }
            cachedPageType = -1;
            clampOreScroll();
            if (selectedOres.size() > TerminalRequestMessage.MAX_TARGETS) {
                limitNoticeTicks = 80;
            }
        }
        // A state acknowledgement must not change the tab or discard a local draft.
        rebuildButtons();
    }

    private int oreRows() { return Math.max(1, Math.min(8, (PANEL_HEIGHT - 145) / ORE_ROW_HEIGHT)); }
    private int panelWidth() { return Math.min(PANEL_WIDTH, Math.max(280, width - 12)); }
    private int panelHeight() { return Math.min(PANEL_HEIGHT, Math.max(250, height - 12)); }
    private int panelLeft() { return (width - panelWidth()) / 2; }
    private int panelTop() { return (height - panelHeight()) / 2; }
    private int oreListTop() { return panelTop() + 80; }
    private int oreListHeight() { return oreRows() * ORE_ROW_HEIGHT; }
    private int oreFooter() { return 85 + oreListHeight(); }
    private int controlsTop() { return pageType == ORE_PAGE ? oreFooter() + 38 : 238; }
    private int oreSearchX() { return 12 + (int) (fontRendererObj.getStringWidth(text("ore_search")) * 0.8F); }
    private int containerListTop() { return panelTop() + 80; }
    private int containerRowWidth() { return panelWidth() - 30; }
    private int containerCheckboxLeft() { return panelLeft() + 8 + containerRowWidth() - 27; }
    private int containerCheckboxTop(int row) { return containerListTop() + row * CONTAINER_ROW_HEIGHT + 7; }
    private boolean isPage(int value) { return pageType == value; }
    private int oreTrackX() { return panelLeft() + panelWidth() - 17; }
    private int oreRowWidth() { return panelWidth() - 30; }
    private int maxOreScroll() { return Math.max(0, filteredOres().size() - oreRows()); }
    private void clampOreScroll() { oreScroll = Math.max(0, Math.min(oreScroll, maxOreScroll())); }
    private int oreThumbHeight() {
        return Math.min(oreListHeight(), Math.max(12, oreListHeight() * oreRows() / Math.max(1, filteredOres().size())));
    }
    private int oreThumbTop() {
        return oreListTop() + (maxOreScroll() == 0 ? 0
            : (oreListHeight() - oreThumbHeight()) * oreScroll / maxOreScroll());
    }
    private int gridLeft() { return panelLeft() + 35; }
    private int gridTop() { return panelTop() + 57; }
    private String text(String key) { return I18n.format("xtracer.gui." + key); }
    private String toggleLabel(String key, boolean enabled) {
        return text(key) + ": " + text(enabled ? "on" : "off");
    }

    private void addButton(int id, int x, int y, int width, String label) {
        PanelButton button = new PanelButton(id, panelLeft() + x, panelTop() + y, width, 16, label);
        button.selected = (id == 0 && pageType == SCAN_PAGE) || (id == 1 && pageType == BLOCK_PAGE)
            || (id == 9 && pageType == ORE_PAGE) || (id == 10 && pageType == CONTAINER_PAGE);
        button.enabled = true;
        buttonList.add(button);
    }

    private void rebuildButtons() {
        buttonList.clear();
        int tabWidth = (panelWidth() - 30) / 4;
        addButton(0, 8, 19, tabWidth, text("scan"));
        addButton(1, 13 + tabWidth, 19, tabWidth, text("block_mode"));
        addButton(9, 18 + tabWidth * 2, 19, tabWidth, text("ore_mode"));
        addButton(10, 23 + tabWidth * 3, 19, panelWidth() - 23 - tabWidth * 3, text("containers"));
        if (pageType == BLOCK_PAGE) {
            addButton(2, 8, 159, 38, text("previous"));
            addButton(3, 49, 159, 38, text("next"));
            addButton(4, 159, 159, 83, text("save_target"));
        } else if (pageType == ORE_PAGE) {
            addButton(13, panelWidth() - 49, 42, 41, text("clear_search"));
            addButton(14, panelWidth() - 110, 62, 102, text("only_selected"));
            ((PanelButton) buttonList.get(buttonList.size() - 1)).selected = onlySelected;
            addButton(15, panelWidth() - 204, oreFooter(), 82, text("clear_selection"));
            addButton(4, panelWidth() - 119, oreFooter(), 111, I18n.format("xtracer.gui.save_ores", selectedOres.size()));
        } else if (pageType == CONTAINER_PAGE) {
            addButton(2, 8, 238, 38, text("previous"));
            addButton(3, 49, 238, 38, text("next"));
            addButton(18, 8, 257, 112, toggleLabel("remote_input", remoteInput));
            addButton(19, 8, 276, 115, text("bind_looked_at"));
            addButton(20, 126, 276, 116, text("unbind_looked_at"));
            addButton(21, 8, 295, 112, text("unbind_selected"));
            ((PanelButton) buttonList.get(buttonList.size() - 1)).enabled = !bindings().isEmpty();
        } else if (pageType == SCAN_PAGE) {
            addButton(7, 89, controlsTop(), 45, text("apply_radius"));
            addButton(5, 137, controlsTop(), 105, toggleLabel("drops", drop));
            addButton(8, 123, controlsTop() + 19, 57, text("ray_select"));
            addButton(6, 183, controlsTop() + 19, 59, text("trace"));
        }
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
            if ((query.isEmpty() || entry.searchText().contains(query))
                && (!onlySelected || selectedOres.contains(entry.matchKey))) cachedFilteredOres.add(entry);
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
        if (button.id == 0 || button.id == 1 || button.id == 9 || button.id == 10) {
            pageType = button.id == 0 ? SCAN_PAGE : button.id == 1 ? BLOCK_PAGE
                : button.id == 9 ? ORE_PAGE : CONTAINER_PAGE;
            page = 0;
            draggingOreScrollbar = false;
            initGui();
        } else if (button.id == 13) {
            search.setText("");
            search.setFocused(true);
            oreScroll = 0;
        } else if (button.id == 14) {
            onlySelected = !onlySelected;
            cachedPageType = -1;
            oreScroll = 0;
            rebuildButtons();
        } else if (button.id == 15) {
            selectedOres.clear();
            targetDirty = true;
            cachedPageType = -1;
            limitNoticeTicks = 0;
            oreScroll = 0;
            rebuildButtons();
        } else if (button.id == 21) {
            List<ContainerReference> refs = bindings();
            if (selectedContainer >= 0 && selectedContainer < refs.size()) {
                ContainerReference reference = refs.get(selectedContainer);
                CommonProxy.network.sendToServer(new com.yukar1n.x_tracer.network.TerminalContainerEditMessage(
                    com.yukar1n.x_tracer.network.TerminalContainerEditMessage.REMOVE, reference, false));
            }
        } else if (button.id == 2) {
            if (page > 0) page--;
        } else if (button.id == 3) {
            if (page < lastPage()) page++;
        } else if (button.id == 4) {
            if (pageType == BLOCK_PAGE && selectedBlock != null && !selectedBlock.isEmpty()) {
                CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_BLOCK, selectedBlock, 0, drop));
                targetDirty = false;
            } else if (pageType == ORE_PAGE && !selectedOres.isEmpty()
                && selectedOres.size() <= TerminalRequestMessage.MAX_TARGETS) {
                List<String> targets = new ArrayList<>();
                List<String> names = new ArrayList<>();
                for (OreCatalogEntry entry : XTracerCatalog.ores()) names.add(entry.name);
                for (String key : selectedOres) {
                    String canonical = SelectorLogic.canonical(key, names);
                    targets.add(canonical == null ? key : canonical);
                }
                java.util.Collections.sort(targets);
                CommonProxy.network.sendToServer(new TerminalRequestMessage(targets, drop));
                targetDirty = false;
            }
        } else if (button.id == 5) {
            drop = !drop;
            button.displayString = toggleLabel("drops", drop);
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_DROP, "", 0, drop));
        } else if (button.id == 18) {
            remoteInput = !remoteInput;
            button.displayString = toggleLabel("remote_input", remoteInput);
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_REMOTE_INPUT, "", 0, remoteInput));
        } else if (button.id == 6) {
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SCAN, "", 0, drop));
        } else if (button.id == 8) {
            CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.RAY, "", 0, drop));
        } else if (button.id == 19) {
            net.minecraft.util.MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;
            if (hit != null && hit.typeOfHit == net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) {
                CommonProxy.network.sendToServer(new com.yukar1n.x_tracer.network.TerminalBindingMessage(true, hit.blockX, hit.blockY, hit.blockZ));
                requestStateRefresh();
            }
        } else if (button.id == 20) {
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
        if (pageType == BLOCK_PAGE || pageType == ORE_PAGE) search.textboxKeyTyped(typedChar, keyCode);
        if (pageType == SCAN_PAGE) radius.textboxKeyTyped(typedChar, keyCode);
        if (!radiusBefore.equals(radius.getText())) radiusDirty = true;
        if (keyCode == Keyboard.KEY_ESCAPE) super.keyTyped(typedChar, keyCode);
        if (pageType == BLOCK_PAGE || pageType == ORE_PAGE) {
            page = SelectorLogic.pageAfterSearch(page, before, search.getText());
            if (!before.equals(search.getText())) oreScroll = 0;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean clickedButton = false;
        if (mouseButton == 0) {
            for (Object value : buttonList) {
                GuiButton button = (GuiButton) value;
                if (button.visible && mouseX >= button.xPosition && mouseX < button.xPosition + button.width
                    && mouseY >= button.yPosition && mouseY < button.yPosition + button.height) {
                    clickedButton = true;
                    break;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (clickedButton) return;
        if (pageType == BLOCK_PAGE || pageType == ORE_PAGE) search.mouseClicked(mouseX, mouseY, mouseButton);
        if (pageType == SCAN_PAGE) radius.mouseClicked(mouseX, mouseY, mouseButton);
        if (pageType == ORE_PAGE) {
            if (mouseButton != 0) return;
            if (mouseY >= oreListTop() && mouseY < oreListTop() + oreListHeight()) {
                if (mouseX >= oreTrackX() && mouseX < oreTrackX() + 9 && maxOreScroll() > 0) {
                    scrollbarGrab = mouseY >= oreThumbTop() && mouseY < oreThumbTop() + oreThumbHeight()
                        ? mouseY - oreThumbTop() : oreThumbHeight() / 2;
                    draggingOreScrollbar = true;
                    dragOreScrollbar(mouseY);
                } else if (mouseX >= panelLeft() + 9 && mouseX < panelLeft() + 9 + oreRowWidth()) {
                    int index = oreScroll + (mouseY - oreListTop()) / ORE_ROW_HEIGHT;
                    List<OreCatalogEntry> entries = filteredOres();
                    if (index < entries.size()) {
                        String key = entries.get(index).matchKey;
                        if (!SelectorLogic.toggleOreSelection(selectedOres, key, TerminalRequestMessage.MAX_TARGETS)) {
                            limitNoticeTicks = 80;
                            return;
                        }
                        limitNoticeTicks = 0;
                        targetDirty = true;
                        cachedPageType = -1;
                        clampOreScroll();
                        rebuildButtons();
                    }
                }
            }
            return;
        }
        int left = gridLeft();
        int top = gridTop();
        if (pageType == CONTAINER_PAGE) {
            if (mouseButton != 0) return;
            int row = (mouseY - containerListTop()) / CONTAINER_ROW_HEIGHT;
            int index = page * CONTAINER_ROWS + row;
            int checkboxLeft = containerCheckboxLeft();
            int checkboxTop = containerCheckboxTop(row);
            if (mouseX >= panelLeft() + 8 && mouseX < panelLeft() + 8 + containerRowWidth()
                && mouseY >= containerListTop() && mouseY < containerListTop() + CONTAINER_ROWS * CONTAINER_ROW_HEIGHT
                && index < bindings().size()) {
                selectedContainer = index;
                if (mouseX >= checkboxLeft && mouseX < checkboxLeft + 16
                    && mouseY >= checkboxTop && mouseY < checkboxTop + 11) {
                    ContainerReference reference = bindings().get(index);
                    boolean enabled = index >= containerRouteEnabled.size() || containerRouteEnabled.get(index);
                    CommonProxy.network.sendToServer(new com.yukar1n.x_tracer.network.TerminalContainerEditMessage(
                        com.yukar1n.x_tracer.network.TerminalContainerEditMessage.SET_ENABLED, reference, !enabled));
                }
            }
            return;
        }
        if (pageType == SCAN_PAGE) return;
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
        }
    }

    private void dragOreScrollbar(int mouseY) {
        int travel = oreListHeight() - oreThumbHeight();
        if (travel > 0) {
            oreScroll = Math.round((mouseY - oreListTop() - scrollbarGrab) * (float) maxOreScroll() / travel);
            clampOreScroll();
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long elapsed) {
        super.mouseClickMove(mouseX, mouseY, button, elapsed);
        if (draggingOreScrollbar && button == 0) dragOreScrollbar(mouseY);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        super.mouseMovedOrUp(mouseX, mouseY, button);
        if (button == 0) draggingOreScrollbar = false;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        search.updateCursorCounter();
        radius.updateCursorCounter();
        if (limitNoticeTicks > 0) limitNoticeTicks--;
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            if (pageType == ORE_PAGE) {
                int mouseX = Mouse.getEventX() * width / mc.displayWidth;
                int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
                if (mouseX >= panelLeft() + 8 && mouseX < panelLeft() + panelWidth() - 8
                    && mouseY >= oreListTop() && mouseY < oreListTop() + oreListHeight()) {
                    oreScroll += wheel < 0 ? 3 : -3;
                    clampOreScroll();
                }
                return;
            }
            if ((pageType == BLOCK_PAGE || pageType == CONTAINER_PAGE) && wheel < 0 && page < lastPage()) page++;
            if ((pageType == BLOCK_PAGE || pageType == CONTAINER_PAGE) && wheel > 0 && page > 0) page--;
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
        IndustrialPanel.bevel(left, top, panelWidth(), panelHeight(), 0xFF252E34, false);
        IndustrialPanel.bevel(left + 5, top + 5, panelWidth() - 10, 11, 0xFF18242B, true);
        IndustrialPanel.fitText(fontRendererObj, text("terminal"), left + 10, top + 7, panelWidth() - 20, IndustrialPanel.ACCENT, 0.8F);
        if (pageType == BLOCK_PAGE || pageType == ORE_PAGE) search.drawTextBox();
        else if (pageType == CONTAINER_PAGE) IndustrialPanel.fitText(fontRendererObj, text("containers_read_only"), left + 9, top + 42, panelWidth() - 18, IndustrialPanel.MUTED, 0.8F);
        else drawScanStatus(left, top);
        if (pageType == ORE_PAGE) {
            clampOreScroll();
            IndustrialPanel.fitText(fontRendererObj, text("ore_search"), left + 9, top + 46, oreSearchX() - 12, IndustrialPanel.MUTED, 0.8F);
            IndustrialPanel.fitText(fontRendererObj, I18n.format("xtracer.gui.ore_matches", filteredOres().size()),
                left + 10, top + 67, panelWidth() - 126, IndustrialPanel.MUTED, 0.8F);
            IndustrialPanel.bevel(left + 8, oreListTop() - 1, panelWidth() - 16, oreListHeight() + 2, 0xFF172127, true);
            IndustrialPanel.fitText(fontRendererObj, I18n.format("xtracer.gui.ore_selected", selectedOres.size(), TerminalRequestMessage.MAX_TARGETS),
                left + 9, top + oreFooter() + 5, panelWidth() - 218, IndustrialPanel.ACCENT, 0.8F);
            if (limitNoticeTicks > 0 || selectedOres.size() > TerminalRequestMessage.MAX_TARGETS) {
                IndustrialPanel.fitText(fontRendererObj, I18n.format("xtracer.gui.ore_limit", TerminalRequestMessage.MAX_TARGETS),
                    left + 9, top + oreFooter() + 21, panelWidth() - 18, 0xFFBB77, 0.8F);
            }
            drawRect(left + 8, top + controlsTop() - 4, left + panelWidth() - 8, top + controlsTop() - 3, 0xFF52636B);
        } else if (pageType == CONTAINER_PAGE) {
            IndustrialPanel.bevel(left + 8, containerListTop() - 1, panelWidth() - 16,
                CONTAINER_ROWS * CONTAINER_ROW_HEIGHT + 2, 0xFF172127, true);
            IndustrialPanel.fitText(fontRendererObj, (page + 1) + " / " + (lastPage() + 1),
                left + panelWidth() - 74, top + 238, 62, IndustrialPanel.MUTED, 0.8F);
        } else if (pageType == BLOCK_PAGE) {
            IndustrialPanel.bevel(left + 8, gridTop() - 1, 234, 102, 0xFF172127, true);
            IndustrialPanel.fitText(fontRendererObj, (page + 1) + " / " + (lastPage() + 1), left + 92, top + 163, 62, IndustrialPanel.MUTED, 0.8F);
        }
        if (pageType == SCAN_PAGE) {
                IndustrialPanel.fitText(fontRendererObj, text("radius"), left + 8, top + controlsTop() + 4, 31, IndustrialPanel.MUTED, 0.75F);
            radius.drawTextBox();
        }
        for (Object value : buttonList) {
            GuiButton button = (GuiButton) value;
            if (button.id == 2) button.enabled = page > 0;
            if (button.id == 3) button.enabled = page < lastPage();
            if (button.id == 4 && pageType == ORE_PAGE) button.enabled = !selectedOres.isEmpty()
                && selectedOres.size() <= TerminalRequestMessage.MAX_TARGETS;
            if (button.id == 15) button.enabled = !selectedOres.isEmpty();
            if (button.id == 13) button.enabled = !search.getText().isEmpty();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (pageType == CONTAINER_PAGE) drawContainers(left + 8, mouseX, mouseY);
        else if (pageType == ORE_PAGE) drawOreList(mouseX, mouseY);
        else if (pageType == BLOCK_PAGE) drawCatalog(gridLeft(), mouseX, mouseY);
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
        }
        } finally {
            RenderHelper.disableStandardItemLighting();
        }
        if (hover != null) drawHoveringText(hover, mouseX, mouseY, fontRendererObj);
    }

    private void drawOreList(int mouseX, int mouseY) {
        List<OreCatalogEntry> entries = filteredOres();
        int left = panelLeft() + 9;
        OreCatalogEntry hovered = null;
        for (int row = 0; row < oreRows() && oreScroll + row < entries.size(); row++) {
            OreCatalogEntry entry = entries.get(oreScroll + row);
            int y = oreListTop() + row * ORE_ROW_HEIGHT;
            boolean selected = selectedOres.contains(entry.matchKey);
            boolean hover = mouseX >= left && mouseX < left + oreRowWidth()
                && mouseY >= y && mouseY < y + ORE_ROW_HEIGHT;
            IndustrialPanel.bevel(left, y, oreRowWidth(), ORE_ROW_HEIGHT,
                selected ? 0xFF29474F : hover ? 0xFF30434B : 0xFF202D34, true);
            if (selected) {
                drawRect(left, y, left + oreRowWidth(), y + 1, 0xFF55C5D1);
                drawRect(left, y + ORE_ROW_HEIGHT - 1, left + oreRowWidth(), y + ORE_ROW_HEIGHT, 0xFF55C5D1);
                drawRect(left, y, left + 1, y + ORE_ROW_HEIGHT, 0xFF55C5D1);
                drawRect(left + oreRowWidth() - 1, y, left + oreRowWidth(), y + ORE_ROW_HEIGHT, 0xFF55C5D1);
            }
            IndustrialPanel.bevel(left + 5, y + 6, 10, 10, 0xFF14232A, true);
            if (selected) {
                for (int i = 0; i < 3; i++) drawRect(left + 7 + i, y + 10 + i, left + 8 + i, y + 12 + i, 0xFF55C5D1);
                for (int i = 0; i < 5; i++) drawRect(left + 9 + i, y + 12 - i, left + 10 + i, y + 14 - i, 0xFF55C5D1);
            }
            drawOreIcon(entry.representative(), left + 20, y + 3);
            String count = I18n.format("xtracer.gui.ore_members", entry.memberCount());
            int countWidth = (int) Math.ceil(fontRendererObj.getStringWidth(count) * 0.8F);
            int countX = left + oreRowWidth() - countWidth - 6;
            IndustrialPanel.fitText(fontRendererObj, entry.name, left + 42, y + 7, countX - left - 48,
                selected ? IndustrialPanel.ACCENT : IndustrialPanel.TEXT, 1.0F);
            IndustrialPanel.fitText(fontRendererObj, count, countX, y + 8, countWidth, IndustrialPanel.MUTED, 0.8F);
            if (hover) hovered = entry;
        }
        if (entries.isEmpty()) {
            IndustrialPanel.fitText(fontRendererObj, text("no_ores"), left + 8, oreListTop() + 8,
                oreRowWidth() - 16, IndustrialPanel.MUTED, 0.9F);
        }
        IndustrialPanel.bevel(oreTrackX(), oreListTop(), 8, oreListHeight(), 0xFF111B20, true);
        if (maxOreScroll() > 0) {
            IndustrialPanel.bevel(oreTrackX() + 1, oreThumbTop(), 6, oreThumbHeight(), 0xFF43828D, false);
        }
        if (hovered != null && !draggingOreScrollbar) drawOreTooltip(hovered, mouseX, mouseY);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
    }

    private void drawOreIcon(ItemStack stack, int x, int y) {
        if (stack == null) return;
        org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ENABLE_BIT | org.lwjgl.opengl.GL11.GL_CURRENT_BIT);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        RenderHelper.enableGUIStandardItemLighting();
        try {
            renderItem.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, x, y);
        } catch (Throwable ignored) {
            drawRect(x + 4, y + 4, x + 12, y + 12, 0xFF52636B);
        } finally {
            RenderHelper.disableStandardItemLighting();
            org.lwjgl.opengl.GL11.glPopAttrib();
        }
    }

    private void drawOreTooltip(OreCatalogEntry entry, int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        lines.add(entry.name);
        String representative = text("ore_no_icon");
        if (entry.representative() != null) {
            try { representative = entry.representative().getDisplayName(); }
            catch (Throwable ignored) {}
        }
        lines.add(I18n.format("xtracer.gui.ore_representative", representative));
        lines.add(I18n.format("xtracer.gui.ore_members", entry.memberCount()));
        List<String> wrapped = new ArrayList<>();
        int tooltipWidth = Math.min(280, width - 20);
        for (String line : lines) wrapped.addAll(fontRendererObj.listFormattedStringToWidth(line, tooltipWidth - 12));
        int actualWidth = entry.preview().size() * 20;
        for (String line : wrapped) actualWidth = Math.max(actualWidth, fontRendererObj.getStringWidth(line));
        tooltipWidth = Math.min(tooltipWidth, actualWidth + 12);
        int tooltipHeight = wrapped.size() * 11 + (entry.preview().isEmpty() ? 0 : 21) + 10;
        int x = Math.max(4, Math.min(mouseX + 12, width - tooltipWidth - 4));
        int y = mouseY + 12;
        if (y + tooltipHeight > height - 4) y = Math.max(4, mouseY - tooltipHeight - 6);
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glTranslatef(0, 0, 300);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        IndustrialPanel.bevel(x, y, tooltipWidth, tooltipHeight, 0xFF18262D, false);
        for (int i = 0; i < wrapped.size(); i++) {
            fontRendererObj.drawString(wrapped.get(i), x + 6, y + 5 + i * 11,
                i == 0 ? IndustrialPanel.ACCENT : IndustrialPanel.TEXT);
        }
        int iconX = x + 6;
        for (ItemStack stack : entry.preview()) {
            drawOreIcon(stack, iconX, y + tooltipHeight - 22);
            iconX += 20;
        }
        org.lwjgl.opengl.GL11.glPopMatrix();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
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
        applyContainerSummaries(summaries, null);
    }

    public void applyContainerSummaries(List<String> summaries, List<Boolean> enabled) {
        containerSummaries = summaries == null ? new ArrayList<String>() : new ArrayList<>(summaries);
        containerRouteEnabled = enabled == null ? new ArrayList<Boolean>() : new ArrayList<>(enabled);
        while (containerRouteEnabled.size() < containerSummaries.size()) containerRouteEnabled.add(true);
        selectedContainer = Math.max(0, Math.min(selectedContainer, Math.max(0, containerSummaries.size() - 1)));
    }

    private void drawScanStatus(int left, int top) {
        IndustrialPanel.fitText(fontRendererObj, text("scan_status"), left + 9, top + 40,
            panelWidth() - 18, IndustrialPanel.ACCENT, 0.9F);
        String target;
        if ("ore".equals(serverMode)) target = I18n.format("xtracer.gui.scan_target_ore", serverOreTargets.isEmpty() ? 0 : serverOreTargets.split("\\n").length);
        else target = serverBlockTarget.isEmpty() ? text("none") : serverBlockTarget;
        IndustrialPanel.fitText(fontRendererObj, I18n.format("xtracer.gui.scan_mode", serverMode), left + 12, top + 62,
            panelWidth() - 24, IndustrialPanel.TEXT, 0.8F);
        IndustrialPanel.fitText(fontRendererObj, I18n.format("xtracer.gui.scan_target", target), left + 12, top + 79,
            panelWidth() - 24, IndustrialPanel.TEXT, 0.8F);
        IndustrialPanel.fitText(fontRendererObj, I18n.format("xtracer.gui.scan_radius", serverRadius), left + 12, top + 96,
            panelWidth() - 24, IndustrialPanel.TEXT, 0.8F);
        IndustrialPanel.fitText(fontRendererObj, I18n.format("xtracer.gui.scan_drops", text(drop ? "on" : "off")), left + 12, top + 113,
            panelWidth() - 24, IndustrialPanel.TEXT, 0.8F);
        IndustrialPanel.fitText(fontRendererObj, I18n.format("xtracer.gui.scan_remote", text(remoteInput ? "on" : "off")), left + 12, top + 130,
            panelWidth() - 24, IndustrialPanel.MUTED, 0.8F);
    }

    private void drawContainers(int left, int mouseX, int mouseY) {
        List<ContainerReference> bindings = bindings();
        if (bindings.isEmpty()) {
            IndustrialPanel.fitText(fontRendererObj, text("container_empty"), left + 6, containerListTop() + 10,
                panelWidth() - 28, IndustrialPanel.MUTED, 0.8F);
            return;
        }
        selectedContainer = Math.max(0, Math.min(selectedContainer, bindings.size() - 1));
        List<String> hover = null;
        for (int row = 0; row < CONTAINER_ROWS && page * CONTAINER_ROWS + row < bindings.size(); row++) {
            int index = page * CONTAINER_ROWS + row;
            int y = containerListTop() + row * CONTAINER_ROW_HEIGHT;
            ContainerReference ref = bindings.get(index);
            int rowWidth = containerRowWidth();
            IndustrialPanel.bevel(left, y, rowWidth, CONTAINER_ROW_HEIGHT,
                index == selectedContainer ? 0xFF29474F : 0xFF202D34, true);
            if (index == selectedContainer) {
                drawRect(left, y, left + rowWidth, y + 1, 0xFF55C5D1);
                drawRect(left, y + CONTAINER_ROW_HEIGHT - 1, left + rowWidth, y + CONTAINER_ROW_HEIGHT, 0xFF55C5D1);
            }
            String location = (index + 1) + ". " + ref.dimension + "  " + ref.x + "," + ref.y + "," + ref.z;
            String summary = index < containerSummaries.size() ? containerSummaries.get(index) : text("container_rules_hint");
            IndustrialPanel.fitText(fontRendererObj, location, left + 5, y + 3, rowWidth - 46,
                index == selectedContainer ? IndustrialPanel.ACCENT : IndustrialPanel.TEXT, 0.8F);
            IndustrialPanel.fitText(fontRendererObj, summary, left + 5, y + 13, rowWidth - 46, IndustrialPanel.MUTED, 0.75F);
            IndustrialPanel.bevel(left + rowWidth - 27, y + 7, 16, 11, 0xFF14232A, true);
            boolean routeEnabled = index >= containerRouteEnabled.size() || containerRouteEnabled.get(index);
            if (routeEnabled) {
                drawRect(left + rowWidth - 24, y + 11, left + rowWidth - 21, y + 14, 0xFF55C5D1);
                drawRect(left + rowWidth - 22, y + 9, left + rowWidth - 19, y + 13, 0xFF55C5D1);
                drawRect(left + rowWidth - 20, y + 7, left + rowWidth - 17, y + 12, 0xFF55C5D1);
            }
            if (mouseX >= left && mouseX < left + rowWidth && mouseY >= y && mouseY < y + CONTAINER_ROW_HEIGHT) {
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
