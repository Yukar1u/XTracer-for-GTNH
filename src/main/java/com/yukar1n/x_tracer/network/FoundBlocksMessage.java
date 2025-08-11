package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.client.ClientData;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class FoundBlocksMessage implements IMessage {
    private List<int[]> positions;

    public FoundBlocksMessage() {
        // 必须要有无参构造，反序列化时用
    }
    public FoundBlocksMessage(List<int[]> positions) {
        this.positions = positions;
    }
    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        positions = new ArrayList<int[]>(size);
        for (int i = 0; i < size; i++) {
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();
            positions.add(new int[] { x, y, z });
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        if (positions == null) {
            buf.writeInt(0);
            return;
        }
        buf.writeInt(positions.size());
        for (int[] p : positions) {
            buf.writeInt(p[0]);
            buf.writeInt(p[1]);
            buf.writeInt(p[2]);
        }
    }

    public List<int[]> getBlockPositions() {
        return positions;
    }

    public static class Handler implements IMessageHandler<FoundBlocksMessage, IMessage> {
        @Override
        public IMessage onMessage(final FoundBlocksMessage message, MessageContext ctx) {
            // 必须在主线程执行 Minecraft 相关操作
            Minecraft.getMinecraft().func_152344_a(new Runnable() {
                @Override
                public void run() {
                    // 这里存到一个客户端全局变量中，供渲染用
                    List<int[]> passingPosition = message.getBlockPositions();
                    System.out.println("x_tracer: Client got " + passingPosition.size() + " Objects");
                    ClientData.setHighlightBlocks(passingPosition);
                }
            });
            return null; // 不需要返回数据包
        }
    }
}
