package com.yukar1n.x_tracer.client;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.util.List;
@SideOnly(Side.CLIENT)

public class ClientRender {
    @SubscribeEvent
    public static void render(){

        List<int[]> blocks = ClientData.getHighlightBlocks();
        if (blocks.isEmpty()) return;
        double dx = RenderManager.instance.viewerPosX;
        double dy = RenderManager.instance.viewerPosY;
        double dz = RenderManager.instance.viewerPosZ;

        GL11.glPushMatrix();
        GL11.glTranslated(-dx, -dy, -dz);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColor3f(1f, 0f, 0f);
        GL11.glLineWidth(2f);

        Tessellator tess = Tessellator.instance;

        for (int[] pos : blocks) {
            double x = pos[0];
            double y = pos[1];
            double z = pos[2];

            GL11.glBegin(GL11.GL_LINES);
            // 简单画一个立方体边框
            drawCubeLines(tess, x, y, z);
            GL11.glEnd();
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glPopMatrix();
    }

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
