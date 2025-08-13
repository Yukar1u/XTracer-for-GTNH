package com.yukar1n.x_tracer;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

public class Util {

    // 不抓取方块矿词，而是抓取掉落物显示名，再提取矿词，如oreIron, oreCopper 然后统一小写
    public static List<String> getBlockOreNames(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        TileEntity curEntity = world.getTileEntity(x, y, z);

        //debug
        //String regName = Block.blockRegistry.getNameForObject(block);
        //System.out.println("===注册名： " + regName + " Meta: " + meta + " TileEntity: " + curEntity);

        if (curEntity == null){
            return new ArrayList<>();
        }
        String[] curEntitySpl = curEntity.toString().split("@");
        //如果Entity输入gt矿物类，开始模拟掉落物
        if (curEntitySpl[0].equals("gregtech.common.blocks.TileEntityOres")){

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
            System.out.println(oreNames);
            return oreNames;
        }

        return new ArrayList<>();
    }

    public static String removeRawPrefix(String str) {
        if (str.startsWith("raw")) {
            return str.substring(3);
        }
        return str;
    }
}
