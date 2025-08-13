package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;

import com.yukar1n.x_tracer.Config;
import com.yukar1n.x_tracer.Util;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

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

            // 中心和半径
            int radius = Config.radius;
            int playerPosX = MathHelper.floor_double(player.posX);
            int playerPosY = MathHelper.floor_double(player.posY);
            int playerPosZ = MathHelper.floor_double(player.posZ);
            World world = player.worldObj;

            List<int[]> foundBlocks = new ArrayList<>();
            int radiusSquared = radius * radius; // 预计算半径平方

            // 遍历范围（限制为球形区域）
            int minX = playerPosX - radius;
            int maxX = playerPosX + radius;
            int minY = Math.max(0, playerPosY - radius);
            int maxY = Math.min(255, playerPosY + radius);
            int minZ = playerPosZ - radius;
            int maxZ = playerPosZ + radius;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        // 检查是否在球形范围内
                        int dx = x - playerPosX;
                        int dy = y - playerPosY;
                        int dz = z - playerPosZ;
                        if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                            continue; // 跳过超出半径的方块
                        }

                        //block模式匹配逻辑
                        if (Config.curMatchMode.equals("block")){

                            Block curBlock = world.getBlock(x, y, z);
                            String curBlockId = Block.blockRegistry.getNameForObject(curBlock);
                            int meta = world.getBlockMetadata(x, y, z);
                            //抓取目标方块id(Str)
                            List<String> targetBlockIDLst = (Config.getTargetBlockIDs());

                            //格式： ModName:Name@meta
                            String curFullID = curBlockId + "@" + meta;

                            //对比目标列表
                            for (String targetBlockID : targetBlockIDLst) {
                                if (targetBlockID.equals(curFullID)) {
                                    foundBlocks.add(new int[]{x, y, z});
                                }
                            }
                        }

                        //ore模式匹配逻辑
                        if (Config.curMatchMode.equals("ore")){
                            //抓取目标矿词(Str)
                            List<String> targetOreNameLst = Config.getTargetOreNameList();

                            List<String> curOreNames = Util.getBlockOreNames(world, x, y, z);

                            for (String t : targetOreNameLst){
                                for (String c : curOreNames) {
                                    if (c != null && c.equals(t)) {
                                        foundBlocks.add(new int[]{x, y, z});
                                    }
                                }
                            }
                        }
                    }
                }
            }
            player.addChatMessage(new ChatComponentText("§b§k:" + "§bTrace Out: " + foundBlocks.size() + " §bObject§b§k:"));
            CommonProxy.network.sendTo(new FoundBlocksMessage(foundBlocks), player);
            return null;
        }
    }
}

