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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GuiXTracerTerminal extends GuiScreen {
    private static final int PAGE_SIZE = 6;
    private static final int BLOCK_PAGE = 0;
    private static final int ORE_PAGE = 1;

    private GuiTextField search;
    private GuiTextField radius;
    private int pageType;
    private int page;
    private boolean drop;
    private String selectedBlock;
    private final Set<String> selectedOres = new HashSet<>();
    private final RenderItem renderItem = new RenderItem();

    @Override
    public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        XTracerCatalog.ensureBuilt();
        ClientConfig config = Config.getPlayerConfig(Minecraft.getMinecraft().thePlayer.getUniqueID());
        pageType = "ore".equals(config.curMatchMode) ? ORE_PAGE : BLOCK_PAGE;
        drop = "True".equals(config.getDrop);
        selectedBlock = config.targetList.isEmpty() ? "" : config.targetList.get(0);
        selectedOres.addAll(config.targetOreNameList);
        int left = width / 2 - 110;
        search = new GuiTextField(fontRendererObj, left, 34, 220, 20);
        radius = new GuiTextField(fontRendererObj, left, 205, 55, 20);
        radius.setText(String.valueOf(config.radius));
        rebuildButtons();
    }

    private void rebuildButtons() {
        buttonList.clear();
        int left = width / 2 - 110;
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

    private List<BlockCatalogEntry> filteredBlocks() {
        String query = search.getText().toLowerCase();
        List<BlockCatalogEntry> result = new ArrayList<>();
        for (BlockCatalogEntry entry : XTracerCatalog.blocks()) {
            if (query.isEmpty() || entry.searchText().contains(query)) result.add(entry);
        }
        return result;
    }

    private List<OreCatalogEntry> filteredOres() {
        String query = search.getText().toLowerCase();
        List<OreCatalogEntry> result = new ArrayList<>();
        for (OreCatalogEntry entry : XTracerCatalog.ores()) {
            if (query.isEmpty() || entry.searchText().contains(query)) result.add(entry);
        }
        return result;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        int left = width / 2 - 110;
        if (button.id == 0 || button.id == 1) {
            pageType = button.id;
            page = 0;
            rebuildButtons();
        } else if (button.id == 2) {
            if (page > 0) page--;
        } else if (button.id == 3) {
            int size = pageType == BLOCK_PAGE ? filteredBlocks().size() : filteredOres().size();
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
        search.textboxKeyTyped(typedChar, keyCode);
        radius.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_ESCAPE) super.keyTyped(typedChar, keyCode);
        page = 0;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        search.mouseClicked(mouseX, mouseY, mouseButton);
        radius.mouseClicked(mouseX, mouseY, mouseButton);
        int left = width / 2 - 110;
        if (mouseX < left || mouseX > left + 220 || mouseY < 60 || mouseY >= 60 + PAGE_SIZE * 23) return;
        int index = page * PAGE_SIZE + (mouseY - 60) / 23;
        if (pageType == BLOCK_PAGE) {
            List<BlockCatalogEntry> entries = filteredBlocks();
            if (index < entries.size()) selectedBlock = entries.get(index).target();
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
            int size = pageType == BLOCK_PAGE ? filteredBlocks().size() : filteredOres().size();
            if (wheel < 0 && (page + 1) * PAGE_SIZE < size) page++;
            if (wheel > 0 && page > 0) page--;
        }
    }

    @Override
    public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int left = width / 2 - 110;
        drawCenteredString(fontRendererObj, "XTracer Terminal", width / 2, 0, 0xFFFFFF);
        drawString(fontRendererObj, pageType == BLOCK_PAGE ? "Placeable blocks" : "Ore Dictionary (multi-select)", left, 58, 0xFFFFFF);
        search.drawTextBox();
        drawCatalog(left, mouseX, mouseY);
        drawString(fontRendererObj, "Radius", left + 65, 210, 0xFFFFFF);
        radius.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCatalog(int left, int mouseX, int mouseY) {
        RenderHelper.enableGUIStandardItemLighting();
        if (pageType == BLOCK_PAGE) {
            List<BlockCatalogEntry> entries = filteredBlocks();
            for (int i = 0; i < PAGE_SIZE && page * PAGE_SIZE + i < entries.size(); i++) {
                BlockCatalogEntry entry = entries.get(page * PAGE_SIZE + i);
                int y = 60 + i * 23;
                drawString(fontRendererObj, entry.target().equals(selectedBlock) ? ">" : " ", left, y + 4, 0x55FF55);
                renderItem.renderItemAndEffectIntoGUI(fontRendererObj, Minecraft.getMinecraft().getTextureManager(), entry.stack, left + 12, y);
                drawString(fontRendererObj, entry.displayName, left + 35, y + 4, 0xFFFFFF);
            }
        } else {
            List<OreCatalogEntry> entries = filteredOres();
            for (int i = 0; i < PAGE_SIZE && page * PAGE_SIZE + i < entries.size(); i++) {
                OreCatalogEntry entry = entries.get(page * PAGE_SIZE + i);
                int y = 60 + i * 23;
                drawString(fontRendererObj, selectedOres.contains(entry.name) ? "[x]" : "[ ]", left, y + 4, 0xFFFFFF);
                renderItem.renderItemAndEffectIntoGUI(fontRendererObj, Minecraft.getMinecraft().getTextureManager(), entry.representative, left + 28, y);
                drawString(fontRendererObj, entry.name, left + 52, y + 4, 0xFFFFFF);
            }
        }
        RenderHelper.disableStandardItemLighting();
    }
}
