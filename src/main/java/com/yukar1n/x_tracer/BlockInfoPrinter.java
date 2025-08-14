package com.yukar1n.x_tracer;

import net.minecraft.block.Block;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

public class BlockInfoPrinter {

    public static void printBlockInfo(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);
        TileEntity tileEntity = world.getTileEntity(x, y, z);

        System.out.println("==================================================");
        System.out.println("=== 方块详细信息 @ (" + x + ", " + y + ", " + z + ")");
        System.out.println("==================================================");

        // 1. 基础信息
        printBasicInfo(block, meta);

        // 2. 物理属性
        printPhysicalProperties(world, block, x, y, z);

        // 3. 位置与环境信息
        printEnvironmentInfo(world, x, y, z);

        // 4. 视觉与渲染信息
        printVisualInfo(block, meta);

        // 5. 矿物词典信息
        printOreDictInfo(block, meta);

        // 6. 掉落物信息
        printDropsInfo(world, block, x, y, z, meta);

        // 7. TileEntity信息
        if (tileEntity != null) {
            printTileEntityInfo(tileEntity);
        } else {
            System.out.println("--- TileEntity: null");
        }

        // 8. GregTech特殊信息
        if (isGregTechBlock(block)) {
            printGregTechInfo(world, block, x, y, z, meta);
        }

