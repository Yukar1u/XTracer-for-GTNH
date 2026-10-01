package com.yukar1n.x_tracer.command;

import com.yukar1n.x_tracer.Config;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;

import java.util.Objects;
import java.util.UUID;

public class CommandXTracer extends CommandBase {
    public static UUID getSenderUUID(ICommandSender sender) {
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        return player.getUniqueID();
    }

    @Override
    public String getCommandName() {
        return "xtracer";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/xtracer cur";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        UUID playerID = getSenderUUID(sender);
        if (args.length != 1 || !"cur".equalsIgnoreCase(args[0])) {
            sender.addChatMessage(new ChatComponentText("§eUsage: /xtracer cur"));
            return;
        }
        if (Objects.equals(Config.getPlayerConfig(playerID).curMatchMode, "block")) {
            sender.addChatMessage(new ChatComponentText("§dCurrent Tracing: " + Config.getPlayerConfig(playerID).getTargetBlockIDs()));
        } else if (Objects.equals(Config.getPlayerConfig(playerID).curMatchMode, "ore")) {
            sender.addChatMessage(new ChatComponentText("§dCurrent Tracing: " + Config.getPlayerConfig(playerID).getTargetOreNameList()));
        } else if (Objects.equals(Config.getPlayerConfig(playerID).curMatchMode, "gtName")) {
            sender.addChatMessage(new ChatComponentText("§dCurrent Tracing: " + Config.getPlayerConfig(playerID).targetGtName));
        }
        sender.addChatMessage(new ChatComponentText("§dCurrent MatchMode: " + Config.getPlayerConfig(playerID).curMatchMode));
        sender.addChatMessage(new ChatComponentText("§dCurrent Radius: " + Config.getPlayerConfig(playerID).radius));
        sender.addChatMessage(new ChatComponentText("§dGet drops: " + Config.getPlayerConfig(playerID).getDrop));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
