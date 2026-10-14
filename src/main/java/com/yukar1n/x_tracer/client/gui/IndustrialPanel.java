package com.yukar1n.x_tracer.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import org.lwjgl.opengl.GL11;

/** Shared metal panel primitives for the terminal and storage screens. */
public final class IndustrialPanel extends Gui {
    public static final int TEXT = 0xDCE8EB;
    public static final int MUTED = 0x8CABB2;
    public static final int ACCENT = 0x55C5D1;

    private IndustrialPanel() {}

    public static void bevel(int x, int y, int width, int height, int fill, boolean inset) {
        drawRect(x, y, x + width, y + height, fill);
        int light = inset ? 0xFF11191D : 0xFF52636B;
        int dark = inset ? 0xFF52636B : 0xFF11191D;
        drawRect(x, y, x + width, y + 1, light);
        drawRect(x, y, x + 1, y + height, light);
        drawRect(x, y + height - 1, x + width, y + height, dark);
        drawRect(x + width - 1, y, x + width, y + height, dark);
    }

    public static void fitText(FontRenderer font, String text, int x, int y, int width, int color, float scale) {
        if (text == null || width <= 0) return;
        int available = (int) (width / scale);
        if (font.getStringWidth(text) > available) {
            text = font.trimStringToWidth(text, Math.max(0, available - font.getStringWidth("..."))) + "...";
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(scale, scale, 1);
        font.drawString(text, 0, 0, color);
        GL11.glPopMatrix();
        GL11.glColor4f(1, 1, 1, 1);
    }

    public static class PanelButton extends GuiButton {
        public boolean selected;

        public PanelButton(int id, int x, int y, int width, int height, String label) {
            super(id, x, y, width, height, label);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) return;
            field_146123_n = mouseX >= xPosition && mouseY >= yPosition
                && mouseX < xPosition + width && mouseY < yPosition + height;
            bevel(xPosition, yPosition, width, height,
                !enabled ? 0xFF20292E : field_146123_n ? 0xFF3A535D : selected ? 0xFF2C4852 : 0xFF2D3A41, false);
            if (selected) drawRect(xPosition + 2, yPosition + height - 2, xPosition + width - 2, yPosition + height - 1, 0xFF55C5D1);
            float scale = 0.8F;
            int textWidth = Math.min(mc.fontRenderer.getStringWidth(displayString), (int) ((width - 6) / scale));
            fitText(mc.fontRenderer, displayString, xPosition + Math.max(3, (width - (int) (textWidth * scale)) / 2),
                yPosition + (height - 7) / 2, width - 6, enabled ? TEXT : MUTED, scale);
        }
    }
}