        System.out.println("==================================================");
    }

    private static void printBasicInfo(Block block, int meta) {
        System.out.println("--- 基础信息 ---");
        System.out.println("注册名: " + Block.blockRegistry.getNameForObject(block));
        System.out.println("本地化名称: " + block.getLocalizedName());
        System.out.println("未本地化名称: " + block.getUnlocalizedName());
        System.out.println("元数据: " + meta);
        System.out.println("类名: " + block.getClass().getName());
        System.out.println("是否为空气: " + block.isAir(null, 0, 0, 0));
        System.out.println("是否为完整立方体: " + block.isOpaqueCube());
    }

    private static void printPhysicalProperties(World world, Block block, int x, int y, int z) {
        System.out.println("--- 物理属性 ---");
        System.out.println("硬度: " + block.getBlockHardness(world, x, y, z));
        System.out.println("抗爆性: " + block.getExplosionResistance(null, world, x, y, z, 0, 0, 0));
        System.out.println("亮度值: " + block.getLightValue());
        System.out.println("发光等级: " + block.getLightOpacity());
        System.out.println("材质: " + block.getMaterial());

        // 红石相关
        System.out.println("红石信号强度:");
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            int power = world.getIndirectPowerLevelTo(x, y, z, dir.ordinal());
            System.out.println("  " + dir.name() + ": " + power);
        }
        System.out.println("是否被强充能: " + world.isBlockProvidingPowerTo(x, y, z, ForgeDirection.UP.ordinal()));
        System.out.println("是否接收红石信号: " + world.isBlockIndirectlyGettingPowered(x, y, z));
    }

    private static void printEnvironmentInfo(World world, int x, int y, int z) {
        System.out.println("--- 环境信息 ---");
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        System.out.println("生物群系: " + biome.biomeName);
        System.out.println("温度: " + biome.temperature);
        System.out.println("降雨量: " + biome.rainfall);
        System.out.println("方块光照: " + world.getBlockLightValue(x, y, z));
        System.out.println("天空光照: " + world.getFullBlockLightValue(x, y, z));
        System.out.println("是否暴露于天空: " + world.canBlockSeeTheSky(x, y, z));
        System.out.println("维度ID: " + world.provider.dimensionId);
        System.out.println("维度名称: " + world.provider.getDimensionName());
    }

    private static void printVisualInfo(Block block, int meta) {
        System.out.println("--- 视觉信息 ---");
        System.out.println("渲染类型: " + block.getRenderType());
        System.out.println("纹理: ");
        try {
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                System.out.println("  " + dir.name() + ": " + block.getIcon(dir.ordinal(), meta).getIconName());
            }
        } catch (Exception e) {
            System.out.println("  无法获取纹理信息: " + e.getMessage());
        }
        System.out.println("颜色值: " + block.getRenderColor(meta));
    }

    private static void printOreDictInfo(Block block, int meta) {
        System.out.println("--- 矿物词典信息 ---");
        ItemStack stack = new ItemStack(block, 1, meta);
        int[] oreIDs = OreDictionary.getOreIDs(stack);

        if (oreIDs.length > 0) {
            System.out.println("矿物词典名称:");
            for (int id : oreIDs) {
                String name = OreDictionary.getOreName(id);
                System.out.println("  - " + name);
            }
        } else {
            System.out.println("无矿物词典名称");
        }
    }

    private static void printDropsInfo(World world, Block block, int x, int y, int z, int meta) {
        System.out.println("--- 掉落物信息 ---");
        System.out.println("基础掉落数量: " + block.quantityDropped(meta, 0, world.rand));

        try {
            List<ItemStack> drops = block.getDrops(world, x, y, z, meta, 0);
            System.out.println("掉落物数量: " + drops.size());

            for (int i = 0; i < drops.size(); i++) {
                ItemStack drop = drops.get(i);
                System.out.println(" 掉落物 #" + (i+1) + ":");
                System.out.println("   物品: " + drop.getDisplayName());
                System.out.println("   注册名: " + Item.itemRegistry.getNameForObject(drop.getItem()));
                System.out.println("   数量: " + drop.stackSize);
                System.out.println("   元数据: " + drop.getItemDamage());

                // 掉落物的矿物词典
                int[] dropOreIDs = OreDictionary.getOreIDs(drop);
                if (dropOreIDs.length > 0) {
                    System.out.println("   矿物词典:");
                    for (int id : dropOreIDs) {
                        System.out.println("     - " + OreDictionary.getOreName(id));
                    }
                }

                // NBT数据
                if (drop.hasTagCompound()) {
                    System.out.println("   NBT数据: " + drop.getTagCompound().toString());
                }
            }
        } catch (Exception e) {
            System.out.println("  获取掉落物失败: " + e.getMessage());
        }
    }

    private static void printTileEntityInfo(TileEntity te) {
        System.out.println("--- TileEntity 信息 ---");
        System.out.println("类名: " + te.getClass().getName());

        // NBT数据
        NBTTagCompound nbt = new NBTTagCompound();
        te.writeToNBT(nbt);
        System.out.println("NBT数据: " + nbt.toString());

        // 如果是容器
        if (te instanceof net.minecraft.inventory.IInventory) {
            net.minecraft.inventory.IInventory inv = (net.minecraft.inventory.IInventory)te;
            System.out.println("容器大小: " + inv.getSizeInventory());
            System.out.println("内容物:");
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack != null) {
                    System.out.println("  槽位 #" + i + ": " + stack.getDisplayName() + " x" + stack.stackSize);
                }
            }
        }
    }

    private static boolean isGregTechBlock(Block block) {
        String className = block.getClass().getName().toLowerCase();
        return className.contains("gregtech") || className.contains("gt");
    }

    private static void printGregTechInfo(World world, Block block, int x, int y, int z, int meta) {
        System.out.println("--- GregTech 特殊信息 ---");

        try {
            // 获取方块名称
            String regName = Block.blockRegistry.getNameForObject(block);
            System.out.println("GT注册名: " + regName);

            // 尝试获取矿石等级
            int oreGrade = (meta & 0xF0) >> 4;
            String gradeName = "";
            switch(oreGrade) {
                case 0: gradeName = "Poor"; break;
                case 1: gradeName = "Normal"; break;
                case 2: gradeName = "Rich"; break;
                case 3: gradeName = "Impoverished"; break;
                default: gradeName = "Unknown";
            }
            System.out.println("矿石等级: " + gradeName + " (值: " + oreGrade + ")");

            // 尝试获取矿石类型
            int oreType = meta & 0x0F;
            System.out.println("矿石类型ID: " + oreType);

            // 获取玩家面向的名称
            try {
                MovingObjectPosition mop = new MovingObjectPosition(x, y, z, 0, Vec3.createVectorHelper(x, y, z));
                ItemStack pickStack = block.getPickBlock(mop, world, x, y, z, null);
                if (pickStack != null) {
                    System.out.println("玩家面向名称: " + pickStack.getDisplayName());
                }
            } catch (Exception e) {
                System.out.println("无法获取玩家面向名称: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("获取GregTech信息失败: " + e.getMessage());
        }
    }

    // 打印物品堆栈详细信息（用于掉落物）
    private static void printItemStackDetails(ItemStack stack) {
        if (stack == null) return;

        System.out.println("   物品: " + stack.getDisplayName());
        System.out.println("   注册名: " + Item.itemRegistry.getNameForObject(stack.getItem()));
        System.out.println("   数量: " + stack.stackSize);
        System.out.println("   元数据: " + stack.getItemDamage());
        System.out.println("   最大堆叠: " + stack.getMaxStackSize());
        System.out.println("   耐久: " + (stack.getMaxDamage() - stack.getItemDamage()) + "/" + stack.getMaxDamage());

        // 附魔信息
        if (stack.isItemEnchanted()) {
            NBTTagList enchantments = stack.getEnchantmentTagList();
            System.out.println("   附魔:");
            for (int i = 0; i < enchantments.tagCount(); i++) {
                NBTTagCompound enchant = enchantments.getCompoundTagAt(i);
                int id = enchant.getShort("id");
                int lvl = enchant.getShort("lvl");
                Enchantment ench = Enchantment.enchantmentsList[id];
                if (ench != null) {
                    System.out.println("     - " + ench.getTranslatedName(lvl));
                }
            }
        }

        // NBT数据
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            System.out.println("   NBT数据: " + nbt.toString());
        }
    }
}
