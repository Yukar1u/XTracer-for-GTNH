package com.yukar1n.x_tracer.command;

import com.yukar1n.x_tracer.Config;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

public class CommandXTracer extends CommandBase {

    @Override
    public String getCommandName() {
        return "xtracer";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        sender.addChatMessage(new ChatComponentText("§6§o/xtracer block <block_id>@<meta_data>"));
        sender.addChatMessage(new ChatComponentText("§6§o/xtracer ore <ore_id>   (Only trace gt mineral! example: oreIron)"));
        sender.addChatMessage(new ChatComponentText("§6§o/xtracer radius <radius>   (1 <= radius <= " + Config.maxRadius + ")"));
        return "";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText("§b     ===XTracer Usage==="));
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            return;
        }
        if (args.length > 2) {
            sender.addChatMessage(new ChatComponentText("§bNeed help? Enter </xtracer>!"));
            return;
        }
        // 获取第一个参数（指令类型）
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {

            case "block":
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("§cUsage: /xtracer block <block_id>@<meta_data>"));
                    return;
                }
                String[] split = args[1].split("@");//[<block_id>, <meta>]
                String[] split_ = split[0].split(":");//[mod, block]
                if (split.length != 2){
                    sender.addChatMessage(new ChatComponentText("§cInvalid meta Data format! Use <block_id>@<meta_data>"));
                    return;
                }
                if (split_.length != 2) {
                    sender.addChatMessage(new ChatComponentText("§cInvalid block ID format! Use <mod_name:block_name>"));
                    return;
                }

                Block block = GameRegistry.findBlock(split_[0], split_[1]);
                if (block != null) {
                    List<String> target = new ArrayList<>();
                    target.add(args[1]);
                    Config.curMatchMode = "block";
                    Config.setTargetBlockID(target);
                    sender.addChatMessage(new ChatComponentText("§aXTracer: Tracking block " + args[1]));
                } else {
                    sender.addChatMessage(new ChatComponentText("§cInvalid block ID: " + args[1]));
                }
                break;

            case "ore":
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("§cUsage: /xtracer ore <OreDictName>"));
                    return;
                }
                String targetOre = args[1];
                List<String> targetOreLst = new ArrayList<>();
                targetOreLst.add(targetOre.toLowerCase());
                //验证oreDict合法性
                if (!OreDictionary.doesOreNameExist(targetOre)) {
                    sender.addChatMessage(new ChatComponentText("§cInvalid OreDictName: " + args[1]));
                    return;
                }
                /*
                //提取OreName
                List<ItemStack> targetOreStkLst = OreDictionary.getOres(targetOre);
                if (targetOreStkLst.isEmpty()) {
                    sender.addChatMessage(new ChatComponentText("§cNo such ore Found with input: " + args[1]));
                    return;
                }
                for (ItemStack i : targetOreStkLst) {
                    Block b = Block.getBlockFromItem(i.getItem());
                    if (b != null) {
                        String oreBlockId = Block.blockRegistry.getNameForObject(b);
                        sender.addChatMessage(new ChatComponentText("§6Add: " + oreBlockId));
                    }
                }
                */
                sender.addChatMessage(new ChatComponentText("§aXTracer: Tracking GT oreDict " + args[1]));
                Config.setTargetOreName(targetOreLst);
                Config.curMatchMode = "ore";
                break;

            case "radius":
                try {
                    int r = Integer.parseInt(args[1]);
                    if (r < 1 || r > Config.maxRadius){
                        sender.addChatMessage(new ChatComponentText("§cRadius out of Range! Input: " + r + " Range: (1 <= radius <= " + Config.maxRadius + ")"));
                        return;
                    }
                    Config.radius = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.addChatMessage(new ChatComponentText("§cRadius must be an Integer!"));
                    return;
                }
                sender.addChatMessage(new ChatComponentText("§6Radius set to: §b" + args[1]));
                break;


            default:
                sender.addChatMessage(new ChatComponentText("§cUnknown subcommand"));
                break;


        }
    }


    @Override
    public int getRequiredPermissionLevel() {
        return 0; // 普通玩家可用
    }
}
