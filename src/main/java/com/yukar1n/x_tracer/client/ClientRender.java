package com.yukar1n.x_tracer.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;
@SideOnly(Side.CLIENT)
public class ClientRender {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!ClientData.shouldRender()) return;
        System.out.println("x_tracer: Rendering");

        Minecraft mc = Minecraft.getMinecraft();
        double dx = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * event.partialTicks;
        double dy = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * event.partialTicks;
        double dz = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * event.partialTicks;

        GL11.glPushMatrix();
        GL11.glTranslated(-dx, -dy, -dz);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor4f(1F, 0F, 0F, 1F); // 红色
        GL11.glLineWidth(2F);

        for (int[] pos : ClientData.getHighlightBlocks()) {
            AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                pos[0], pos[1], pos[2],
                pos[0] + 1, pos[1] + 1, pos[2] + 1
            );
            RenderGlobal.drawOutlinedBoundingBox(box, 0xFFFFFFFF);
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }
}
/*
    private static void drawCubeLines(Tessellator tess, double x, double y, double z) {
        double max = 1.0;
        // 底面
        tess.addVertex(x, y, z);
        tess.addVertex(x + max, y, z);

        tess.addVertex(x + max, y, z);
        tess.addVertex(x + max, y, z + max);

        tess.addVertex(x + max, y, z + max);
        tess.addVertex(x, y, z + max);

        tess.addVertex(x, y, z + max);
        tess.addVertex(x, y, z);

        // 顶面
        tess.addVertex(x, y + max, z);
        tess.addVertex(x + max, y + max, z);

        tess.addVertex(x + max, y + max, z);
        tess.addVertex(x + max, y + max, z + max);

        tess.addVertex(x + max, y + max, z + max);
        tess.addVertex(x, y + max, z + max);

        tess.addVertex(x, y + max, z + max);
        tess.addVertex(x, y + max, z);

        // 竖线
        tess.addVertex(x, y, z);
        tess.addVertex(x, y + max, z);

        tess.addVertex(x + max, y, z);
        tess.addVertex(x + max, y + max, z);

        tess.addVertex(x + max, y, z + max);
        tess.addVertex(x + max, y + max, z + max);

        tess.addVertex(x, y, z + max);
        tess.addVertex(x, y + max, z + max);
    }
}
**/
