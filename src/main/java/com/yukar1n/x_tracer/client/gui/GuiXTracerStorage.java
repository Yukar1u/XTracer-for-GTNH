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
import java.util.Arrays;
import java.util.List;

public class GuiXTracerStorage extends GuiContainer {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 300;
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
        name = field(8, 20, 204, 18);
        accepts = field(8, 42, 204, 18);
        excludes = field(8, 64, 204, 18);
        buttonList.add(new GuiButton(0, guiLeft + 8, guiTop + 86, 98, 20, "Save rules"));
        buttonList.add(new GuiButton(1, guiLeft + 114, guiTop + 86, 98, 20, "Refresh"));
        buttonList.add(new GuiButton(2, guiLeft + 8, guiTop + 108, 98, 20, "Prev items"));
        buttonList.add(new GuiButton(3, guiLeft + 114, guiTop + 108, 98, 20, "Next items"));
        name.setText(tile.getCustomName());
        accepts.setText(join(tile.getAccepts()));
        excludes.setText(join(tile.getExcludes()));
        revision = tile.getRevision();
        requestContents();
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

    public void applyContents(int page, long revision, List<ContainerContentsMessage.Entry> values) {
        contentPage = page;
        contentRevision = revision;
        contents.clear();
        if (values != null) contents.addAll(values);
    }

    private static List<String> split(String value) {
        if (value == null || value.trim().isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(value.split(",")));
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
            if (contents.size() == ContainerContentsMessage.PAGE_SIZE) { contentPage++; requestContents(); }
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
        if (localY >= 132 && localY < 176 && localX >= 8 && localX < 206) {
            int column = (localX - 8) / 22;
            int row = (localY - 132) / 22;
            int index = contentPage * ContainerContentsMessage.PAGE_SIZE + row * 9 + column;
            if (index < contents.size() + contentPage * ContainerContentsMessage.PAGE_SIZE) {
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
        drawRect(guiLeft + 7, guiTop + 7, guiLeft + PANEL_WIDTH - 7, guiTop + 108, 0xffeeeeee);
        drawRect(guiLeft + 7, guiTop + 108, guiLeft + PANEL_WIDTH - 7, guiTop + PANEL_HEIGHT - 7, 0xff9b9b9b);
    }

    @Override protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRendererObj.drawString("Name", 8, 9, 0x202020);
        fontRendererObj.drawString("Accept terms (comma separated)", 8, 31, 0x202020);
        fontRendererObj.drawString("Exclude terms (comma separated)", 8, 53, 0x202020);
        fontRendererObj.drawString("Storage is output-only", 8, 112, 0x202020);
        fontRendererObj.drawString("Stored item types: " + tile.getSizeInventory(), 8, 124, 0x202020);
        fontRendererObj.drawString("Page " + (contentPage + 1), 174, 124, 0x202020);
        for (int i = 0; i < contents.size() && i < ContainerContentsMessage.PAGE_SIZE; i++) {
            ContainerContentsMessage.Entry entry = contents.get(i);
            Item item = (Item) Item.itemRegistry.getObject(entry.itemId);
            if (item == null) continue;
            int x = 10 + (i % 9) * 22;
            int y = 132 + (i / 9) * 22;
            net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(item, 1, entry.damage);
            RenderHelper.enableGUIStandardItemLighting();
            renderItem.renderItemAndEffectIntoGUI(fontRendererObj, Minecraft.getMinecraft().getTextureManager(), stack, x, y);
            RenderHelper.disableStandardItemLighting();
            fontRendererObj.drawString(formatCount(entry.count), x, y + 15, 0x202020);
        }
        fontRendererObj.drawString("Player inventory", 8, 130, 0x202020);
        name.drawTextBox();
        accepts.drawTextBox();
        excludes.drawTextBox();
    }
}
