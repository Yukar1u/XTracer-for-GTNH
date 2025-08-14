package com.yukar1n.x_tracer;

import com.yukar1n.x_tracer.network.FoundBlocksMessage;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import scala.tools.nsc.transform.patmat.Logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Util {

    // 不抓取方块矿词，而是抓取掉落物显示名，再提取矿词，如oreIron, oreCopper 然后统一小写
    public static List<String> getBlockOreNames(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        TileEntity curEntity = world.getTileEntity(x, y, z);

        //debug
        //String regName = Block.blockRegistry.getNameForObject(block);
        //System.out.println("===注册名： " + regName + " Meta: " + meta + " TileEntity: " + curEntity);

        //如果带有ore词条，开始模拟掉落物
        if (isOre(block)){

            List<ItemStack> drops = block.getDrops(world, x, y, z, meta, 0);
            List<String> oreNames = new ArrayList<>();//存放匹配到的矿辞

            //遍历方块的掉落物
            for (ItemStack drop : drops) {

                //debug
                //System.out.println(drop.getDisplayName());
                //System.out.println(drop.getItem() + " " + drop.getItemDamage());

                //提取矿辞数字Lst
                int[] oreIDs = OreDictionary.getOreIDs(drop);

                //遍历每个矿辞数字
                for (int oreID : oreIDs){
                    String oreName = OreDictionary.getOreName(oreID);
                    //处理oreName
                    oreName = removeRawPrefix(oreName);
                    oreName = oreName.toLowerCase();
                    //
                    oreNames.add(oreName);
                }
            }
            //System.out.println(oreNames);
            return oreNames;
        }

        return new ArrayList<>();
    }

    // Helper Method
    public static String removeRawPrefix(String str) {
        if (str.startsWith("raw")) {
            return str.substring(3);
        }
        return str;
    }

    // 获取玩家准星上的方块坐标
    public static int setRayTrace (EntityPlayerMP player) {
        Vec3 eyePos = player.getPosition(1.0F).addVector(0, player.getEyeHeight(), 0);
        Vec3 unitVec = player.getLookVec();
        double xIncrement = unitVec.xCoord * 0.1;
        double yIncrement = unitVec.yCoord * 0.1;
        double zIncrement = unitVec.zCoord * 0.1;

        Vec3 cur = eyePos;
        int x = 0;
        int y = 0;
        int z = 0;
        for (int i = 0; i <= 100; i++) {
            cur = cur.addVector(xIncrement, yIncrement, zIncrement);
            x = (int) Math.floor(cur.xCoord);
            y = (int) Math.floor(cur.yCoord);
            z = (int) Math.floor(cur.zCoord);
            World world = player.worldObj;
            Block curBlock = world.getBlock(x, y, z);
            String curBLockName = Block.blockRegistry.getNameForObject(curBlock);

            if (curBlock.isAir(null, 0, 0, 0)){
                continue;
            }
            // 接触第一个非空气方块
            System.out.println(curBLockName);

            //后台打印方块信息，调试用
            BlockInfoPrinter.printBlockInfo(world, x,y,z);

            // 格雷处理逻辑
            if (isGregTechBlock(curBlock)){
                try {
                    MovingObjectPosition mop = new MovingObjectPosition(x, y, z, 0, Vec3.createVectorHelper(x, y, z));
                    ItemStack pickStack = curBlock.getPickBlock(mop, world, x, y, z, null);
                    if (pickStack != null) {
                        String gtName =  pickStack.getDisplayName();
                        Config.getPlayerConfig(player.getUniqueID()).targetGtName = gtName;
                        Config.getPlayerConfig(player.getUniqueID()).curMatchMode = "gtName";
                        player.addChatMessage(new ChatComponentText("§aXTracer: Tracing " + gtName));

                        //显示选中方块轮廓
                        List<int[]> loc = new ArrayList<>();
                        loc.add(new int[]{x, y, z});
                        CommonProxy.network.sendTo(new FoundBlocksMessage(loc), player);
                        return 1;
                    }
                } catch (Exception e) {
                    System.out.println("无法获取玩家面向名称: " + e.getMessage());
                }
            }

            // 其他处理逻辑
            String curBlockId = Block.blockRegistry.getNameForObject(curBlock);
            int meta = world.getBlockMetadata(x ,y ,z);

            //格式： ModName:Name@meta
            String curFullID = curBlockId + "@" + meta;
            List<String> target = new ArrayList<>();
            target.add(curFullID);
            Config.getPlayerConfig(player.getUniqueID()).curMatchMode = "block";
            Config.getPlayerConfig(player.getUniqueID()).setTargetBlockID(target);
            player.addChatMessage(new ChatComponentText("§aXTracer: Tracing " + curFullID));

            //显示选中方块轮廓
            List<int[]> loc = new ArrayList<>();
            loc.add(new int[]{x, y, z});
            CommonProxy.network.sendTo(new FoundBlocksMessage(loc), player);
            return 1;
        }

        // 匹配失败
        return 0;
    }

    // HelperMethod
    public static boolean isGregTechBlock(Block block) {
        String className = block.getClass().getName().toLowerCase();
        return className.contains("gregtech") || className.contains("gt");
    }
    // HelperMethod
    private static boolean isOre(Block block) {
        String className = block.getClass().getName().toLowerCase();
        return className.contains("ore");
    }
}
