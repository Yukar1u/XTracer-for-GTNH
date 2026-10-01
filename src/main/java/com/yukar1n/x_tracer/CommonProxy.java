package com.yukar1n.x_tracer;

import com.yukar1n.x_tracer.gui.XTracerGuiHandler;
import com.yukar1n.x_tracer.item.ItemXTracerTerminal;
import com.yukar1n.x_tracer.network.FoundBlocksMessage;
import com.yukar1n.x_tracer.network.TerminalRequestMessage;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import com.yukar1n.x_tracer.network.KeyPressMessage;
import cpw.mods.fml.relauncher.Side;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)

    public static SimpleNetworkWrapper network;

    public void preInit(FMLPreInitializationEvent event) {
        // 创建网络通道
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

        ItemXTracerTerminal.register();
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
