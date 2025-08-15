package com.yukar1n.x_tracer.command;

import com.yukar1n.x_tracer.Config;
import com.yukar1n.x_tracer.Util;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CommandXTracer extends CommandBase {

    public static UUID getSenderUUID(ICommandSender sender){
        try {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            UUID playerID = player.getUniqueID();
            System.out.println(playerID.toString());
            return playerID;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getCommandName() {
        return "xtracer";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {

        UUID playerID = getSenderUUID(sender);
        if (playerID == null){return "";}

        sender.addChatMessage(new ChatComponentText("§b=========XTracer Usage========="));
        sender.addChatMessage(new ChatComponentText("§6§o/xtracer block <block_id>@<meta_data>"));
        sender.addChatMessage(new ChatComponentText("§6§o/xtracer ore <ore_id>      --Only trace ore! example: oreIron"));
        sender.addChatMessage(new ChatComponentText("§6§o/xtracer radius <radius>   --Domain: 1 <= radius <= " + Config.getPlayerConfig(playerID).maxRadius));
        sender.addChatMessage(new ChatComponentText("§6§o/xtracer ray                --Trace what you look at"));
        sender.addChatMessage(new ChatComponentText("§6§o/xtracer cur                --Current settings"));
        return "";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {

        UUID playerID = getSenderUUID(sender);
        if (playerID == null){return;}

        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            return;
        }

        //过滤参数大于2的指令
        //if (args.length > 2) {
        //    sender.addChatMessage(new ChatComponentText("§bNeed help? Enter </xtracer>!"));
        //   return;
        //}

        // 获取第一个参数（指令类型）
        String subCommand = args[0].toLowerCase();
        switch (subCommand) {

            case "block":
                //只有一个参数的指令
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("§cUsage: /xtracer block <block_id>@<meta_data>"));
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
                    Config.getPlayerConfig(playerID).curMatchMode = "block";
                    Config.getPlayerConfig(playerID).setTargetBlockID(target);
                    sender.addChatMessage(new ChatComponentText("§aXTracer: Tracing block " + args[1]));
                } else {
                    sender.addChatMessage(new ChatComponentText("§cInvalid block ID: " + args[1]));
                }
                break;

            case "ore":
                //只有一个参数的指令
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("§cUsage: /xtracer ore <OreDictName>"));
                }

                String targetOre = args[1];
                List<String> targetOreLst = new ArrayList<>();
                targetOreLst.add(targetOre.toLowerCase());
                //验证oreDict合法性
                if (!OreDictionary.doesOreNameExist(targetOre)) {
                    sender.addChatMessage(new ChatComponentText("§cInvalid OreDictName: " + args[1]));
                    return;
                }
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

                sender.addChatMessage(new ChatComponentText("§aXTracer: Tracing oreDict " + args[1]));
                Config.getPlayerConfig(playerID).setTargetOreName(targetOreLst);
                Config.getPlayerConfig(playerID).curMatchMode = "ore";
                break;

            case "radius":
                //只有一个参数的指令
                if (args.length < 2) {
                    sender.addChatMessage(new ChatComponentText("§cUsage: /xtracer radius <radius>"));
                    return;
                }
                try {
                    int r = Integer.parseInt(args[1]);
                    if (r < 1 || r > Config.getPlayerConfig(playerID).maxRadius){
                        sender.addChatMessage(new ChatComponentText("§cRadius out of Range! Input: " + r + " Range: (1 <= radius <= " + Config.getPlayerConfig(playerID).maxRadius + ")"));
                        return;
                    }
                    Config.getPlayerConfig(playerID).radius = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.addChatMessage(new ChatComponentText("§cRadius must be an Integer!"));
                    return;
                }
                sender.addChatMessage(new ChatComponentText("§6Radius set to: §b" + args[1]));
                break;

            case "ray":
                if (args.length == 2){
                    sender.addChatMessage(new ChatComponentText("§cXTracer: Unknown argument: " +args[1]));
                    return;
                }
                try {
                    EntityPlayerMP player = getCommandSenderAsPlayer(sender);
                    int bol = Util.setRayTrace(player);
                    if (bol == 0){
                        sender.addChatMessage(new ChatComponentText("§cXTracer: Failed to get block data"));
                    }
                }
                catch (Exception e){
                    return;
                }
                break;

            case "cur":
                if (args.length == 2) {
                    sender.addChatMessage(new ChatComponentText("§cXTracer: Unknown argument: " + args[1]));
                    return;
                }

                if (Objects.equals(Config.getPlayerConfig(playerID).curMatchMode, "block")) {
                    sender.addChatMessage(new ChatComponentText("§dCurrent Tracing: " + Config.getPlayerConfig(playerID).targetBlockID));
                } else if (Objects.equals(Config.getPlayerConfig(playerID).curMatchMode, "ore")) {
                    sender.addChatMessage(new ChatComponentText("§dCurrent Tracing: " + Config.getPlayerConfig(playerID).targetOreNameList));
                } else if (Objects.equals(Config.getPlayerConfig(playerID).curMatchMode, "gtName")) {
                    sender.addChatMessage(new ChatComponentText("§dCurrent Tracing: " + Config.getPlayerConfig(playerID).targetGtName));
                }
                sender.addChatMessage(new ChatComponentText("§dCurrent MatchMode: " + Config.getPlayerConfig(playerID).curMatchMode));
                sender.addChatMessage(new ChatComponentText("§dCurrent Radius: " + Config.getPlayerConfig(playerID).radius));
                break;



            default:
                sender.addChatMessage(new ChatComponentText("§5XTracer: Unknown subcommand, Try /xtracer for help"));
                break;
        }
    }


    @Override
    public int getRequiredPermissionLevel() {
        return 0; // 普通玩家可用
    }
}
