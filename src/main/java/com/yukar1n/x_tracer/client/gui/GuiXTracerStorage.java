package com.yukar1n.x_tracer.client.gui;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.container.ContainerXTracerStorage;
import com.yukar1n.x_tracer.network.ContainerEditMessage;
import com.yukar1n.x_tracer.network.ContainerContentsMessage;
import com.yukar1n.x_tracer.network.ContainerContentsRequestMessage;
import com.yukar1n.x_tracer.network.ContainerExtractMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.Item;
import com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class GuiXTracerStorage extends GuiContainer {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 342;
    private final TileEntityXTracerContainer tile;
    private GuiTextField name;
    private GuiTextField accepts;
    private GuiTextField excludes;
    private long revision;
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

    @Override public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        name = field(8, 18, 204, 18);
        accepts = field(8, 50, 204, 18);
        excludes = field(8, 82, 204, 18);
        buttonList.add(new GuiButton(0, guiLeft + 8, guiTop + 108, 98, 20, "Save rules"));
        buttonList.add(new GuiButton(1, guiLeft + 114, guiTop + 108, 98, 20, "Refresh"));
        buttonList.add(new GuiButton(2, guiLeft + 8, guiTop + 130, 98, 20, "Prev page"));
        buttonList.add(new GuiButton(3, guiLeft + 114, guiTop + 130, 98, 20, "Next page"));
        name.setText(tile.getCustomName());
        accepts.setText(join(tile.getAccepts()));
        excludes.setText(join(tile.getExcludes()));
        revision = tile.getRevision();
        requestContents();
        updatePagingButtons();
        CommonProxy.network.sendToServer(ContainerEditMessage.request(tile.xCoord, tile.yCoord, tile.zCoord));
    }

    private GuiTextField field(int x, int y, int width, int height) {
        return new GuiTextField(fontRendererObj, x, y, width, height);
    }

    public boolean isContainer(int x, int y, int z) {
        return tile.xCoord == x && tile.yCoord == y && tile.zCoord == z;
    }

    public void applyState(String value, long stateRevision, List<String> accept, List<String> exclude) {
        if (name == null || accepts == null || excludes == null) return;
        revision = stateRevision;
        acceptValues = accept == null ? new ArrayList<String>() : new ArrayList<>(accept);
        excludeValues = exclude == null ? new ArrayList<String>() : new ArrayList<>(exclude);
        name.setText(value == null ? "" : value);
        accepts.setText(join(accept));
        excludes.setText(join(exclude));
    }

    private static String join(List<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append(",");
            result.append(value);
        }
        return result.toString();
    }

    private void requestContents() {
        CommonProxy.network.sendToServer(new ContainerContentsRequestMessage(tile.xCoord, tile.yCoord, tile.zCoord, contentPage));
    }

    public void applyContents(int page, long revision, int totalEntries, List<ContainerContentsMessage.Entry> values) {
        contentPage = Math.max(0, page);
        contentRevision = revision;
        this.totalEntries = Math.max(0, totalEntries);
        contents.clear();
        if (values != null) contents.addAll(values);
        updatePagingButtons();
    }

    private static List<String> split(String value) {
        if (value == null || value.trim().isEmpty()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String part : value.split("[,;|，；]+")) {
            String normalized = part.trim();
            if (!normalized.isEmpty() && !result.contains(normalized)) result.add(normalized);
        }
        return result;
    }

    private int totalPages() {
        return Math.max(1, (totalEntries + ContainerContentsMessage.PAGE_SIZE - 1) / ContainerContentsMessage.PAGE_SIZE);
    }

    private void updatePagingButtons() {
        for (Object value : buttonList) {
            GuiButton button = (GuiButton) value;
            if (button.id == 2) button.enabled = contentPage > 0;
            if (button.id == 3) button.enabled = contentPage + 1 < totalPages();
        }
    }

    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            CommonProxy.network.sendToServer(new ContainerEditMessage(ContainerEditMessage.SAVE,
                tile.xCoord, tile.yCoord, tile.zCoord, revision, name.getText(),
                split(accepts.getText()), split(excludes.getText())));
        } else if (button.id == 1) {
            requestContents();
            CommonProxy.network.sendToServer(ContainerEditMessage.request(tile.xCoord, tile.yCoord, tile.zCoord));
        } else if (button.id == 2) {
            if (contentPage > 0) { contentPage--; requestContents(); }
        } else if (button.id == 3) {
            if (contentPage + 1 < totalPages()) { contentPage++; requestContents(); }
        }
    }

    @Override protected void keyTyped(char typedChar, int keyCode) {
        if (name.textboxKeyTyped(typedChar, keyCode)
            || accepts.textboxKeyTyped(typedChar, keyCode)
            || excludes.textboxKeyTyped(typedChar, keyCode)) return;
        super.keyTyped(typedChar, keyCode);
    }

    @Override protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        int localX = mouseX - guiLeft;
        int localY = mouseY - guiTop;
        if (localY >= 174 && localY < 222 && localX >= 8 && localX < 206) {
            int column = (localX - 8) / 22;
            int row = (localY - 174) / 24;
            int localIndex = row * 9 + column;
            if (localIndex < contents.size()) {
                int index = contentPage * ContainerContentsMessage.PAGE_SIZE + localIndex;
                CommonProxy.network.sendToServer(new ContainerExtractMessage(tile.xCoord, tile.yCoord, tile.zCoord, index, 64, contentRevision));
                requestContents();
                return;
            }
        }
        name.mouseClicked(localX, localY, mouseButton);
        accepts.mouseClicked(localX, localY, mouseButton);
        excludes.mouseClicked(localX, localY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private static String formatCount(long count) {
        if (count >= 1000000000L) return String.format("%.1fB", count / 1000000000D);
        if (count >= 1000000L) return String.format("%.1fM", count / 1000000D);
        if (count >= 1000L) return String.format("%.1fK", count / 1000D);
        return String.valueOf(count);
    }

    @Override public void updateScreen() {
        super.updateScreen();
        name.updateCursorCounter();
        accepts.updateCursorCounter();
        excludes.updateCursorCounter();
    }

    @Override public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        drawRect(guiLeft, guiTop, guiLeft + PANEL_WIDTH, guiTop + PANEL_HEIGHT, 0xffc6c6c6);
        drawRect(guiLeft + 7, guiTop + 7, guiLeft + PANEL_WIDTH - 7, guiTop + 152, 0xffeeeeee);
        drawRect(guiLeft + 7, guiTop + 152, guiLeft + PANEL_WIDTH - 7, guiTop + PANEL_HEIGHT - 7, 0xff9b9b9b);
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString("Name", 8, 8, 0x202020);
        fontRendererObj.drawString("Accept tags (use , ; or |)", 8, 40, 0x202020);
        fontRendererObj.drawString("Exclude tags (use , ; or |)", 8, 72, 0x202020);
        fontRendererObj.drawString("Storage is output-only", 8, 154, 0x202020);
        fontRendererObj.drawString("Stored item types: " + totalEntries, 8, 164, 0x202020);
        fontRendererObj.drawString("Page " + (contentPage + 1) + "/" + totalPages(), 158, 164, 0x202020);
        for (int i = 0; i < contents.size() && i < ContainerContentsMessage.PAGE_SIZE; i++) {
            ContainerContentsMessage.Entry entry = contents.get(i);
            Item item = (Item) Item.itemRegistry.getObject(entry.itemId);
            if (item == null) continue;
            int x = 8 + (i % 9) * 22;
            int y = 174 + (i / 9) * 24;
            drawRect(x, y, x + 18, y + 18, 0xff373737);
            drawRect(x + 1, y + 1, x + 17, y + 17, 0xff8b8b8b);
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item, 1, entry.damage);
            RenderHelper.enableGUIStandardItemLighting();
            renderItem.renderItemAndEffectIntoGUI(fontRendererObj, Minecraft.getMinecraft().getTextureManager(), stack, x + 1, y + 1);
            RenderHelper.disableStandardItemLighting();
            fontRendererObj.drawString(formatCount(entry.count), x + 1, y + 14, 0xFFFFFF);
        }
        fontRendererObj.drawString("Player inventory", 8, 238, 0x202020);
        name.drawTextBox();
        accepts.drawTextBox();
        excludes.drawTextBox();
    }
}
