package com.yukar1n.x_tracer.client.gui;

import com.yukar1n.x_tracer.CommonProxy;
import com.yukar1n.x_tracer.Config;
import com.yukar1n.x_tracer.network.TerminalRequestMessage;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

import java.util.UUID;

public class GuiXTracerTerminal extends GuiScreen {
    private GuiTextField target;
    private GuiTextField radius;
    private int mode;
    private boolean drop;

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        int left = width / 2 - 110;
        UUID id = Minecraft.getMinecraft().thePlayer.getUniqueID();
        com.yukar1n.x_tracer.ClientConfig config = Config.getPlayerConfig(id);
        mode = "ore".equals(config.curMatchMode) ? 1 : 0;
        drop = "True".equals(config.getDrop);
        String current = mode == 0 && !config.targetList.isEmpty() ? config.targetList.get(0) : "";
        if (mode == 1 && !config.targetOreNameList.isEmpty()) current = config.targetOreNameList.get(0);
        target = new GuiTextField(fontRendererObj, left, 45, 220, 20);
        target.setText(current);
        radius = new GuiTextField(fontRendererObj, left, 85, 70, 20);
        radius.setText(String.valueOf(config.radius));
        buttonList.add(new GuiButton(0, left, 115, 70, 20, "Block"));
        buttonList.add(new GuiButton(1, left + 75, 115, 70, 20, "Ore"));
        buttonList.add(new GuiButton(2, left, 140, 105, 20, "Save target"));
        buttonList.add(new GuiButton(3, left + 110, 140, 110, 20, "Save radius"));
        buttonList.add(new GuiButton(4, left, 165, 105, 20, "Drops: " + drop));
        buttonList.add(new GuiButton(5, left + 110, 165, 110, 20, "Ray select"));
        buttonList.add(new GuiButton(6, left, 190, 220, 20, "Scan"));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: mode = 0; break;
            case 1: mode = 1; break;
            case 2: CommonProxy.network.sendToServer(new TerminalRequestMessage(mode == 0 ? TerminalRequestMessage.SET_BLOCK : TerminalRequestMessage.SET_ORE, target.getText(), 0, drop)); break;
            case 3:
                try { CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_RADIUS, "", Integer.parseInt(radius.getText()), drop)); }
                catch (NumberFormatException ignored) {}
                break;
            case 4:
                drop = !drop;
                button.displayString = "Drops: " + drop;
                CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SET_DROP, "", 0, drop));
                break;
            case 5: CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.RAY, "", 0, drop)); break;
            case 6: CommonProxy.network.sendToServer(new TerminalRequestMessage(TerminalRequestMessage.SCAN, "", 0, drop)); break;
            default: break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        target.textboxKeyTyped(typedChar, keyCode);
        radius.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_ESCAPE) super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        target.mouseClicked(mouseX, mouseY, mouseButton);
        radius.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int left = width / 2 - 110;
        drawCenteredString(fontRendererObj, "XTracer Terminal", width / 2, 20, 0xFFFFFF);
        drawString(fontRendererObj, "Target", left, 34, 0xFFFFFF);
        drawString(fontRendererObj, "Radius", left, 74, 0xFFFFFF);
        target.drawTextBox();
        radius.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
