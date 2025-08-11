package com.yukar1n.x_tracer;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import com.yukar1n.x_tracer.network.KeyPressMessage;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    private static KeyBinding keySearch;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event); // 调用父类的 preInit（保留公共逻辑）

        // 这里未来会加客户端资源（比如渲染器）

        // 注册按键（类别是 "key.categories.x_trace"，会在控制设置中分组）
        keySearch = new KeyBinding("key.x_tracer.search", Keyboard.KEY_X, "key.categories.x_tracer");
        ClientRegistry.registerKeyBinding(keySearch);

        // 注册事件监听器
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event){
        if (keySearch.isPressed()){
            //按键按下，发送请求到服务端
            network.sendToServer(new KeyPressMessage());
        }
    }
    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);

        // 这里未来会注册按键监听
    }
}
