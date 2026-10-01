package com.yukar1n.x_tracer.network;

import com.yukar1n.x_tracer.CommonProxy;

import com.yukar1n.x_tracer.Config;
import com.yukar1n.x_tracer.Util;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.*;

import static com.yukar1n.x_tracer.Util.isGregTechBlock;
import static net.minecraft.item.EnumAction.block;

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

    public static class Handler implements IMessageHandler<KeyPressMessage, IMessage> {
        @Override
        public IMessage onMessage(KeyPressMessage message, MessageContext ctx){

            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            UUID playerID = player.getUniqueID();
            System.out.println("x_tracer: Got client key input, playerID: " + playerID.toString());


            // 中心和半径
            int radius = Config.getPlayerConfig(playerID).radius;
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

                        //gtName
                        if (Config.getPlayerConfig(playerID).curMatchMode.equals("gtName")) {
                            Block b = world.getBlock(x, y, z);
                            String targetGtName = Config.getPlayerConfig(playerID).getTargetGtName();
                            if (isGregTechBlock(b)) {
                                try {
                                    int meta = world.getBlockMetadata(x, y, z);
                                    List<ItemStack> drops = b.getDrops(world, x, y, z, meta, 0);
                                    for (ItemStack drop : drops){
                                        String name = Objects.requireNonNull(drop.getItem()).getUnlocalizedName();
                                        int damage = drop.getItemDamage();
                                        String curGtName = name + "@" + damage;
                                        if (curGtName.equals(targetGtName)) {
                                            foundBlocks.add(new int[]{x, y, z});
                                            break;
                                        }

                                    }

                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            continue;
                        }

                        //block模式匹配逻辑
                        if (Config.getPlayerConfig(playerID).curMatchMode.equals("block")){

                            Block curBlock = world.getBlock(x, y, z);
                            String curBlockId = Block.blockRegistry.getNameForObject(curBlock);
                            int meta = world.getBlockMetadata(x, y, z);

                            //格式： ModName:Name@meta
                            String curFullID = curBlockId + "@" + meta;

                            //抓取目标方块id(Str)
                            List<String> targetBlockIDLst = (Config.getPlayerConfig(playerID).getTargetBlockIDs());

                            //对比目标列表
                            for (String targetBlockID : targetBlockIDLst) {
                                if (targetBlockID.equals(curFullID)) {
                                    foundBlocks.add(new int[]{x, y, z});
                                    break;
                                }
                            }
                            continue;
                        }

                        //ore模式匹配逻辑
                        if (Config.getPlayerConfig(playerID).curMatchMode.equals("ore")){
                            //抓取目标矿词(Str)
                            List<String> targetOreNameLst = Config.getPlayerConfig(playerID).getTargetOreNameList();

                            List<String> curOreNames = Util.getBlockOreNames(world, x, y, z);

                            for (String t : targetOreNameLst){
                                for (String c : curOreNames) {
                                    if (c != null && c.equals(t)) {
                                        foundBlocks.add(new int[]{x, y, z});
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            player.addChatMessage(new ChatComponentText("§b§k:" + "§bTrace Out: " + foundBlocks.size() + " §bObject§b§k:"));
            CommonProxy.network.sendTo(new FoundBlocksMessage(foundBlocks), player);

            if ("True".equals(Config.getPlayerConfig(playerID).getDrop)){
                // 存储掉落物
                Map<String, ItemStack> mergedDrops = new HashMap<>();
                int itemCount = 0;

                for (int[] pos : foundBlocks){
                    int x = pos[0];
                    int y = pos[1];
                    int z = pos[2];
                    int oldMeta = world.getBlockMetadata(x, y, z);
                    Block oldBlock = world.getBlock(x, y, z);

                    ArrayList<ItemStack> drops = oldBlock.getDrops(world, x, y, z, oldMeta, 0);

                    // 如果物品种类存在则添加值，不存在则添加新键
                    for (ItemStack drop : drops) {
                        if (drop != null) {
                            String key = Item.itemRegistry.getNameForObject(drop.getItem())
                                + ":" + drop.getItemDamage();
                            if (mergedDrops.containsKey(key)) {
                                mergedDrops.get(key).stackSize += drop.stackSize;
                            } else {
                                mergedDrops.put(key, drop.copy());
                            }
                            itemCount += drop.stackSize;
                        }
                    }


                    TileEntity oldTileEntity = world.getTileEntity(x, y, z);

                    // 移除原方块的 TileEntity（如果有）
                    if (oldTileEntity != null) {
                        world.removeTileEntity(x, y, z);
                    }

                    Block replacement = Blocks.air;
                    int meta =0;

                    // 设置新方块
                    boolean success = world.setBlock(x, y, z, replacement, meta, 2);

                    if (success) {
                        world.notifyBlockChange(x, y, z, replacement);
                        world.markBlockForUpdate(x, y, z);
                    }


                }

                // 最后一次性丢在玩家脚下
                for (ItemStack stack : mergedDrops.values()) {
                    EntityItem entityItem = new EntityItem(world,
                        player.posX, player.posY, player.posZ, stack);
                    world.spawnEntityInWorld(entityItem);
                }
                player.addChatMessage(new ChatComponentText("§6" + itemCount + " Drops generated"));
            }
            return null;
        }
    }
}

