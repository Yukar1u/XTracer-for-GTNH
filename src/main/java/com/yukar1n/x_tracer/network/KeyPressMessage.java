package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.GameRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class KeyPressMessage implements IMessage{

    //没有额外数据，构造函数为空
    public void keyPressMessage(){}

    @Override
    public void fromBytes(ByteBuf buf){
        //无数据，不实现
    }

    @Override
    public void toBytes(ByteBuf buf){
        //无数据，不实现
    }

    public static class Handler implements IMessageHandler<KeyPressMessage, IMessage>{
        @Override
        public IMessage onMessage(KeyPressMessage message, MessageContext ctx){
            System.out.println("x_trace: Got client key input");
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            //中心和半径
            int radius = 10;
            int playerPosX = MathHelper.floor_double(player.posX);
            int playerPosY = MathHelper.floor_double(player.posY);
            int playerPosZ = MathHelper.floor_double(player.posZ);

            World world = player.worldObj;

            List<int[]> foundBlocks = new ArrayList<>();

            //遍历范围
            int minX = playerPosX - radius;
            int maxX = playerPosX + radius;
            int minY = Math.max(0, playerPosY - radius);
            int maxY = Math.min(255, playerPosY + radius);
            int minZ = playerPosZ - radius;
            int maxZ = playerPosZ + radius;

            //遍历
            for (int x = minX; x <= maxX; x++){
                for (int y = minY; y<= maxY; y++){
                    for (int z = minZ; z <= maxZ ; z++){
                        Block curBlock = world.getBlock(x, y, z);
                        String curBlockId = Block.blockRegistry.getNameForObject(curBlock);
                        if ("minecraft:stone".equals(curBlockId)){
                            foundBlocks.add(new int[] { x, y, z });
                        }
                    }
                }
            }
            CommonProxy.network.sendTo(new FoundBlocksMessage(foundBlocks), player);
            return null;
        }
    }
}

