package com.yukar1n.x_tracer.client.gui;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.container.ContainerXTracerStorage;
import com.yukar1n.x_tracer.network.ContainerEditMessage;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiXTracerStorage extends GuiContainer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    private final int rows;
    private GuiTextField name;
    private GuiTextField accepts;
    private GuiTextField excludes;
    private long revision;
    private List<String> acceptValues = new ArrayList<>();
    private List<String> excludeValues = new ArrayList<>();

    public GuiXTracerStorage(InventoryPlayer inventory, com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer tile) {
        super(new ContainerXTracerStorage(inventory, tile));
        rows = Math.max(1, Math.min(6, (tile.getSizeInventory() + 8) / 9));
        xSize = 220;
        ySize = 166 + rows * 18;
    }

    @Override public void initGui() {
        super.initGui(); Keyboard.enableRepeatEvents(true);
        int left = guiLeft + 8;
        name = new GuiTextField(fontRendererObj, left, guiTop + 18, 204, 18);
        accepts = new GuiTextField(fontRendererObj, left, guiTop + 40, 204, 18);
        excludes = new GuiTextField(fontRendererObj, left, guiTop + 62, 204, 18);
        buttonList.add(new GuiButton(0, guiLeft + 8, guiTop + 84, 98, 20, "Save rules"));
        buttonList.add(new GuiButton(1, guiLeft + 114, guiTop + 84, 98, 20, "Refresh"));
        com.yukar1n.x_tracer.tileentity.TileEntityXTracerContainer tile = ((ContainerXTracerStorage) inventorySlots).tile;
        name.setText(tile.getCustomName()); accepts.setText(join(tile.getAccepts())); excludes.setText(join(tile.getExcludes())); revision = tile.getRevision();
        CommonProxy.network.sendToServer(ContainerEditMessage.request(tile.xCoord, tile.yCoord, tile.zCoord));
    }

    public void applyState(String value, long stateRevision, List<String> accept, List<String> exclude) {
        revision = stateRevision; acceptValues = new ArrayList<>(accept); excludeValues = new ArrayList<>(exclude);
        name.setText(value); accepts.setText(join(accept)); excludes.setText(join(exclude));
    }
    private static String join(List<String> values) { StringBuilder b=new StringBuilder(); for(String v:values){if(b.length()>0)b.append(",");b.append(v);} return b.toString(); }
    private static List<String> split(String value) { if(value==null||value.trim().isEmpty())return new ArrayList<>(); return new ArrayList<>(Arrays.asList(value.split(","))); }

    @Override protected void actionPerformed(GuiButton button) {
        ContainerXTracerStorage container = (ContainerXTracerStorage) inventorySlots;
        if (button.id == 0) CommonProxy.network.sendToServer(new ContainerEditMessage(ContainerEditMessage.SAVE,
            container.tile.xCoord, container.tile.yCoord, container.tile.zCoord, revision, name.getText(), split(accepts.getText()), split(excludes.getText())));
        else if (button.id == 1) CommonProxy.network.sendToServer(ContainerEditMessage.request(container.tile.xCoord, container.tile.yCoord, container.tile.zCoord));
    }
    @Override protected void keyTyped(char typedChar, int keyCode) {
        if (name.textboxKeyTyped(typedChar,keyCode) || accepts.textboxKeyTyped(typedChar,keyCode) || excludes.textboxKeyTyped(typedChar,keyCode)) return;
        super.keyTyped(typedChar,keyCode);
    }
    @Override protected void mouseClicked(int x,int y,int button) { super.mouseClicked(x,y,button); name.mouseClicked(x,y,button); accepts.mouseClicked(x,y,button); excludes.mouseClicked(x,y,button); }
    @Override public void onGuiClosed(){Keyboard.enableRepeatEvents(false);super.onGuiClosed();}
    @Override protected void drawGuiContainerBackgroundLayer(float partialTicks,int mouseX,int mouseY){GL11.glColor4f(1F,1F,1F,1F);mc.getTextureManager().bindTexture(TEXTURE);drawTexturedModalRect(guiLeft,guiTop,0,0,xSize,Math.min(222,ySize));}
    @Override protected void drawGuiContainerForegroundLayer(int mouseX,int mouseY){
        fontRendererObj.drawString("Name",8,6,4210752);fontRendererObj.drawString("Accept",8,28,4210752);fontRendererObj.drawString("Exclude",8,50,4210752);fontRendererObj.drawString("Inventory",8,108,4210752);
        name.drawTextBox();accepts.drawTextBox();excludes.drawTextBox();
    }
}
