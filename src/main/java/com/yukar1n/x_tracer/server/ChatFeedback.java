package com.yukar1n.x_tracer.server;

import com.yukar1n.x_tracer.storage.ContainerReference;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

public final class ChatFeedback {
    private ChatFeedback() {}

    public static void send(EntityPlayer player, String key, Object... arguments) {
        player.addChatMessage(new ChatComponentTranslation(key, arguments));
    }

    public static IChatComponent node(ContainerReference reference, String customName) {
        IChatComponent name = customName == null || customName.isEmpty()
            ? new ChatComponentTranslation("container.x_tracer") : new ChatComponentText(customName);
        return new ChatComponentTranslation("xtracer.node.location", name,
            reference.dimension, reference.x, reference.y, reference.z);
    }
}
