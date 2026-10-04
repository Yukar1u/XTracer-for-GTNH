package com.yukar1n.x_tracer;

import com.yukar1n.x_tracer.gui.XTracerGuiHandler;
import com.yukar1n.x_tracer.block.BlockXTracerContainer;
import com.yukar1n.x_tracer.item.ItemXTracerTerminal;
import com.yukar1n.x_tracer.network.FoundBlocksMessage;
import com.yukar1n.x_tracer.network.KeyPressMessage;
import com.yukar1n.x_tracer.network.TerminalRequestMessage;
import com.yukar1n.x_tracer.network.TerminalStateMessage;
import com.yukar1n.x_tracer.network.ContainerEditMessage;
import com.yukar1n.x_tracer.network.ContainerStateMessage;
import com.yukar1n.x_tracer.network.TerminalBindingMessage;
import com.yukar1n.x_tracer.network.TerminalContainersMessage;
import com.yukar1n.x_tracer.network.ContainerContentsMessage;
import com.yukar1n.x_tracer.network.ContainerContentsRequestMessage;
import com.yukar1n.x_tracer.network.ContainerExtractMessage;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import com.yukar1n.x_tracer.server.ServerTaskQueue;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)

    public static SimpleNetworkWrapper network;
    public static ServerTaskQueue serverTaskQueue;

    public static EntityPlayerMP findOnlinePlayer(UUID playerId) {
        if (MinecraftServer.getServer() == null || MinecraftServer.getServer().getConfigurationManager() == null) return null;
        for (Object value : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayerMP player = (EntityPlayerMP) value;
            if (playerId.equals(player.getUniqueID())) return player;
        }
        return null;
    }

    public void preInit(FMLPreInitializationEvent event) {
        // 创建网络通道
        serverTaskQueue = new ServerTaskQueue();
        FMLCommonHandler.instance().bus().register(serverTaskQueue);
        network = new SimpleNetworkWrapper("x_tracer");
        // 参数1: 处理类  参数2: 消息类  参数3: id(唯一)  参数4: 接收端
        network.registerMessage(
            KeyPressMessage.Handler.class,
            KeyPressMessage.class,
            0,
            Side.SERVER
        );
        network.registerMessage(
            FoundBlocksMessage.Handler.class,
            FoundBlocksMessage.class,
            1,
            Side.CLIENT);
        network.registerMessage(
            TerminalRequestMessage.Handler.class,
            TerminalRequestMessage.class,
            2,
            Side.SERVER);
        network.registerMessage(
            TerminalStateMessage.Handler.class,
            TerminalStateMessage.class,
            3,
            Side.CLIENT);
        network.registerMessage(
            ContainerEditMessage.Handler.class,
            ContainerEditMessage.class,
            4,
            Side.SERVER);
        network.registerMessage(
            ContainerStateMessage.Handler.class,
            ContainerStateMessage.class,
            5,
            Side.CLIENT);
        network.registerMessage(
            TerminalBindingMessage.Handler.class,
            TerminalBindingMessage.class,
            6,
            Side.SERVER);
        network.registerMessage(
            TerminalContainersMessage.Handler.class,
            TerminalContainersMessage.class,
            7,
            Side.CLIENT);
        network.registerMessage(ContainerContentsRequestMessage.Handler.class, ContainerContentsRequestMessage.class, 8, Side.SERVER);
        network.registerMessage(ContainerContentsMessage.Handler.class, ContainerContentsMessage.class, 9, Side.CLIENT);
        network.registerMessage(ContainerExtractMessage.Handler.class, ContainerExtractMessage.class, 10, Side.SERVER);

        ItemXTracerTerminal.register();
        BlockXTracerContainer.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(MyMod.INSTANCE, new XTracerGuiHandler());
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {}

    public void openTerminalGui() {}

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {}

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {}
}
